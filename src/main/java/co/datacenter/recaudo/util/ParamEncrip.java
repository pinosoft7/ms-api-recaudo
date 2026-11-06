package co.datacenter.recaudo.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El redirect solo trae este token. Vence a las dos horas y no es el estado del pago.
 */
@Component
public class ParamEncrip {

    private static final int IV = 12;
    private static final int TAG = 128;
    private static final Duration VIGENCIA = Duration.ofHours(2);

    private final byte[] clave;
    private final SecureRandom random = new SecureRandom();

    public ParamEncrip(@Value("${pasarela.cripto-clave}") String clave) {
        try {
            this.clave = MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo preparar la clave del redirect", ex);
        }
    }

    public String cifrar(String idPago, String referencia) {
        long expira = Instant.now().plus(VIGENCIA).getEpochSecond();
        String plano = idPago + "|" + referencia + "|" + expira;
        byte[] iv = new byte[IV];
        random.nextBytes(iv);
        byte[] cifrado = cifrar(plano.getBytes(StandardCharsets.UTF_8), iv, Cipher.ENCRYPT_MODE);
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + cifrado.length);
        buffer.put(iv);
        buffer.put(cifrado);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    public Contenido descifrar(String token) {
        byte[] todo = Base64.getUrlDecoder().decode(token);
        ByteBuffer buffer = ByteBuffer.wrap(todo);
        byte[] iv = new byte[IV];
        buffer.get(iv);
        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);
        String plano = new String(cifrar(cifrado, iv, Cipher.DECRYPT_MODE), StandardCharsets.UTF_8);
        String[] partes = plano.split("\\|", 3);
        if (partes.length != 3) {
            throw new IllegalArgumentException("Token de redirect inválido");
        }
        long expira = Long.parseLong(partes[2]);
        if (Instant.now().getEpochSecond() > expira) {
            throw new IllegalArgumentException("El redirect venció");
        }
        return new Contenido(partes[0], partes[1]);
    }

    private byte[] cifrar(byte[] datos, byte[] iv, int modo) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(modo, new SecretKeySpec(clave, 0, 16, "AES"), new GCMParameterSpec(TAG, iv));
            return cipher.doFinal(datos);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("No se pudo leer el redirect", ex);
        }
    }

    public record Contenido(String idPago, String referencia) {
    }
}
