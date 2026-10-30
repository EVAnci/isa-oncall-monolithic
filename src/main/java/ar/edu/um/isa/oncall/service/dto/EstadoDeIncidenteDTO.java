package ar.edu.um.isa.oncall.service.dto;

import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import java.io.Serializable;
import java.time.Instant;

/**
 * Como quedo un incidente despues de una accion de su ciclo de vida.
 *
 * @param incidenteId el incidente.
 * @param estado el estado resultante.
 * @param detectadoEn marca de deteccion.
 * @param reconocidoEn marca de reconocimiento.
 * @param mitigadoEn marca de mitigacion.
 * @param resueltoEn marca de resolucion.
 * @param silenciadoHasta hasta cuando esta en pausa el escalamiento, si lo esta.
 * @param comandante el login del comandante.
 * @param cumplioObjetivo el veredicto de la regla de MTTA / MTTR, si ya hay.
 * @param cambio {@code false} si la accion no cambio nada (por ejemplo, reconocer dos veces).
 * @param mensaje que se hizo, en castellano.
 */
public record EstadoDeIncidenteDTO(
    Long incidenteId,
    EstadoIncidente estado,
    Instant detectadoEn,
    Instant reconocidoEn,
    Instant mitigadoEn,
    Instant resueltoEn,
    Instant silenciadoHasta,
    String comandante,
    Boolean cumplioObjetivo,
    boolean cambio,
    String mensaje
) implements Serializable {}
