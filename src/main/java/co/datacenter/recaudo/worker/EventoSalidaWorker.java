package co.datacenter.recaudo.worker;

import co.datacenter.recaudo.client.ValidacionClient;
import co.datacenter.recaudo.persistence.EventoSalidaEntity;
import co.datacenter.recaudo.persistence.EventoSalidaRepository;
import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.persistence.PagoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
@Log4j2
@RequiredArgsConstructor
public class EventoSalidaWorker {

    private static final List<Duration> ESPERAS = List.of(
            Duration.ofSeconds(30),
            Duration.ofMinutes(2),
            Duration.ofMinutes(10),
            Duration.ofMinutes(30),
            Duration.ofHours(2));

    private final EventoSalidaRepository eventos;
    private final PagoRepository pagos;
    private final ValidacionClient validacion;
    private final ObjectMapper json;

    @Scheduled(fixedDelay = 15_000)
    public void entregar() {
        for (EventoSalidaEntity evento : pendientes()) {
            entregarUno(evento);
        }
    }

    private List<EventoSalidaEntity> pendientes() {
        try {
            return eventos.tomarPendientes().stream()
                    .map(id -> eventos.findById(id).orElseThrow())
                    .toList();
        } catch (DataAccessException ex) {
            return eventos.pendientesSinBloqueo(Instant.now());
        }
    }

    private void entregarUno(EventoSalidaEntity evento) {
        try {
            JsonNode nodo = json.readTree(evento.getPayload());
            PagoEntity pago = pagos.findById(evento.getIdPago()).orElseThrow();
            validacion.entregarEvento(
                    nodo.path("referencia").asText(pago.getReferencia()),
                    evento.getTipo().name(),
                    pago.getValor(),
                    pago.getIdPago());
            evento.setPublicado(1);
            evento.setUltimoError(null);
            log.info("Evento entregado idPago={} tipo={}", evento.getIdPago(), evento.getTipo());
        } catch (RestClientException | java.io.IOException ex) {
            int intento = evento.getIntentos() + 1;
            evento.setIntentos(intento);
            evento.setUltimoError(recortar(ex.getMessage()));
            if (intento >= ESPERAS.size()) {
                eventos.save(evento);
                log.error("Evento agotado idPago={} tipo={}", evento.getIdPago(), evento.getTipo());
                return;
            }
            evento.setProximoReintento(Instant.now().plus(ESPERAS.get(intento - 1)));
        }
        eventos.save(evento);
    }

    private String recortar(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        return mensaje.length() <= 1000 ? mensaje : mensaje.substring(0, 1000);
    }
}
