package ar.edu.um.isa.oncall.repository;

import ar.edu.um.isa.oncall.domain.TurnoDeGuardia;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas que necesita la regla de guardia vigente.
 *
 * Igual que {@link IncidenteDeduplicacionRepository}, vive aparte de
 * {@link TurnoDeGuardiaRepository} porque ese archivo lo pisa el generador.
 */
@Repository
public interface GuardiaVigenteRepository extends org.springframework.data.repository.Repository<TurnoDeGuardia, Long> {
    /**
     * Turnos de la rotacion que cubren el instante dado.
     *
     * El intervalo es semiabierto, {@code [desde, hasta)}: un turno que termina a las 09:00 ya
     * no cubre las 09:00. Asi dos turnos consecutivos no se pisan en el instante del relevo.
     *
     * @param rotacionId la rotacion consultada.
     * @param instante el instante a resolver.
     * @return los turnos vigentes, sin orden particular: el criterio de desempate es de la regla.
     */
    @Query(
        """
        select turno from TurnoDeGuardia turno
            left join fetch turno.responsable
        where turno.rotacion.id = :rotacionId
            and turno.desde <= :instante
            and turno.hasta > :instante
        """
    )
    List<TurnoDeGuardia> buscarTurnosQueCubren(@Param("rotacionId") Long rotacionId, @Param("instante") Instant instante);
}
