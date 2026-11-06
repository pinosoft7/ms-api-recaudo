package co.datacenter.recaudo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class NotificacionDedup {

    static final String EN_PROCESO = "EN_PROCESO";
    static final String PROCESADA = "PROCESADA";

    private final StringRedisTemplate redis;

    public Toma tomar(String clave) {
        try {
            Boolean creada = redis.opsForValue().setIfAbsent("notif:" + clave, EN_PROCESO, Duration.ofSeconds(30));
            if (Boolean.TRUE.equals(creada)) {
                return Toma.NUEVA;
            }
            String valor = redis.opsForValue().get("notif:" + clave);
            return EN_PROCESO.equals(valor) ? Toma.EN_PROCESO : Toma.PROCESADA;
        } catch (RuntimeException ex) {
            log.warn("Redis no respondió para la confirmación. El UNIQUE de la bandeja hace el corte.");
            return Toma.REDIS_CAIDO;
        }
    }

    public void procesada(String clave) {
        try {
            redis.opsForValue().set("notif:" + clave, PROCESADA, Duration.ofDays(7));
        } catch (RuntimeException ex) {
            log.warn("No se pudo marcar la confirmación {} como procesada.", clave);
        }
    }

    public String clave(String idPagoPasarela, String estado, String idMensaje) {
        String base = idPagoPasarela + "|" + estado + "|" + (idMensaje == null ? "" : idMensaje);
        return sha256(base);
    }

    private String sha256(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }

    public enum Toma {
        NUEVA,
        EN_PROCESO,
        PROCESADA,
        REDIS_CAIDO
    }

    public Optional<String> valor(String clave) {
        try {
            return Optional.ofNullable(redis.opsForValue().get("notif:" + clave));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
