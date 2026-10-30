package ar.edu.um.isa.oncall.service.dto;

import ar.edu.um.isa.oncall.domain.enumeration.RolDeGuardia;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Respuesta de la regla de guardia vigente: quien atiende una rotacion en un instante dado.
 *
 * @param rotacionId la rotacion consultada.
 * @param rotacionNombre su nombre, para leer la respuesta sin ir a buscarlo.
 * @param zonaHoraria la zona de la rotacion, con la que se expresa {@code horaLocal}.
 * @param instante el instante resuelto, en UTC.
 * @param horaLocal el mismo instante en la zona de la rotacion.
 * @param cobertura si hay alguien de guardia y por que via.
 * @param primario quien responde primero, o {@code null} si no hay cobertura.
 * @param secundario el respaldo del primario, o {@code null} si no hay.
 * @param motivo explicacion en castellano de la decision.
 */
public record GuardiaVigenteDTO(
    Long rotacionId,
    String rotacionNombre,
    String zonaHoraria,
    Instant instante,
    OffsetDateTime horaLocal,
    Cobertura cobertura,
    TurnoVigente primario,
    TurnoVigente secundario,
    String motivo
) implements Serializable {
    /**
     * Las respuestas posibles. Un hueco no es una excepcion: es una respuesta mas.
     */
    public enum Cobertura {
        /** Hay un turno primario vigente. */
        CUBIERTA,
        /** No hay primario, pero si un secundario: atiende el secundario. */
        CUBIERTA_POR_SECUNDARIO,
        /** Ningun turno cubre el instante. */
        SIN_COBERTURA,
        /** La rotacion esta dada de baja: no responde por nadie. */
        ROTACION_INACTIVA,
    }

    /**
     * Un turno elegido por la regla, con la persona responsable.
     */
    public record TurnoVigente(
        Long turnoId,
        Long usuarioId,
        String login,
        Instant desde,
        Instant hasta,
        RolDeGuardia rol,
        boolean esReemplazo
    ) implements Serializable {}
}
