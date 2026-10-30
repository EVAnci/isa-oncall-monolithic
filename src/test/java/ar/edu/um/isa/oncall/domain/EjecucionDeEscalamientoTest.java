package ar.edu.um.isa.oncall.domain;

import static ar.edu.um.isa.oncall.domain.EjecucionDeEscalamientoTestSamples.*;
import static ar.edu.um.isa.oncall.domain.IncidenteTestSamples.*;
import static ar.edu.um.isa.oncall.domain.PasoEscalamientoTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.isa.oncall.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class EjecucionDeEscalamientoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(EjecucionDeEscalamiento.class);
        EjecucionDeEscalamiento ejecucionDeEscalamiento1 = getEjecucionDeEscalamientoSample1();
        EjecucionDeEscalamiento ejecucionDeEscalamiento2 = new EjecucionDeEscalamiento();
        assertThat(ejecucionDeEscalamiento1).isNotEqualTo(ejecucionDeEscalamiento2);

        ejecucionDeEscalamiento2.setId(ejecucionDeEscalamiento1.getId());
        assertThat(ejecucionDeEscalamiento1).isEqualTo(ejecucionDeEscalamiento2);

        ejecucionDeEscalamiento2 = getEjecucionDeEscalamientoSample2();
        assertThat(ejecucionDeEscalamiento1).isNotEqualTo(ejecucionDeEscalamiento2);
    }

    @Test
    void incidenteTest() {
        EjecucionDeEscalamiento ejecucionDeEscalamiento = getEjecucionDeEscalamientoRandomSampleGenerator();
        Incidente incidenteBack = getIncidenteRandomSampleGenerator();

        ejecucionDeEscalamiento.setIncidente(incidenteBack);
        assertThat(ejecucionDeEscalamiento.getIncidente()).isEqualTo(incidenteBack);

        ejecucionDeEscalamiento.incidente(null);
        assertThat(ejecucionDeEscalamiento.getIncidente()).isNull();
    }

    @Test
    void pasoTest() {
        EjecucionDeEscalamiento ejecucionDeEscalamiento = getEjecucionDeEscalamientoRandomSampleGenerator();
        PasoEscalamiento pasoEscalamientoBack = getPasoEscalamientoRandomSampleGenerator();

        ejecucionDeEscalamiento.setPaso(pasoEscalamientoBack);
        assertThat(ejecucionDeEscalamiento.getPaso()).isEqualTo(pasoEscalamientoBack);

        ejecucionDeEscalamiento.paso(null);
        assertThat(ejecucionDeEscalamiento.getPaso()).isNull();
    }
}
