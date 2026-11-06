package co.datacenter.recaudo.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialRepository extends JpaRepository<HistorialPagoEntity, String> {
}
