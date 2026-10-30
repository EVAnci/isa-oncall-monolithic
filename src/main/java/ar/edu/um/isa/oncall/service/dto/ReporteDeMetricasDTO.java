package ar.edu.um.isa.oncall.service.dto;

import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * MTTA, MTTR y cumplimiento de objetivos de un periodo.
 *
 * @param desde inicio del periodo, inclusive (por {@code detectadoEn}).
 * @param hasta fin del periodo, exclusive.
 * @param servicioId el servicio filtrado, o {@code null} si son todos.
 * @param total el resumen de todos los incidentes del periodo.
 * @param porSeveridad el mismo resumen abierto por severidad. Solo las que tuvieron incidentes.
 */
public record ReporteDeMetricasDTO(
    Instant desde,
    Instant hasta,
    Long servicioId,
    Resumen total,
    List<Resumen> porSeveridad
) implements Serializable {
    /**
     * Las cuentas de un grupo de incidentes.
     *
     * @param severidad la severidad del grupo, o {@code null} en el total.
     * @param incidentes cuantos incidentes se detectaron.
     * @param reconocidos cuantos tienen tiempo de reconocimiento medible.
     * @param resueltos cuantos tienen tiempo de resolucion medible.
     * @param inconsistentes cuantos se dejaron afuera porque sus marcas de tiempo no cierran.
     * @param mttaMinutos tiempo medio hasta reconocer, en minutos, o {@code null} si no hay datos.
     * @param mttrMinutos tiempo medio hasta resolver, en minutos, o {@code null} si no hay datos.
     * @param cumplimientoReconocimiento porcentaje de incidentes que cumplieron su objetivo de
     *        reconocimiento, sobre los que ya tienen veredicto. {@code null} si ninguno lo tiene.
     * @param cumplimientoResolucion lo mismo para el objetivo de resolucion.
     */
    public record Resumen(
        Severidad severidad,
        long incidentes,
        long reconocidos,
        long resueltos,
        long inconsistentes,
        Double mttaMinutos,
        Double mttrMinutos,
        Double cumplimientoReconocimiento,
        Double cumplimientoResolucion
    ) implements Serializable {}
}
