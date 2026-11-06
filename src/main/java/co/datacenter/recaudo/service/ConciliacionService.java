package co.datacenter.recaudo.service;

import co.datacenter.recaudo.client.EstadoPasarela;
import co.datacenter.recaudo.client.PasarelaClient;
import co.datacenter.recaudo.client.PasarelaInciertoException;
import co.datacenter.recaudo.client.ValidacionClient;
import co.datacenter.recaudo.persistence.OrigenHistorial;
import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.persistence.TipoEvento;
import co.datacenter.recaudo.persistence.PagoRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConciliacionService {

    private final PagoRepository pagos;
    private final PasarelaClient pasarela;
    private final RegistroPagoService registro;
    private final ValidacionClient validacion;

    @Scheduled(fixedDelay = 120_000)
    public void conciliar() {
        for (PagoEntity pago : pagos.porConciliar(Instant.now())) {
            conciliarUno(pago);
        }
    }

    private void conciliarUno(PagoEntity pago) {
        if (Duration.between(pago.getCreadoEn(), Instant.now()).compareTo(Duration.ofHours(24)) > 0) {
            registro.reprogramar(pago.getIdPago(), null);
            validacion.marcarPorConciliar(pago.getReferencia());
            log.error("Conciliación agotada referencia={} idPago={}", pago.getReferencia(), pago.getIdPago());
            return;
        }
        try {
            EstadoPasarela estado = pasarela.consultar(pago.getReferenciaPasarela()).orElse(null);
            if (estado == null) {
                ausencia(pago);
                return;
            }
            if ("APROBADO".equalsIgnoreCase(estado.estado())) {
                registro.cerrar(pago.getIdPago(), pago.getVersion(), 1, pago.getIdPagoPasarela(),
                        OrigenHistorial.CONCILIACION, "APROBADO", TipoEvento.PAGO_APROBADO);
                return;
            }
            if ("RECHAZADO".equalsIgnoreCase(estado.estado())) {
                registro.cerrar(pago.getIdPago(), pago.getVersion(), 2, pago.getIdPagoPasarela(),
                        OrigenHistorial.CONCILIACION, "RECHAZADO", TipoEvento.PAGO_RECHAZADO);
                return;
            }
            registro.reprogramar(pago.getIdPago(), siguiente(pago));
        } catch (PasarelaInciertoException ex) {
            log.info("La reconsulta de {} falló por red. No cuenta como ausencia.", pago.getIdPago());
            registro.reprogramar(pago.getIdPago(), siguiente(pago));
        }
    }

    /**
     * Una sola ausencia no cierra: la pasarela puede persistir tarde.
     * Tres ausencias, con la última pasados 30 minutos, expiran el intento sin cobro.
     */
    private void ausencia(PagoEntity pago) {
        registro.sumarAusencia(pago.getIdPago());
        PagoEntity fresco = pagos.findById(pago.getIdPago()).orElseThrow();
        boolean tarde = Duration.between(fresco.getCreadoEn(), Instant.now()).compareTo(Duration.ofMinutes(30)) > 0;
        if (fresco.getAusenciasPasarela() >= 3 && tarde) {
            registro.cerrar(fresco.getIdPago(), fresco.getVersion(), 2, null,
                    OrigenHistorial.CONCILIACION, "Sin cobro", TipoEvento.PAGO_EXPIRADO);
            return;
        }
        registro.reprogramar(fresco.getIdPago(), siguiente(fresco));
    }

    private Instant siguiente(PagoEntity pago) {
        Duration edad = Duration.between(pago.getCreadoEn(), Instant.now());
        Duration espera = edad.compareTo(Duration.ofMinutes(15)) < 0 ? Duration.ofMinutes(2) : Duration.ofMinutes(15);
        return Instant.now().plus(espera);
    }
}
