package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO;
import ar.edu.um.isa.oncall.service.dto.ResumenDeCicloDTO;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Una corrida del proceso de escalamiento: recorre los incidentes candidatos y avanza la
 * cadena de cada uno.
 *
 * <p>Deliberadamente no es transaccional: cada incidente se procesa en su propia transaccion
 * (la de {@link EscalamientoService#procesar}), asi un incidente con datos rotos no frena ni
 * deshace el escalamiento de los demas.</p>
 */
@Service
public class EjecutorDeEscalamientos {

    private static final Logger LOG = LoggerFactory.getLogger(EjecutorDeEscalamientos.class);

    private final EscalamientoService escalamientoService;

    public EjecutorDeEscalamientos(EscalamientoService escalamientoService) {
        this.escalamientoService = escalamientoService;
    }

    /**
     * Ejecuta una corrida.
     *
     * @param ahora el instante de la corrida.
     * @return que se hizo.
     */
    public ResumenDeCicloDTO ejecutarCiclo(Instant ahora) {
        List<Long> candidatos = escalamientoService.incidentesParaEscalar();
        int conNovedad = 0;
        int registros = 0;
        int errores = 0;

        for (Long incidenteId : candidatos) {
            try {
                int nuevos = escalamientoService
                    .procesar(incidenteId, ahora)
                    .map(EstadoDeEscalamientoDTO::ejecutadosAhora)
                    .map(List::size)
                    .orElse(0);
                if (nuevos > 0) {
                    conNovedad++;
                    registros += nuevos;
                }
            } catch (RuntimeException e) {
                errores++;
                LOG.error("No se pudo escalar el incidente {}", incidenteId, e);
            }
        }

        if (registros > 0 || errores > 0) {
            LOG.info(
                "Escalamiento: {} incidentes revisados, {} con novedad, {} registros, {} errores",
                candidatos.size(),
                conNovedad,
                registros,
                errores
            );
        }
        return new ResumenDeCicloDTO(ahora, candidatos.size(), conNovedad, registros, errores);
    }
}
