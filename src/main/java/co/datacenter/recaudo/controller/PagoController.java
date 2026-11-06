package co.datacenter.recaudo.controller;

import co.datacenter.recaudo.request.ConfirmacionRequest;
import co.datacenter.recaudo.request.CrearPagoRequest;
import co.datacenter.recaudo.response.PagoResponse;
import co.datacenter.recaudo.service.ConfirmarPagoService;
import co.datacenter.recaudo.service.ConsultarPagoService;
import co.datacenter.recaudo.service.CrearPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pago")
public class PagoController {

    private final CrearPagoService crearPago;
    private final ConsultarPagoService consultarPago;
    private final ConfirmarPagoService confirmarPago;

    @PostMapping("/pagos")
    @Operation(summary = "Crea el intento, lo persiste en PENDIENTE y pide el link a la pasarela")
    public PagoResponse crear(@Valid @RequestBody CrearPagoRequest request) {
        return crearPago.crear(request);
    }

    @GetMapping("/pagos/{idPago}")
    @Operation(summary = "Devuelve el intento guardado, sin llamar a la pasarela")
    public PagoResponse porId(@PathVariable String idPago) {
        return consultarPago.porId(idPago);
    }

    @GetMapping("/pagos/retorno")
    @Operation(summary = "El front vuelve del banco. El estado lo informa la pasarela, no el link")
    public PagoResponse retorno(@RequestParam String paramEncrip) {
        return consultarPago.porRedirect(paramEncrip);
    }

    @PostMapping("/pagos/confirmaciones")
    @Operation(summary = "URL de confirmación de la pasarela. Guarda la bandeja y aplica un solo efecto")
    public void confirmar(@Valid @RequestBody ConfirmacionRequest request) {
        confirmarPago.confirmar(request);
    }
}
