package co.datacenter.recaudo.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "evento_salida", uniqueConstraints = {
        @UniqueConstraint(name = "uq_evento_tipo", columnNames = {"id_pago", "tipo"})
})
public class EventoSalidaEntity {

    @Id
    @Column(name = "id_evento", length = 36)
    private String idEvento;

    @Column(name = "id_pago", nullable = false, length = 36)
    private String idPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEvento tipo;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(nullable = false)
    private int publicado;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_reintento")
    private Instant proximoReintento;

    @Column(name = "ultimo_error", length = 1000)
    private String ultimoError;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
}
