package co.datacenter.recaudo.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PasarelaClient {

    private final RestClient alta;
    private final RestClient consulta;

    public PasarelaClient(@Qualifier("pasarelaAlta") RestClient alta,
                          @Qualifier("pasarelaConsulta") RestClient consulta) {
        this.alta = alta;
        this.consulta = consulta;
    }

    @CircuitBreaker(name = "pasarela", fallbackMethod = "altaAbierta")
    public AltaPasarela crear(String referenciaPasarela, BigDecimal valor, String moneda, String linkRedireccion) {
        try {
            AltaPasarela respuesta = alta.post()
                    .uri("/pagos")
                    .body(new AltaSolicitud(referenciaPasarela, valor, moneda, linkRedireccion))
                    .retrieve()
                    .body(AltaPasarela.class);
            if (respuesta == null || respuesta.idPagoPasarela() == null) {
                throw new PasarelaInciertoException("La pasarela no acusó el link");
            }
            return respuesta;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new PasarelaRechazoException("Alta rechazada por la pasarela");
            }
            throw new PasarelaInciertoException("La pasarela respondió " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new PasarelaInciertoException("Timeout al crear el link", ex);
        }
    }

    @SuppressWarnings("unused")
    private AltaPasarela altaAbierta(String referenciaPasarela, BigDecimal valor, String moneda,
                                     String linkRedireccion, CallNotPermittedException ex) {
        throw new PasarelaInciertoException("Circuito de la pasarela abierto");
    }

    @Retry(name = "pasarelaConsulta")
    @CircuitBreaker(name = "pasarela", fallbackMethod = "consultaAbierta")
    public Optional<EstadoPasarela> consultar(String referenciaPasarela) {
        try {
            EstadoPasarela estado = consulta.get()
                    .uri("/pagos/{referencia}", referenciaPasarela)
                    .retrieve()
                    .body(EstadoPasarela.class);
            return Optional.ofNullable(estado);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                return Optional.empty();
            }
            if (ex.getStatusCode().is4xxClientError()) {
                throw new PasarelaRechazoException("Consulta rechazada por la pasarela");
            }
            throw new PasarelaInciertoException("La consulta respondió " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new PasarelaInciertoException("Timeout al consultar el pago", ex);
        }
    }

    @SuppressWarnings("unused")
    private Optional<EstadoPasarela> consultaAbierta(String referenciaPasarela, CallNotPermittedException ex) {
        throw new PasarelaInciertoException("Circuito de la pasarela abierto");
    }

    private record AltaSolicitud(String referenciaPasarela, BigDecimal valor, String moneda, String linkRedireccion) {
    }
}
