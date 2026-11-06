package co.datacenter.recaudo.service;

import co.datacenter.recaudo.client.EstadoPasarela;
import co.datacenter.recaudo.client.PasarelaClient;
import co.datacenter.recaudo.client.PasarelaInciertoException;
import co.datacenter.recaudo.exception.NegocioException;
import co.datacenter.recaudo.mapper.PagoMapper;
import co.datacenter.recaudo.persistence.OrigenHistorial;
import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.persistence.TipoEvento;
import co.datacenter.recaudo.persistence.PagoRepository;
import co.datacenter.recaudo.response.PagoResponse;
import co.datacenter.recaudo.util.ParamEncrip;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConsultarPagoService {

    private final ParamEncrip cripto;
    private final PagoRepository pagos;
    private final PasarelaClient pasarela;
    private final RegistroPagoService registro;
    private final PagoMapper mapper;

    public PagoResponse porRedirect(String paramEncrip) {
        ParamEncrip.Contenido contenido;
        try {
            contenido = cripto.descifrar(paramEncrip);
        } catch (IllegalArgumentException ex) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return consultar(contenido.idPago(), OrigenHistorial.REDIRECT);
    }

    public PagoResponse porId(String idPago) {
        PagoEntity pago = pagos.findById(idPago)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "El pago no existe"));
        return mapper.de(pago, 200, null);
    }

    public PagoResponse consultar(String idPago, OrigenHistorial origen) {
        PagoEntity pago = pagos.findById(idPago)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "El pago no existe"));
        if (pago.getEstado() == 1 || pago.getEstado() == 2) {
            return mapper.de(pago, 200, null);
        }
        try {
            EstadoPasarela estado = pasarela.consultar(pago.getReferenciaPasarela()).orElse(null);
            if (estado == null) {
                return mapper.de(pago, 202, "La referencia todavía no existe en la pasarela");
            }
            Integer codigo = codigo(estado.estado());
            if (codigo == null) {
                return mapper.de(pago, 202, "El pago sigue pendiente");
            }
            TipoEvento tipo = codigo == 1 ? TipoEvento.PAGO_APROBADO : TipoEvento.PAGO_RECHAZADO;
            registro.cerrar(pago.getIdPago(), pago.getVersion(), codigo, pago.getIdPagoPasarela(), origen, estado.estado(), tipo);
            return mapper.de(pagos.findById(idPago).orElseThrow(), 200, null);
        } catch (PasarelaInciertoException ex) {
            log.info("Consulta incierta idPago={}. El intento sigue en confirmación.", idPago);
            return mapper.de(pago, 202, ex.getMessage());
        }
    }

    private Integer codigo(String estado) {
        if ("APROBADO".equalsIgnoreCase(estado)) {
            return 1;
        }
        if ("RECHAZADO".equalsIgnoreCase(estado)) {
            return 2;
        }
        return null;
    }
}
