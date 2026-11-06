package co.datacenter.recaudo.client;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ValidacionClient {

    private final RestClient validacionRestClient;

    public void entregarEvento(String referencia, String tipo, BigDecimal valor, String idPago) {
        validacionRestClient.post()
                .uri("/interno/eventos")
                .body(new EventoRemoto(referencia, tipo, valor, idPago))
                .retrieve()
                .toBodilessEntity();
    }

    public void marcarPorConciliar(String referencia) {
        validacionRestClient.post()
                .uri("/interno/ordenes/{referencia}/por-conciliar", referencia)
                .retrieve()
                .toBodilessEntity();
    }

    private record EventoRemoto(String referencia, String tipo, BigDecimal valor, String idPago) {
    }
}
