package ar.edu.um.isa.oncall.service.dto;

import ar.edu.um.isa.oncall.domain.enumeration.ResultadoEscalamiento;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Por donde va la cadena de escalamiento de un incidente.
 *
 * @param incidenteId el incidente.
 * @param estado en que situacion esta la cadena.
 * @param politicaId la politica que se le aplica, o {@code null} si no tiene.
 * @param politicaNombre su nombre.
 * @param vuelta la vuelta del ultimo escalon ejecutado, empezando en 0; {@code null} si no arranco.
 * @param nivel el {@code orden} del ultimo paso ejecutado; {@code null} si no arranco.
 * @param proximoEscalamientoEn cuando se ejecuta el proximo escalon si nadie reconoce, o
 *        {@code null} si la cadena esta cortada.
 * @param ejecutadosAhora los escalones que se ejecutaron en esta corrida. Vacio en una consulta.
 * @param historial todos los escalones ejecutados sobre el incidente.
 */
public record EstadoDeEscalamientoDTO(
    Long incidenteId,
    Estado estado,
    Long politicaId,
    String politicaNombre,
    Integer vuelta,
    Integer nivel,
    Instant proximoEscalamientoEn,
    List<Escalon> ejecutadosAhora,
    List<Escalon> historial
) implements Serializable {
    /**
     * Situacion de la cadena. Todas menos {@code EN_CURSO} y {@code PENDIENTE} son cortes.
     */
    public enum Estado {
        /** Detectado en el futuro: todavia no hay nada que escalar. */
        PENDIENTE,
        /** La cadena avanza: o esta esperando que venza un paso, o acaba de ejecutar uno. */
        EN_CURSO,
        /** Alguien reconocio (o mitigo) el incidente. */
        RECONOCIDO,
        /** El incidente esta resuelto o cerrado. */
        RESUELTO,
        /** Silenciado: la cadena esta en pausa hasta {@code silenciadoHasta}. */
        SILENCIADO,
        /** Se recorrieron todos los niveles y todas las repeticiones sin respuesta. */
        AGOTADO,
        /** Ninguno de los servicios del incidente tiene una politica con pasos. */
        SIN_POLITICA,
    }

    /**
     * Un escalon ejecutado.
     */
    public record Escalon(
        Long ejecucionId,
        Integer vuelta,
        Integer nivel,
        ResultadoEscalamiento resultado,
        Instant ejecutadoEn,
        Instant proximoEn,
        String destinatario,
        String detalle
    ) implements Serializable {}
}
