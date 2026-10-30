package ar.edu.um.isa.oncall.service.mapper;

import static ar.edu.um.isa.oncall.domain.EjecucionDeEscalamientoAsserts.*;
import static ar.edu.um.isa.oncall.domain.EjecucionDeEscalamientoTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EjecucionDeEscalamientoMapperTest {

    private EjecucionDeEscalamientoMapper ejecucionDeEscalamientoMapper;

    @BeforeEach
    void setUp() {
        ejecucionDeEscalamientoMapper = new EjecucionDeEscalamientoMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getEjecucionDeEscalamientoSample1();
        var actual = ejecucionDeEscalamientoMapper.toEntity(ejecucionDeEscalamientoMapper.toDto(expected));
        assertEjecucionDeEscalamientoAllPropertiesEquals(expected, actual);
    }
}
