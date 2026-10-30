package ar.edu.um.isa.oncall.service.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Lo que hizo una corrida del proceso de escalamiento.
 *
 * @param ejecutadoEn el instante de la corrida.
 * @param incidentesRevisados cuantos incidentes abiertos y sin reconocer se miraron.
 * @param incidentesConNovedad en cuantos se registro algo.
 * @param registros cuantas filas de {@code EjecucionDeEscalamiento} se escribieron.
 * @param errores en cuantos incidentes fallo el proceso. Un error no frena a los demas.
 */
public record ResumenDeCicloDTO(
    Instant ejecutadoEn,
    int incidentesRevisados,
    int incidentesConNovedad,
    int registros,
    int errores
) implements Serializable {}
