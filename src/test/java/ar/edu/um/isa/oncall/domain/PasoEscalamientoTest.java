package ar.edu.um.isa.oncall.domain;

import static ar.edu.um.isa.oncall.domain.EjecucionDeEscalamientoTestSamples.*;
import static ar.edu.um.isa.oncall.domain.PasoEscalamientoTestSamples.*;
import static ar.edu.um.isa.oncall.domain.PoliticaEscalamientoTestSamples.*;
import static ar.edu.um.isa.oncall.domain.RotacionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.isa.oncall.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PasoEscalamientoTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PasoEscalamiento.class);
        PasoEscalamiento pasoEscalamiento1 = getPasoEscalamientoSample1();
        PasoEscalamiento pasoEscalamiento2 = new PasoEscalamiento();
        assertThat(pasoEscalamiento1).isNotEqualTo(pasoEscalamiento2);

        pasoEscalamiento2.setId(pasoEscalamiento1.getId());
        assertThat(pasoEscalamiento1).isEqualTo(pasoEscalamiento2);

        pasoEscalamiento2 = getPasoEscalamientoSample2();
        assertThat(pasoEscalamiento1).isNotEqualTo(pasoEscalamiento2);
    }

    @Test
    void politicaTest() {
        PasoEscalamiento pasoEscalamiento = getPasoEscalamientoRandomSampleGenerator();
        PoliticaEscalamiento politicaEscalamientoBack = getPoliticaEscalamientoRandomSampleGenerator();

        pasoEscalamiento.setPolitica(politicaEscalamientoBack);
        assertThat(pasoEscalamiento.getPolitica()).isEqualTo(politicaEscalamientoBack);

        pasoEscalamiento.politica(null);
        assertThat(pasoEscalamiento.getPolitica()).isNull();
    }

    @Test
    void rotacionTest() {
        PasoEscalamiento pasoEscalamiento = getPasoEscalamientoRandomSampleGenerator();
        Rotacion rotacionBack = getRotacionRandomSampleGenerator();

        pasoEscalamiento.setRotacion(rotacionBack);
        assertThat(pasoEscalamiento.getRotacion()).isEqualTo(rotacionBack);

        pasoEscalamiento.rotacion(null);
        assertThat(pasoEscalamiento.getRotacion()).isNull();
    }

    @Test
    void ejecucionTest() {
        PasoEscalamiento pasoEscalamiento = getPasoEscalamientoRandomSampleGenerator();
        EjecucionDeEscalamiento ejecucionDeEscalamientoBack = getEjecucionDeEscalamientoRandomSampleGenerator();

        pasoEscalamiento.addEjecucion(ejecucionDeEscalamientoBack);
        assertThat(pasoEscalamiento.getEjecucions()).containsOnly(ejecucionDeEscalamientoBack);
        assertThat(ejecucionDeEscalamientoBack.getPaso()).isEqualTo(pasoEscalamiento);

        pasoEscalamiento.removeEjecucion(ejecucionDeEscalamientoBack);
        assertThat(pasoEscalamiento.getEjecucions()).doesNotContain(ejecucionDeEscalamientoBack);
        assertThat(ejecucionDeEscalamientoBack.getPaso()).isNull();

        pasoEscalamiento.ejecucions(new HashSet<>(Set.of(ejecucionDeEscalamientoBack)));
        assertThat(pasoEscalamiento.getEjecucions()).containsOnly(ejecucionDeEscalamientoBack);
        assertThat(ejecucionDeEscalamientoBack.getPaso()).isEqualTo(pasoEscalamiento);

        pasoEscalamiento.setEjecucions(new HashSet<>());
        assertThat(pasoEscalamiento.getEjecucions()).doesNotContain(ejecucionDeEscalamientoBack);
        assertThat(ejecucionDeEscalamientoBack.getPaso()).isNull();
    }
}
