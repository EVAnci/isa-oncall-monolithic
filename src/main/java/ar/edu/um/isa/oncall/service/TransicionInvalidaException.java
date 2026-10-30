package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;

/**
 * Se pidio una accion que el estado del incidente no admite, como reconocer uno ya resuelto.
 */
public class TransicionInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransicionInvalidaException(Long incidenteId, EstadoIncidente estado, String accion) {
        super("No se puede " + accion + " el incidente " + incidenteId + ": esta " + estado);
    }
}
