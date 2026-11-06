package co.datacenter.recaudo.service;

import co.datacenter.recaudo.persistence.EventoSalidaEntity;
import co.datacenter.recaudo.persistence.EventoSalidaRepository;
import co.datacenter.recaudo.persistence.HistorialPagoEntity;
import co.datacenter.recaudo.persistence.HistorialRepository;
import co.datacenter.recaudo.persistence.OrigenHistorial;
import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.persistence.PagoRepository;
import co.datacenter.recaudo.persistence.TipoEvento;
import co.datacenter.recaudo.request.CrearPagoRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistroPagoService {

    private final PagoRepository pagos;
    private final HistorialRepository historial;
    private final EventoSalidaRepository eventos;
    private final ObjectMapper json;

    @Transactional
    public PagoEntity crearPendiente(CrearPagoRequest request, String idPago, String referenciaPasarela,
                                     String linkRedireccion) {
        Instant ahora = Instant.now();
        int numero = pagos.maximoIntento(request.referencia()).orElse(0) + 1;
        PagoEntity pago = new PagoEntity();
        pago.setIdPago(idPago);
        pago.setIdOrden(request.idOrden());
        pago.setReferencia(request.referencia());
        pago.setReferenciaPasarela(referenciaPasarela);
        pago.setNumeroIntento(numero);
        pago.setDocumentoCliente(request.documentoCliente());
        pago.setNombreCliente(request.nombreCliente());
        pago.setDescripcion(request.descripcionOrden());
        pago.setValor(request.valorOrden());
        pago.setMoneda(request.moneda());
        pago.setEstado(3);
        pago.setLinkRedireccion(linkRedireccion);
        pago.setAusenciasPasarela(0);
        pago.setVersion(0);
        pago.setCreadoEn(ahora);
        pago.setActualizadoEn(ahora);
        pago.setProximoConciliacion(ahora.plus(Duration.ofMinutes(2)));
        PagoEntity guardado = pagos.save(pago);
        anotar(guardado, "Inicio", "Creando link", OrigenHistorial.ALTA, "Fila creada antes de llamar a la pasarela", null, null);
        return guardado;
    }

    @Transactional
    public void marcarLink(String idPago, String linkPasarela, String idPagoPasarela) {
        PagoEntity pago = pagos.findById(idPago).orElseThrow();
        pago.setEstado(4);
        pago.setLinkPasarela(linkPasarela);
        pago.setIdPagoPasarela(idPagoPasarela);
        pago.setVersion(pago.getVersion() + 1);
        pago.setActualizadoEn(Instant.now());
        anotar(pago, "Creando link", "Link creado", OrigenHistorial.ALTA, null, null, idPagoPasarela);
    }

    @Transactional
    public void marcarRechazoAlta(String idPago, String detalle) {
        PagoEntity pago = pagos.findById(idPago).orElseThrow();
        pago.setEstado(2);
        pago.setProximoConciliacion(null);
        pago.setVersion(pago.getVersion() + 1);
        pago.setActualizadoEn(Instant.now());
        anotar(pago, "Creando link", "RECHAZADO", OrigenHistorial.ALTA, detalle, null, null);
        publicar(pago, TipoEvento.PAGO_RECHAZADO);
    }

    @Transactional
    public void marcarIncierto(String idPago, String detalle) {
        PagoEntity pago = pagos.findById(idPago).orElseThrow();
        pago.setEstado(4);
        pago.setVersion(pago.getVersion() + 1);
        pago.setActualizadoEn(Instant.now());
        anotar(pago, "Creando link", "POR_CONFIRMAR", OrigenHistorial.ALTA, detalle, null, null);
    }

    /**
     * El UPDATE condicionado y el evento salen en la misma transacción.
     * Si otro proceso ya cerró el intento, no se inserta un segundo evento.
     */
    @Transactional
    public boolean cerrar(String idPago, long version, int estadoNuevo, String idPagoPasarela,
                          OrigenHistorial origen, String observacion, TipoEvento tipo) {
        int filas = pagos.cerrarSiAbierto(idPago, version, estadoNuevo, Instant.now());
        if (filas == 0) {
            return false;
        }
        PagoEntity pago = pagos.findById(idPago).orElseThrow();
        if (idPagoPasarela != null && pago.getIdPagoPasarela() == null) {
            pago.setIdPagoPasarela(idPagoPasarela);
        }
        anotar(pago, "POR_CONFIRMAR", tipo.name(), origen, observacion, null, null);
        publicar(pago, tipo);
        return true;
    }

    @Transactional
    public void sumarAusencia(String idPago) {
        PagoEntity pago = pagos.findById(idPago).orElseThrow();
        pago.setAusenciasPasarela(pago.getAusenciasPasarela() + 1);
        pago.setUltimaAusencia(Instant.now());
        pago.setActualizadoEn(Instant.now());
    }

    @Transactional
    public void reprogramar(String idPago, Instant proximo) {
        PagoEntity pago = pagos.findById(idPago).orElseThrow();
        pago.setProximoConciliacion(proximo);
        pago.setActualizadoEn(Instant.now());
    }

    private void publicar(PagoEntity pago, TipoEvento tipo) {
        EventoSalidaEntity evento = new EventoSalidaEntity();
        evento.setIdEvento(UUID.randomUUID().toString());
        evento.setIdPago(pago.getIdPago());
        evento.setTipo(tipo);
        evento.setPayload(payload(pago, tipo));
        evento.setPublicado(0);
        evento.setIntentos(0);
        evento.setProximoReintento(Instant.now());
        evento.setCreadoEn(Instant.now());
        eventos.save(evento);
    }

    private String payload(PagoEntity pago, TipoEvento tipo) {
        try {
            return json.writeValueAsString(new EventoPayload(
                    pago.getReferencia(), tipo.name(), pago.getValor(), pago.getIdPago()));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo armar el evento de salida", ex);
        }
    }

    private void anotar(PagoEntity pago, String inicial, String fin, OrigenHistorial origen,
                        String observacion, String request, String response) {
        HistorialPagoEntity fila = new HistorialPagoEntity();
        fila.setIdHistorial(UUID.randomUUID().toString());
        fila.setIdPago(pago.getIdPago());
        fila.setEstadoPago(pago.getEstado());
        fila.setEstadoInicial(inicial);
        fila.setEstadoFinal(fin);
        fila.setOrigen(origen);
        fila.setObservacion(observacion);
        fila.setRequestJson(request);
        fila.setResponseJson(response);
        fila.setFechaRegistro(Instant.now());
        historial.save(fila);
    }

    private record EventoPayload(String referencia, String tipo, java.math.BigDecimal valor, String idPago) {
    }
}
