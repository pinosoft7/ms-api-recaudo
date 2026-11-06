package co.datacenter.recaudo.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FirmaConfirmacion {

    private final byte[] secreto;

    public FirmaConfirmacion(@Value("${pasarela.firma-secreto}") String secreto) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
    }

    public boolean valida(String idPagoPasarela, String estado, String idMensaje, String firma) {
        String mensaje = idMensaje == null ? "" : idMensaje;
        String esperado = firmar(idPagoPasarela + "|" + estado + "|" + mensaje);
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                firma.getBytes(StandardCharsets.UTF_8));
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("No se pudo verificar la firma", ex);
        }
    }
}
