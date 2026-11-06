package co.datacenter.recaudo.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pago", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pago_intento", columnNames = {"referencia", "numero_intento"}),
        @UniqueConstraint(name = "uq_pago_pasarela", columnNames = {"referencia_pasarela"})
})
public class PagoEntity {

    @Id
    @Column(name = "id_pago", length = 36)
    private String idPago;

    @Column(name = "id_orden", nullable = false, length = 36)
    private String idOrden;

    @Column(nullable = false, length = 40)
    private String referencia;

    @Column(name = "referencia_pasarela", nullable = false, length = 64)
    private String referenciaPasarela;

    @Column(name = "numero_intento", nullable = false)
    private int numeroIntento;

    @Column(name = "documento_cliente", length = 32)
    private String documentoCliente;

    @Column(name = "nombre_cliente", length = 120)
    private String nombreCliente;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private int estado;

    @Column(name = "id_pago_pasarela", length = 64)
    private String idPagoPasarela;

    @Column(name = "link_pasarela", length = 1000)
    private String linkPasarela;

    @Column(name = "link_redireccion", length = 1000)
    private String linkRedireccion;

    @Column(name = "proximo_conciliacion")
    private Instant proximoConciliacion;

    @Column(name = "ausencias_pasarela", nullable = false)
    private int ausenciasPasarela;

    @Column(name = "ultima_ausencia")
    private Instant ultimaAusencia;

    @Column(nullable = false)
    private long version;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
