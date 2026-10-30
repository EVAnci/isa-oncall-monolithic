package ar.edu.um.isa.oncall.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.isa.oncall.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EjecucionDeEscalamientoDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(EjecucionDeEscalamientoDTO.class);
        EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO1 = new EjecucionDeEscalamientoDTO();
        ejecucionDeEscalamientoDTO1.setId(1L);
        EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO2 = new EjecucionDeEscalamientoDTO();
        assertThat(ejecucionDeEscalamientoDTO1).isNotEqualTo(ejecucionDeEscalamientoDTO2);
        ejecucionDeEscalamientoDTO2.setId(ejecucionDeEscalamientoDTO1.getId());
        assertThat(ejecucionDeEscalamientoDTO1).isEqualTo(ejecucionDeEscalamientoDTO2);
        ejecucionDeEscalamientoDTO2.setId(2L);
        assertThat(ejecucionDeEscalamientoDTO1).isNotEqualTo(ejecucionDeEscalamientoDTO2);
        ejecucionDeEscalamientoDTO1.setId(null);
        assertThat(ejecucionDeEscalamientoDTO1).isNotEqualTo(ejecucionDeEscalamientoDTO2);
    }
}
