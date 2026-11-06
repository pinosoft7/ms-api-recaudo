package co.datacenter.recaudo.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmacionRequest(
        @NotBlank String idPagoPasarela,
        @NotBlank String referenciaPasarela,
        @NotBlank String estado,
        String idMensaje,
        @NotBlank String firma
) {
}
