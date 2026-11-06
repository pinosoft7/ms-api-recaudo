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
@Table(name = "historial_pago")
public class HistorialPagoEntity {

    @Id
    @Column(name = "id_historial", length = 36)
    private String idHistorial;

    @Column(name = "id_pago", nullable = false, length = 36)
    private String idPago;

    @Column(name = "estado_pago", nullable = false)
    private int estadoPago;

    @Column(name = "estado_inicial", length = 40)
    private String estadoInicial;

    @Column(name = "estado_final", length = 40)
    private String estadoFinal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenHistorial origen;

    @Column(length = 500)
    private String observacion;

    @Lob
    @Column(name = "request_json")
    private String requestJson;

    @Lob
    @Column(name = "response_json")
    private String responseJson;

    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
}
