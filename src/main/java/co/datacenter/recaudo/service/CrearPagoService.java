package co.datacenter.recaudo.service;

import co.datacenter.recaudo.client.AltaPasarela;
import co.datacenter.recaudo.client.PasarelaClient;
import co.datacenter.recaudo.client.PasarelaInciertoException;
import co.datacenter.recaudo.client.PasarelaRechazoException;
import co.datacenter.recaudo.exception.NegocioException;
import co.datacenter.recaudo.mapper.PagoMapper;
import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.persistence.PagoRepository;
import co.datacenter.recaudo.request.CrearPagoRequest;
import co.datacenter.recaudo.response.PagoResponse;
import co.datacenter.recaudo.util.Mascara;
import co.datacenter.recaudo.util.ParamEncrip;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class CrearPagoService {

    private final PagoRepository pagos;
    private final RegistroPagoService registro;
    private final PasarelaClient pasarela;
    private final PagoMapper mapper;
    private final ParamEncrip cripto;
    private final CircuitBreaker circuito;
    private final String retornoBase;

    public CrearPagoService(PagoRepository pagos, RegistroPagoService registro, PasarelaClient pasarela,
                            PagoMapper mapper, ParamEncrip cripto, CircuitBreakerRegistry breakers,
                            @Value("${comercio.link-redireccion:http://localhost:8080/retorno}") String retornoBase) {
        this.pagos = pagos;
        this.registro = registro;
        this.pasarela = pasarela;
        this.mapper = mapper;
        this.cripto = cripto;
        this.circuito = breakers.circuitBreaker("pasarela");
        this.retornoBase = retornoBase;
    }

    public PagoResponse crear(CrearPagoRequest request) {
        if (pagos.contarAprobados(request.referencia()) > 0) {
            throw new NegocioException(HttpStatus.CONFLICT, "La orden ya tiene un pago aprobado");
        }
        if (pagos.contarAbiertos(request.referencia()) > 0) {
            throw new NegocioException(HttpStatus.CONFLICT, "Hay un intento abierto");
        }
        if (circuito.getState() == CircuitBreaker.State.OPEN) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "Pasarela no disponible");
        }
        String referenciaPasarela = UUID.randomUUID().toString().replace("-", "");
        String idPago = UUID.randomUUID().toString();
        String linkReal = retornoBase + "?paramEncrip=" + cripto.cifrar(idPago, request.referencia());
        PagoEntity pago = registro.crearPendiente(request, idPago, referenciaPasarela, linkReal);
        try {
            AltaPasarela alta = pasarela.crear(referenciaPasarela, request.valorOrden(), request.moneda(), linkReal);
            registro.marcarLink(pago.getIdPago(), alta.linkPasarela(), alta.idPagoPasarela());
            log.info("Link creado referencia={} documento={} idPago={} referenciaPasarela={}",
                    pago.getReferencia(), Mascara.documento(pago.getDocumentoCliente()),
                    pago.getIdPago(), referenciaPasarela);
            return mapper.de(pagos.findById(pago.getIdPago()).orElseThrow(), 200, null);
        } catch (PasarelaRechazoException ex) {
            registro.marcarRechazoAlta(pago.getIdPago(), ex.getMessage());
            return mapper.de(pagos.findById(pago.getIdPago()).orElseThrow(), 409, ex.getMessage());
        } catch (PasarelaInciertoException ex) {
            registro.marcarIncierto(pago.getIdPago(), ex.getMessage());
            return mapper.de(pagos.findById(pago.getIdPago()).orElseThrow(), 202, ex.getMessage());
        }
    }
}
