package co.datacenter.recaudo.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bandeja_notificacion")
public class BandejaEntity {

    @Id
    @Column(name = "id_bandeja", length = 36)
    private String idBandeja;

    @Column(name = "id_pago", length = 36)
    private String idPago;

    @Column(name = "referencia_pasarela", length = 64)
    private String referenciaPasarela;

    @Column(name = "id_pago_pasarela", length = 64)
    private String idPagoPasarela;

    @Column(name = "clave_dedup", nullable = false, unique = true, length = 128)
    private String claveDedup;

    @Column(name = "estado_notificado", length = 20)
    private String estadoNotificado;

    @Column(name = "firma_valida", nullable = false)
    private int firmaValida;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_proceso", nullable = false, length = 20)
    private EstadoBandeja estadoProceso;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_reintento")
    private Instant proximoReintento;

    @Column(name = "recibida_en", nullable = false)
    private Instant recibidaEn;

    @Column(name = "procesada_en")
    private Instant procesadaEn;
}
