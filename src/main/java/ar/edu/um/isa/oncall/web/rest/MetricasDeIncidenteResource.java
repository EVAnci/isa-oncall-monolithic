package ar.edu.um.isa.oncall.web.rest;

import ar.edu.um.isa.oncall.service.MetricasDeIncidenteService;
import ar.edu.um.isa.oncall.service.dto.EvaluacionDeIncidenteDTO;
import ar.edu.um.isa.oncall.service.dto.ReporteDeMetricasDTO;
import ar.edu.um.isa.oncall.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Endpoints de la regla de MTTA / MTTR. Aparte de {@link IncidenteResource}, que es el ABM.
 */
@RestController
@RequestMapping("/api")
public class MetricasDeIncidenteResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetricasDeIncidenteResource.class);

    private static final String ENTITY_NAME = "incidente";

    private final MetricasDeIncidenteService metricasDeIncidenteService;

    public MetricasDeIncidenteResource(MetricasDeIncidenteService metricasDeIncidenteService) {
        this.metricasDeIncidenteService = metricasDeIncidenteService;
    }

    /**
     * {@code GET /api/incidentes/:id/metricas} : tiempos del incidente contra sus objetivos.
     * No modifica nada.
     *
     * @param id el incidente.
     * @return {@code 200} con la evaluacion, o {@code 404} si el incidente no existe.
     */
    @GetMapping("/incidentes/{id}/metricas")
    public ResponseEntity<EvaluacionDeIncidenteDTO> evaluarIncidente(@PathVariable("id") Long id) {
        LOG.debug("REST request para evaluar el Incidente {}", id);
        return ResponseUtil.wrapOrNotFound(metricasDeIncidenteService.evaluar(id, Instant.now()));
    }

    /**
     * {@code POST /api/incidentes/:id/metricas/cumplimiento} : evalua el incidente y, si ya
     * termino, guarda el veredicto en {@code cumplioObjetivo}. Sirve para los incidentes que se
     * cerraron por el ABM y no por {@code /resolver}.
     *
     * @param id el incidente.
     * @return {@code 200} con la evaluacion, o {@code 404} si el incidente no existe.
     */
    @PostMapping("/incidentes/{id}/metricas/cumplimiento")
    public ResponseEntity<EvaluacionDeIncidenteDTO> registrarCumplimiento(@PathVariable("id") Long id) {
        LOG.debug("REST request para registrar el cumplimiento del Incidente {}", id);
        return ResponseUtil.wrapOrNotFound(metricasDeIncidenteService.registrarCumplimiento(id, Instant.now()));
    }

    /**
     * {@code GET /api/metricas/incidentes} : MTTA, MTTR y cumplimiento de un periodo.
     *
     * @param desde inicio del periodo (por {@code detectadoEn}), inclusive. Por defecto, treinta
     *        dias antes de {@code hasta}.
     * @param hasta fin del periodo, exclusive. Por defecto, ahora.
     * @param servicioId opcional: solo los incidentes que afectaron a ese servicio.
     * @return {@code 200} con el reporte.
     */
    @GetMapping("/metricas/incidentes")
    public ReporteDeMetricasDTO reporte(
        @RequestParam(name = "desde", required = false) Instant desde,
        @RequestParam(name = "hasta", required = false) Instant hasta,
        @RequestParam(name = "servicioId", required = false) Long servicioId
    ) {
        Instant ahora = Instant.now();
        Instant fin = hasta != null ? hasta : ahora;
        Instant inicio = desde != null ? desde : fin.minus(MetricasDeIncidenteService.PERIODO_POR_DEFECTO);
        if (!inicio.isBefore(fin)) {
            throw new BadRequestAlertException("'desde' tiene que ser anterior a 'hasta'", ENTITY_NAME, "periodoinvalido");
        }
        LOG.debug("REST request para el reporte de metricas entre {} y {} (servicio {})", inicio, fin, servicioId);
        return metricasDeIncidenteService.reporte(inicio, fin, servicioId, ahora);
    }
}
