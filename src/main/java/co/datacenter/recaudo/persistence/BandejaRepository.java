package co.datacenter.recaudo.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BandejaRepository extends JpaRepository<BandejaEntity, String> {

    Optional<BandejaEntity> findByClaveDedup(String claveDedup);
}
