package co.datacenter.recaudo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("ms-api-recaudo")
                .version("1.0.0")
                .description("Pago, pasarela, bandeja de confirmación y evento de salida."));
    }
}
