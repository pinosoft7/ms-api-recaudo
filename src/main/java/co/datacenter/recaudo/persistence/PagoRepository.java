package co.datacenter.recaudo.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<PagoEntity, String> {

    Optional<PagoEntity> findByReferenciaPasarela(String referenciaPasarela);

    Optional<PagoEntity> findByIdPagoPasarela(String idPagoPasarela);

    @Query("""
            select p from PagoEntity p
             where p.estado in (3, 4)
               and p.proximoConciliacion is not null
               and p.proximoConciliacion <= :ahora
            """)
    java.util.List<PagoEntity> porConciliar(@Param("ahora") Instant ahora);

    @Query("select max(p.numeroIntento) from PagoEntity p where p.referencia = :referencia")
    Optional<Integer> maximoIntento(@Param("referencia") String referencia);

    @Query("""
            select count(p) from PagoEntity p
             where p.referencia = :referencia and p.estado in (3, 4)
            """)
    long contarAbiertos(@Param("referencia") String referencia);

    @Query("""
            select count(p) from PagoEntity p
             where p.referencia = :referencia and p.estado = 1
            """)
    long contarAprobados(@Param("referencia") String referencia);

    /**
     * Solo cierra un intento que sigue en 3 o 4 y con la versión leída.
     * Cero filas significa que otro proceso ya lo cerró: no se publica otro evento.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update PagoEntity p
               set p.estado = :estado,
                   p.version = p.version + 1,
                   p.actualizadoEn = :ahora
             where p.idPago = :id
               and p.version = :version
               and p.estado in (3, 4)
            """)
    int cerrarSiAbierto(@Param("id") String id,
                         @Param("version") long version,
                         @Param("estado") int estado,
                         @Param("ahora") Instant ahora);
}
