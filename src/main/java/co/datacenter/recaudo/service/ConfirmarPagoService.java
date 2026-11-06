package co.datacenter.recaudo.service;

import co.datacenter.recaudo.exception.NegocioException;
import co.datacenter.recaudo.persistence.BandejaEntity;
import co.datacenter.recaudo.persistence.BandejaRepository;
import co.datacenter.recaudo.persistence.EstadoBandeja;
import co.datacenter.recaudo.persistence.OrigenHistorial;
import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.persistence.TipoEvento;
import co.datacenter.recaudo.persistence.PagoRepository;
import co.datacenter.recaudo.request.ConfirmacionRequest;
import co.datacenter.recaudo.service.NotificacionDedup.Toma;
import co.datacenter.recaudo.util.FirmaConfirmacion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConfirmarPagoService {

    private final FirmaConfirmacion firma;
    private final NotificacionDedup dedup;
    private final BandejaRepository bandeja;
    private final PagoRepository pagos;
    private final RegistroPagoService registro;
    private final ObjectMapper json;

    public void confirmar(ConfirmacionRequest request) {
        String clave = dedup.clave(request.idPagoPasarela(), request.estado(), request.idMensaje());
        boolean firmaOk = firma.valida(request.idPagoPasarela(), request.estado(), request.idMensaje(), request.firma());
        if (!firmaOk) {
            guardar(request, clave, null, EstadoBandeja.DESCARTADA, 0);
            throw new NegocioException(HttpStatus.UNAUTHORIZED, "Firma inválida");
        }
        Toma toma = dedup.tomar(clave);
        if (toma == Toma.PROCESADA) {
            log.info("Confirmación duplicada idPagoPasarela={} estado={}", request.idPagoPasarela(), request.estado());
            return;
        }
        if (toma == Toma.EN_PROCESO) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "La confirmación sigue en proceso");
        }
        Optional<PagoEntity> pago = pagos.findByIdPagoPasarela(request.idPagoPasarela())
                .or(() -> pagos.findByReferenciaPasarela(request.referenciaPasarela()));
        if (pago.isEmpty()) {
            guardar(request, clave, null, EstadoBandeja.RECIBIDA, 1);
            dedup.procesada(clave);
            log.warn("Confirmación sin pago idPagoPasarela={}", request.idPagoPasarela());
            return;
        }
        PagoEntity intento = pago.get();
        if (intento.getEstado() == 1) {
            guardar(request, clave, intento.getIdPago(), EstadoBandeja.PROCESADA, 1);
            dedup.procesada(clave);
            log.warn("Rechazo tardío ignorado. El pago {} ya está aprobado.", intento.getIdPago());
            return;
        }
        int destino = "APROBADO".equalsIgnoreCase(request.estado()) ? 1 : 2;
        TipoEvento tipo = destino == 1 ? TipoEvento.PAGO_APROBADO : TipoEvento.PAGO_RECHAZADO;
        boolean cerro = registro.cerrar(intento.getIdPago(), intento.getVersion(), destino,
                request.idPagoPasarela(), OrigenHistorial.CONFIRMACION, request.estado(), tipo);
        guardar(request, clave, intento.getIdPago(), cerro ? EstadoBandeja.PROCESADA : EstadoBandeja.DUPLICADA, 1);
        dedup.procesada(clave);
        log.info("Confirmación idPago={} estadoInicial={} estadoFinal={} efecto={}",
                intento.getIdPago(), intento.getEstado(), destino, cerro);
    }

    private void guardar(ConfirmacionRequest request, String clave, String idPago, EstadoBandeja proceso, int firmaValida) {
        if (bandeja.findByClaveDedup(clave).isPresent() && proceso != EstadoBandeja.DESCARTADA) {
            return;
        }
        BandejaEntity fila = new BandejaEntity();
        fila.setIdBandeja(UUID.randomUUID().toString());
        fila.setIdPago(idPago);
        fila.setReferenciaPasarela(request.referenciaPasarela());
        fila.setIdPagoPasarela(request.idPagoPasarela());
        fila.setClaveDedup(proceso == EstadoBandeja.DESCARTADA ? clave + ":descartada:" + UUID.randomUUID() : clave);
        fila.setEstadoNotificado(request.estado());
        fila.setFirmaValida(firmaValida);
        fila.setEstadoProceso(proceso);
        fila.setPayload(payload(request));
        fila.setIntentos(1);
        fila.setRecibidaEn(Instant.now());
        fila.setProcesadaEn(proceso == EstadoBandeja.RECIBIDA ? null : Instant.now());
        try {
            bandeja.saveAndFlush(fila);
        } catch (DataIntegrityViolationException ex) {
            log.info("La bandeja ya tenía la clave {}. No hay segundo efecto.", clave);
        }
    }

    private String payload(ConfirmacionRequest request) {
        try {
            return json.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
