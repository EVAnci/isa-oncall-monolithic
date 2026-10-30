package ar.edu.um.isa.oncall.repository;

import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.PoliticaEscalamiento;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas que necesita la cadena de escalamiento. Aparte de los repositorios generados, por
 * la misma razon que {@link IncidenteDeduplicacionRepository}.
 */
@Repository
public interface EscalamientoRepository extends org.springframework.data.repository.Repository<Incidente, Long> {
    /**
     * El incidente, con la fila bloqueada hasta el fin de la transaccion
     * ({@code select ... for update}).
     *
     * Es lo que hace idempotente al escalamiento: si el proceso corre dos veces a la vez
     * (el scheduler y una ejecucion manual, o dos instancias de la aplicacion), la segunda
     * espera a que termine la primera y despues ve lo que la primera ya registro. Lo usa
     * tambien el ciclo de vida, para que reconocer y escalar no se crucen.
     *
     * @param id el incidente.
     * @return el incidente bloqueado, o vacio si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select incidente from Incidente incidente where incidente.id = :id")
    Optional<Incidente> bloquear(@Param("id") Long id);

    /**
     * El incidente, sin bloquear. Para consultar el estado de la cadena.
     *
     * @param id el incidente.
     * @return el incidente, o vacio si no existe.
     */
    @Query("select incidente from Incidente incidente where incidente.id = :id")
    Optional<Incidente> buscar(@Param("id") Long id);

    /**
     * Candidatos a escalar: en el estado dado y sin reconocer, los mas viejos primero. Es un
     * filtro grueso; los demas cortes (silencio, agotado) los decide la regla.
     *
     * @param estado el estado en el que se escala.
     * @return los ids de los candidatos.
     */
    @Query(
        """
        select incidente.id from Incidente incidente
        where incidente.estado = :estado and incidente.reconocidoEn is null
        order by incidente.detectadoEn asc, incidente.id asc
        """
    )
    List<Long> buscarIdsSinReconocer(@Param("estado") EstadoIncidente estado);

    /**
     * Todo lo que el escalamiento ya hizo sobre el incidente, en orden.
     *
     * @param incidenteId el incidente.
     * @return los escalones ejecutados, el mas viejo primero.
     */
    @Query(
        """
        select ejecucion from EjecucionDeEscalamiento ejecucion
            left join fetch ejecucion.destinatario
            left join fetch ejecucion.paso
        where ejecucion.incidente.id = :incidenteId
        order by ejecucion.ejecutadoEn asc, ejecucion.id asc
        """
    )
    List<EjecucionDeEscalamiento> buscarHistorial(@Param("incidenteId") Long incidenteId);

    /**
     * Las politicas de un conjunto de servicios, con sus pasos y a quien apunta cada uno.
     *
     * @param servicioIds los servicios afectados por el incidente.
     * @return las politicas.
     */
    @Query(
        """
        select distinct politica from PoliticaEscalamiento politica
            left join fetch politica.pasos paso
            left join fetch paso.rotacion
            left join fetch paso.destinatarioDirecto
        where politica.servicio.id in :servicioIds
        """
    )
    List<PoliticaEscalamiento> buscarPoliticasDeServicios(@Param("servicioIds") Collection<Long> servicioIds);
}
