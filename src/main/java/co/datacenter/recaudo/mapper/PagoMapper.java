package co.datacenter.recaudo.mapper;

import co.datacenter.recaudo.persistence.PagoEntity;
import co.datacenter.recaudo.response.PagoResponse;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoResponse de(PagoEntity pago, int code, String error) {
        return new PagoResponse(
                code,
                error,
                pago.getLinkPasarela(),
                pago.getEstado(),
                pago.getReferencia(),
                pago.getIdPago(),
                pago.getNumeroIntento());
    }
}
