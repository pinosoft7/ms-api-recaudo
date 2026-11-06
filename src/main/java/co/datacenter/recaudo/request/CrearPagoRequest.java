package co.datacenter.recaudo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CrearPagoRequest(
        @NotBlank String idOrden,
        @NotBlank String referencia,
        int numeroIntento,
        @NotBlank String documentoCliente,
        @NotBlank String nombreCliente,
        @NotBlank String descripcionOrden,
        @NotNull BigDecimal valorOrden,
        @NotBlank String moneda
) {
}
