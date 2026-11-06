package co.datacenter.recaudo.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient pasarelaAlta(@Value("${pasarela.base-url}") String baseUrl) {
        return cliente(baseUrl, Duration.ofSeconds(8));
    }

    @Bean
    public RestClient pasarelaConsulta(@Value("${pasarela.base-url}") String baseUrl) {
        return cliente(baseUrl, Duration.ofSeconds(5));
    }

    @Bean
    public RestClient validacionRestClient(@Value("${validacion.base-url}") String baseUrl) {
        return cliente(baseUrl, Duration.ofSeconds(3));
    }

    private RestClient cliente(String baseUrl, Duration lectura) {
        ClientHttpRequestFactorySettings ajustes = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(lectura);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(ajustes))
                .build();
    }
}
