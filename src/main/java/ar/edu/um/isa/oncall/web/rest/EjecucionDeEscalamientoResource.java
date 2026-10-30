package ar.edu.um.isa.oncall.web.rest;

import ar.edu.um.isa.oncall.repository.EjecucionDeEscalamientoRepository;
import ar.edu.um.isa.oncall.service.EjecucionDeEscalamientoService;
import ar.edu.um.isa.oncall.service.dto.EjecucionDeEscalamientoDTO;
import ar.edu.um.isa.oncall.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento}.
 */
@RestController
@RequestMapping("/api/ejecucion-de-escalamientos")
public class EjecucionDeEscalamientoResource {

    private static final Logger LOG = LoggerFactory.getLogger(EjecucionDeEscalamientoResource.class);

    private static final String ENTITY_NAME = "ejecucionDeEscalamiento";

    @Value("${jhipster.clientApp.name:oncall}")
    private String applicationName;

    private final EjecucionDeEscalamientoService ejecucionDeEscalamientoService;

    private final EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository;

    public EjecucionDeEscalamientoResource(
        EjecucionDeEscalamientoService ejecucionDeEscalamientoService,
        EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository
    ) {
        this.ejecucionDeEscalamientoService = ejecucionDeEscalamientoService;
        this.ejecucionDeEscalamientoRepository = ejecucionDeEscalamientoRepository;
    }

    /**
     * {@code POST  /ejecucion-de-escalamientos} : Create a new ejecucionDeEscalamiento.
     *
     * @param ejecucionDeEscalamientoDTO the ejecucionDeEscalamientoDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new ejecucionDeEscalamientoDTO, or with status {@code 400 (Bad Request)} if the ejecucionDeEscalamiento has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EjecucionDeEscalamientoDTO> createEjecucionDeEscalamiento(
        @Valid @RequestBody EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save EjecucionDeEscalamiento : {}", ejecucionDeEscalamientoDTO);
        if (ejecucionDeEscalamientoDTO.getId() != null) {
            throw new BadRequestAlertException("A new ejecucionDeEscalamiento cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ejecucionDeEscalamientoDTO = ejecucionDeEscalamientoService.save(ejecucionDeEscalamientoDTO);
        return ResponseEntity.created(new URI("/api/ejecucion-de-escalamientos/" + ejecucionDeEscalamientoDTO.getId()))
            .headers(
                HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, ejecucionDeEscalamientoDTO.getId().toString())
            )
            .body(ejecucionDeEscalamientoDTO);
    }

    /**
     * {@code PUT  /ejecucion-de-escalamientos/:id} : Updates an existing ejecucionDeEscalamiento.
     *
     * @param id the id of the ejecucionDeEscalamientoDTO to save.
     * @param ejecucionDeEscalamientoDTO the ejecucionDeEscalamientoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated ejecucionDeEscalamientoDTO,
     * or with status {@code 400 (Bad Request)} if the ejecucionDeEscalamientoDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the ejecucionDeEscalamientoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EjecucionDeEscalamientoDTO> updateEjecucionDeEscalamiento(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update EjecucionDeEscalamiento : {}, {}", id, ejecucionDeEscalamientoDTO);
        if (ejecucionDeEscalamientoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ejecucionDeEscalamientoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ejecucionDeEscalamientoRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ejecucionDeEscalamientoDTO = ejecucionDeEscalamientoService.update(ejecucionDeEscalamientoDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ejecucionDeEscalamientoDTO.getId().toString()))
            .body(ejecucionDeEscalamientoDTO);
    }

    /**
     * {@code PATCH  /ejecucion-de-escalamientos/:id} : Partial updates given fields of an existing ejecucionDeEscalamiento, field will ignore if it is null
     *
     * @param id the id of the ejecucionDeEscalamientoDTO to save.
     * @param ejecucionDeEscalamientoDTO the ejecucionDeEscalamientoDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated ejecucionDeEscalamientoDTO,
     * or with status {@code 400 (Bad Request)} if the ejecucionDeEscalamientoDTO is not valid,
     * or with status {@code 404 (Not Found)} if the ejecucionDeEscalamientoDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the ejecucionDeEscalamientoDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EjecucionDeEscalamientoDTO> partialUpdateEjecucionDeEscalamiento(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update EjecucionDeEscalamiento partially : {}, {}", id, ejecucionDeEscalamientoDTO);
        if (ejecucionDeEscalamientoDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ejecucionDeEscalamientoDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ejecucionDeEscalamientoRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EjecucionDeEscalamientoDTO> result = ejecucionDeEscalamientoService.partialUpdate(ejecucionDeEscalamientoDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ejecucionDeEscalamientoDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /ejecucion-de-escalamientos} : get all the Ejecucion De Escalamientos.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Ejecucion De Escalamientos in body.
     */
    @GetMapping("")
    public ResponseEntity<List<EjecucionDeEscalamientoDTO>> getAllEjecucionDeEscalamientos(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of EjecucionDeEscalamientos");
        Page<EjecucionDeEscalamientoDTO> page;
        if (eagerload) {
            page = ejecucionDeEscalamientoService.findAllWithEagerRelationships(pageable);
        } else {
            page = ejecucionDeEscalamientoService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /ejecucion-de-escalamientos/:id} : get the "id" ejecucionDeEscalamiento.
     *
     * @param id the id of the ejecucionDeEscalamientoDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the ejecucionDeEscalamientoDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EjecucionDeEscalamientoDTO> getEjecucionDeEscalamiento(@PathVariable("id") Long id) {
        LOG.debug("REST request to get EjecucionDeEscalamiento : {}", id);
        Optional<EjecucionDeEscalamientoDTO> ejecucionDeEscalamientoDTO = ejecucionDeEscalamientoService.findOne(id);
        return ResponseUtil.wrapOrNotFound(ejecucionDeEscalamientoDTO);
    }

    /**
     * {@code DELETE  /ejecucion-de-escalamientos/:id} : delete the "id" ejecucionDeEscalamiento.
     *
     * @param id the id of the ejecucionDeEscalamientoDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEjecucionDeEscalamiento(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete EjecucionDeEscalamiento : {}", id);
        ejecucionDeEscalamientoService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
