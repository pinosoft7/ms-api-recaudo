package co.datacenter.recaudo.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventoSalidaRepository extends JpaRepository<EventoSalidaEntity, String> {

    /**
     * Oracle toma la fila y la salta si otro worker ya la bloqueó.
     * publicado = 0 e intentos &lt; 5 dejan fuera los avisos agotados.
     */
    @Query(value = """
            SELECT id_evento FROM evento_salida
             WHERE publicado = 0
               AND intentos < 5
               AND proximo_reintento <= SYSTIMESTAMP
             ORDER BY proximo_reintento
             FETCH FIRST 20 ROWS ONLY
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<String> tomarPendientes();

    @Query("""
            select e from EventoSalidaEntity e
             where e.publicado = 0 and e.intentos < 5 and e.proximoReintento <= :ahora
             order by e.proximoReintento
            """)
    List<EventoSalidaEntity> pendientesSinBloqueo(Instant ahora);
}
