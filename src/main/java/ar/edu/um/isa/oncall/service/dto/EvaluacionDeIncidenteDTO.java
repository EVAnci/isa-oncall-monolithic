package ar.edu.um.isa.oncall.service.dto;

import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.Severidad;
import ar.edu.um.isa.oncall.domain.enumeration.TipoObjetivo;
import java.io.Serializable;
import java.time.Instant;

/**
 * Como le fue a un incidente contra los objetivos de su servicio.
 *
 * @param incidenteId el incidente.
 * @param titulo su titulo.
 * @param severidad la severidad, que es la que elige que objetivo aplica.
 * @param estado el estado del incidente al momento de evaluar.
 * @param detectadoEn el punto de partida de las dos mediciones.
 * @param evaluadoEn el instante contra el que se midieron los incidentes todavia abiertos.
 * @param reconocimiento el tiempo hasta reconocer, contra {@code TIEMPO_DE_RECONOCIMIENTO}.
 * @param resolucion el tiempo hasta resolver, contra {@code TIEMPO_DE_RESOLUCION}.
 * @param cumplioObjetivo veredicto global: {@code true} si cumplio todo lo que tenia que
 *        cumplir, {@code false} si incumplio algo, {@code null} si todavia no se sabe o no
 *        habia contra que medir.
 */
public record EvaluacionDeIncidenteDTO(
    Long incidenteId,
    String titulo,
    Severidad severidad,
    EstadoIncidente estado,
    Instant detectadoEn,
    Instant evaluadoEn,
    Medicion reconocimiento,
    Medicion resolucion,
    Boolean cumplioObjetivo
) implements Serializable {
    /**
     * El resultado de medir un tiempo contra un objetivo.
     */
    public enum EstadoMedicion {
        /** Termino dentro del objetivo. */
        CUMPLIDO,
        /** Termino fuera del objetivo, o todavia no termino y el objetivo ya vencio. */
        INCUMPLIDO,
        /** Todavia no termino y el objetivo todavia no vencio. */
        EN_CURSO,
        /** El servicio no define objetivo para esta severidad: se mide, pero no se juzga. */
        SIN_OBJETIVO,
        /** Las marcas de tiempo no cierran (por ejemplo, resuelto antes de detectado). */
        INCONSISTENTE,
    }

    /**
     * Una medicion.
     *
     * @param tipo que se midio.
     * @param estado el resultado.
     * @param hasta la marca de tiempo que cierra la medicion, o {@code null} si sigue abierta.
     * @param implicito {@code true} si {@code hasta} no se cargo explicitamente y se dedujo de
     *        otra marca (por ejemplo, un incidente resuelto sin reconocer se da por reconocido
     *        al resolverse).
     * @param segundos la duracion medida, o {@code null} si sigue abierta.
     * @param minutosObjetivo el objetivo aplicado, o {@code null} si no hay.
     * @param objetivoId el {@code ObjetivoDeServicio} aplicado.
     * @param servicioDelObjetivo el servicio al que pertenece ese objetivo.
     */
    public record Medicion(
        TipoObjetivo tipo,
        EstadoMedicion estado,
        Instant hasta,
        boolean implicito,
        Long segundos,
        Integer minutosObjetivo,
        Long objetivoId,
        String servicioDelObjetivo
    ) implements Serializable {}
}
