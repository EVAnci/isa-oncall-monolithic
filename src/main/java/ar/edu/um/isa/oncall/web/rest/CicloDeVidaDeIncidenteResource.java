package ar.edu.um.isa.oncall.web.rest;

import ar.edu.um.isa.oncall.security.SecurityUtils;
import ar.edu.um.isa.oncall.service.CicloDeVidaDeIncidenteService;
import ar.edu.um.isa.oncall.service.TransicionInvalidaException;
import ar.edu.um.isa.oncall.service.dto.EstadoDeIncidenteDTO;
import ar.edu.um.isa.oncall.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Las acciones de una persona sobre un incidente. Aparte de {@link IncidenteResource}, que es
 * el ABM generado.
 */
@RestController
@RequestMapping("/api/incidentes")
public class CicloDeVidaDeIncidenteResource {

    private static final Logger LOG = LoggerFactory.getLogger(CicloDeVidaDeIncidenteResource.class);

    private static final String ENTITY_NAME = "incidente";

    private final CicloDeVidaDeIncidenteService cicloDeVidaDeIncidenteService;

    public CicloDeVidaDeIncidenteResource(CicloDeVidaDeIncidenteService cicloDeVidaDeIncidenteService) {
        this.cicloDeVidaDeIncidenteService = cicloDeVidaDeIncidenteService;
    }

    /**
     * Cuanto silenciar.
     *
     * @param minutos de 0 a 1440; 0 levanta el silencio.
     */
    public record SilencioDTO(@NotNull @Min(0) @Max(CicloDeVidaDeIncidenteService.MAX_MINUTOS_DE_SILENCIO) Integer minutos) {}

    /**
     * {@code POST /api/incidentes/:id/reconocer} : el usuario actual reconoce el incidente.
     *
     * @param id el incidente.
     * @return {@code 200} con el incidente, {@code 404} si no existe, {@code 400} si ya termino.
     */
    @PostMapping("/{id}/reconocer")
    public ResponseEntity<EstadoDeIncidenteDTO> reconocer(@PathVariable("id") Long id) {
        LOG.debug("REST request para reconocer el Incidente {}", id);
        return ejecutar(() -> cicloDeVidaDeIncidenteService.reconocer(id, usuarioActual(), Instant.now()));
    }

    /**
     * {@code POST /api/incidentes/:id/mitigar} : el impacto se corto.
     *
     * @param id el incidente.
     * @return {@code 200} con el incidente, {@code 404} si no existe, {@code 400} si ya termino.
     */
    @PostMapping("/{id}/mitigar")
    public ResponseEntity<EstadoDeIncidenteDTO> mitigar(@PathVariable("id") Long id) {
        LOG.debug("REST request para mitigar el Incidente {}", id);
        return ejecutar(() -> cicloDeVidaDeIncidenteService.mitigar(id, usuarioActual(), Instant.now()));
    }

    /**
     * {@code POST /api/incidentes/:id/resolver} : el incidente termino. Registra el
     * cumplimiento de objetivos.
     *
     * @param id el incidente.
     * @return {@code 200} con el incidente, o {@code 404} si no existe.
     */
    @PostMapping("/{id}/resolver")
    public ResponseEntity<EstadoDeIncidenteDTO> resolver(@PathVariable("id") Long id) {
        LOG.debug("REST request para resolver el Incidente {}", id);
        return ejecutar(() -> cicloDeVidaDeIncidenteService.resolver(id, usuarioActual(), Instant.now()));
    }

    /**
     * {@code POST /api/incidentes/:id/silenciar} : pausa el escalamiento.
     *
     * @param id el incidente.
     * @param silencio cuantos minutos.
     * @return {@code 200} con el incidente, {@code 404} si no existe, {@code 400} si ya termino.
     */
    @PostMapping("/{id}/silenciar")
    public ResponseEntity<EstadoDeIncidenteDTO> silenciar(@PathVariable("id") Long id, @Valid @RequestBody SilencioDTO silencio) {
        LOG.debug("REST request para silenciar el Incidente {} durante {} minutos", id, silencio.minutos());
        return ejecutar(() -> cicloDeVidaDeIncidenteService.silenciar(id, silencio.minutos(), usuarioActual(), Instant.now()));
    }

    private ResponseEntity<EstadoDeIncidenteDTO> ejecutar(Supplier<Optional<EstadoDeIncidenteDTO>> accion) {
        try {
            return ResponseUtil.wrapOrNotFound(accion.get());
        } catch (TransicionInvalidaException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "transicioninvalida");
        }
    }

    private static String usuarioActual() {
        return SecurityUtils.getCurrentUserLogin().orElse(null);
    }
}
