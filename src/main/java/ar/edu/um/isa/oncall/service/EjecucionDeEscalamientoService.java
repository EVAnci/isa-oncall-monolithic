package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import ar.edu.um.isa.oncall.repository.EjecucionDeEscalamientoRepository;
import ar.edu.um.isa.oncall.service.dto.EjecucionDeEscalamientoDTO;
import ar.edu.um.isa.oncall.service.mapper.EjecucionDeEscalamientoMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento}.
 */
@Service
@Transactional
public class EjecucionDeEscalamientoService {

    private static final Logger LOG = LoggerFactory.getLogger(EjecucionDeEscalamientoService.class);

    private final EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository;

    private final EjecucionDeEscalamientoMapper ejecucionDeEscalamientoMapper;

    public EjecucionDeEscalamientoService(
        EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository,
        EjecucionDeEscalamientoMapper ejecucionDeEscalamientoMapper
    ) {
        this.ejecucionDeEscalamientoRepository = ejecucionDeEscalamientoRepository;
        this.ejecucionDeEscalamientoMapper = ejecucionDeEscalamientoMapper;
    }

    /**
     * Save a ejecucionDeEscalamiento.
     *
     * @param ejecucionDeEscalamientoDTO the entity to save.
     * @return the persisted entity.
     */
    public EjecucionDeEscalamientoDTO save(EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO) {
        LOG.debug("Request to save EjecucionDeEscalamiento : {}", ejecucionDeEscalamientoDTO);
        EjecucionDeEscalamiento ejecucionDeEscalamiento = ejecucionDeEscalamientoMapper.toEntity(ejecucionDeEscalamientoDTO);
        ejecucionDeEscalamiento = ejecucionDeEscalamientoRepository.save(ejecucionDeEscalamiento);
        return ejecucionDeEscalamientoMapper.toDto(ejecucionDeEscalamiento);
    }

    /**
     * Update a ejecucionDeEscalamiento.
     *
     * @param ejecucionDeEscalamientoDTO the entity to save.
     * @return the persisted entity.
     */
    public EjecucionDeEscalamientoDTO update(EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO) {
        LOG.debug("Request to update EjecucionDeEscalamiento : {}", ejecucionDeEscalamientoDTO);
        EjecucionDeEscalamiento ejecucionDeEscalamiento = ejecucionDeEscalamientoMapper.toEntity(ejecucionDeEscalamientoDTO);
        ejecucionDeEscalamiento = ejecucionDeEscalamientoRepository.save(ejecucionDeEscalamiento);
        return ejecucionDeEscalamientoMapper.toDto(ejecucionDeEscalamiento);
    }

    /**
     * Partially update a ejecucionDeEscalamiento.
     *
     * @param ejecucionDeEscalamientoDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EjecucionDeEscalamientoDTO> partialUpdate(EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO) {
        LOG.debug("Request to partially update EjecucionDeEscalamiento : {}", ejecucionDeEscalamientoDTO);

        return ejecucionDeEscalamientoRepository
            .findById(ejecucionDeEscalamientoDTO.getId())
            .map(existingEjecucionDeEscalamiento -> {
                ejecucionDeEscalamientoMapper.partialUpdate(existingEjecucionDeEscalamiento, ejecucionDeEscalamientoDTO);

                return existingEjecucionDeEscalamiento;
            })
            .map(ejecucionDeEscalamientoRepository::save)
            .map(ejecucionDeEscalamientoMapper::toDto);
    }

    /**
     * Get all the ejecucionDeEscalamientos.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<EjecucionDeEscalamientoDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all EjecucionDeEscalamientos");
        return ejecucionDeEscalamientoRepository.findAll(pageable).map(ejecucionDeEscalamientoMapper::toDto);
    }

    /**
     * Get all the ejecucionDeEscalamientos with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<EjecucionDeEscalamientoDTO> findAllWithEagerRelationships(Pageable pageable) {
        return ejecucionDeEscalamientoRepository.findAllWithEagerRelationships(pageable).map(ejecucionDeEscalamientoMapper::toDto);
    }

    /**
     * Get one ejecucionDeEscalamiento by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EjecucionDeEscalamientoDTO> findOne(Long id) {
        LOG.debug("Request to get EjecucionDeEscalamiento : {}", id);
        return ejecucionDeEscalamientoRepository.findOneWithEagerRelationships(id).map(ejecucionDeEscalamientoMapper::toDto);
    }

    /**
     * Delete the ejecucionDeEscalamiento by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete EjecucionDeEscalamiento : {}", id);
        ejecucionDeEscalamientoRepository.deleteById(id);
    }
}
