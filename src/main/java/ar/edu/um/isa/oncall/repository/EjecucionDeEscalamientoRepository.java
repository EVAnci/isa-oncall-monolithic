package ar.edu.um.isa.oncall.repository;

import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EjecucionDeEscalamiento entity.
 */
@Repository
public interface EjecucionDeEscalamientoRepository extends JpaRepository<EjecucionDeEscalamiento, Long> {
    @Query(
        "select ejecucionDeEscalamiento from EjecucionDeEscalamiento ejecucionDeEscalamiento where ejecucionDeEscalamiento.destinatario.login = ?#{authentication.name}"
    )
    List<EjecucionDeEscalamiento> findByDestinatarioIsCurrentUser();

    default Optional<EjecucionDeEscalamiento> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<EjecucionDeEscalamiento> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<EjecucionDeEscalamiento> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select ejecucionDeEscalamiento from EjecucionDeEscalamiento ejecucionDeEscalamiento left join fetch ejecucionDeEscalamiento.incidente left join fetch ejecucionDeEscalamiento.paso left join fetch ejecucionDeEscalamiento.destinatario",
        countQuery = "select count(ejecucionDeEscalamiento) from EjecucionDeEscalamiento ejecucionDeEscalamiento"
    )
    Page<EjecucionDeEscalamiento> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select ejecucionDeEscalamiento from EjecucionDeEscalamiento ejecucionDeEscalamiento left join fetch ejecucionDeEscalamiento.incidente left join fetch ejecucionDeEscalamiento.paso left join fetch ejecucionDeEscalamiento.destinatario"
    )
    List<EjecucionDeEscalamiento> findAllWithToOneRelationships();

    @Query(
        "select ejecucionDeEscalamiento from EjecucionDeEscalamiento ejecucionDeEscalamiento left join fetch ejecucionDeEscalamiento.incidente left join fetch ejecucionDeEscalamiento.paso left join fetch ejecucionDeEscalamiento.destinatario where ejecucionDeEscalamiento.id =:id"
    )
    Optional<EjecucionDeEscalamiento> findOneWithToOneRelationships(@Param("id") Long id);
}
