package ar.edu.um.isa.oncall.repository;

import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.ObjetivoDeServicio;
import ar.edu.um.isa.oncall.domain.enumeration.TipoObjetivo;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Consultas que necesita la regla de MTTA / MTTR. Aparte de los repositorios generados, por
 * la misma razon que {@link IncidenteDeduplicacionRepository}.
 */
@Repository
public interface MetricasDeIncidenteRepository extends org.springframework.data.repository.Repository<Incidente, Long> {
    /**
     * El incidente con sus servicios, que son los que definen que objetivos le aplican.
     *
     * @param id el incidente.
     * @return el incidente, o vacio si no existe.
     */
    @Query("select incidente from Incidente incidente left join fetch incidente.servicios where incidente.id = :id")
    Optional<Incidente> buscarConServicios(@Param("id") Long id);

    /**
     * Incidentes detectados en el periodo {@code [desde, hasta)}, con sus servicios.
     *
     * @param desde inicio del periodo, inclusive.
     * @param hasta fin del periodo, exclusive.
     * @return los incidentes del periodo.
     */
    @Query(
        """
        select distinct incidente from Incidente incidente
            left join fetch incidente.servicios
        where incidente.detectadoEn >= :desde and incidente.detectadoEn < :hasta
        """
    )
    List<Incidente> buscarDetectadosEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * Igual que {@link #buscarDetectadosEntre}, pero solo los que afectaron a un servicio. Se
     * traen igual todos sus servicios: el objetivo que aplica puede ser el de otro de ellos.
     *
     * @param desde inicio del periodo, inclusive.
     * @param hasta fin del periodo, exclusive.
     * @param servicioId el servicio afectado.
     * @return los incidentes del periodo sobre ese servicio.
     */
    @Query(
        """
        select distinct incidente from Incidente incidente
            left join fetch incidente.servicios
        where incidente.detectadoEn >= :desde and incidente.detectadoEn < :hasta
            and exists (
                select 1 from Incidente afectado join afectado.servicios servicio
                where afectado.id = incidente.id and servicio.id = :servicioId
            )
        """
    )
    List<Incidente> buscarDetectadosEntreSobreServicio(
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        @Param("servicioId") Long servicioId
    );

    /**
     * Los objetivos de tiempo de un conjunto de servicios. La severidad se filtra despues, en
     * la regla, para poder resolver muchos incidentes con una sola consulta.
     *
     * @param servicioIds los servicios.
     * @param tipos los tipos de objetivo que se miden.
     * @return los objetivos, con su servicio.
     */
    @Query(
        """
        select objetivo from ObjetivoDeServicio objetivo
            join fetch objetivo.servicio servicio
        where servicio.id in :servicioIds and objetivo.tipo in :tipos
        """
    )
    List<ObjetivoDeServicio> buscarObjetivos(
        @Param("servicioIds") Collection<Long> servicioIds,
        @Param("tipos") Collection<TipoObjetivo> tipos
    );
}
