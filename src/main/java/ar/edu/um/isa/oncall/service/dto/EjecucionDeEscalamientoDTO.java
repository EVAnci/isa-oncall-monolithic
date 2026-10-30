package ar.edu.um.isa.oncall.service.dto;

import ar.edu.um.isa.oncall.domain.enumeration.ResultadoEscalamiento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento} entity.
 */
@Schema(
    description = "Cada escalon que la cadena de escalamiento ejecuto sobre un incidente.\nEs la auditoria del escalamiento y tambien su memoria: el proceso que\ndespierta mira la ultima fila para saber por donde va."
)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EjecucionDeEscalamientoDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(value = 0)
    @Max(value = 5)
    private Integer vuelta;

    @NotNull
    @Min(value = 0)
    @Max(value = 10)
    private Integer nivel;

    @NotNull
    private ResultadoEscalamiento resultado;

    @NotNull
    private Instant ejecutadoEn;

    private Instant proximoEn;

    @Size(max = 500)
    private String detalle;

    @NotNull
    private IncidenteDTO incidente;

    private PasoEscalamientoDTO paso;

    private UserDTO destinatario;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVuelta() {
        return vuelta;
    }

    public void setVuelta(Integer vuelta) {
        this.vuelta = vuelta;
    }

    public Integer getNivel() {
        return nivel;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public ResultadoEscalamiento getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoEscalamiento resultado) {
        this.resultado = resultado;
    }

    public Instant getEjecutadoEn() {
        return ejecutadoEn;
    }

    public void setEjecutadoEn(Instant ejecutadoEn) {
        this.ejecutadoEn = ejecutadoEn;
    }

    public Instant getProximoEn() {
        return proximoEn;
    }

    public void setProximoEn(Instant proximoEn) {
        this.proximoEn = proximoEn;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public IncidenteDTO getIncidente() {
        return incidente;
    }

    public void setIncidente(IncidenteDTO incidente) {
        this.incidente = incidente;
    }

    public PasoEscalamientoDTO getPaso() {
        return paso;
    }

    public void setPaso(PasoEscalamientoDTO paso) {
        this.paso = paso;
    }

    public UserDTO getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(UserDTO destinatario) {
        this.destinatario = destinatario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EjecucionDeEscalamientoDTO)) {
            return false;
        }

        EjecucionDeEscalamientoDTO ejecucionDeEscalamientoDTO = (EjecucionDeEscalamientoDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, ejecucionDeEscalamientoDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EjecucionDeEscalamientoDTO{" +
            "id=" + getId() +
            ", vuelta=" + getVuelta() +
            ", nivel=" + getNivel() +
            ", resultado='" + getResultado() + "'" +
            ", ejecutadoEn='" + getEjecutadoEn() + "'" +
            ", proximoEn='" + getProximoEn() + "'" +
            ", detalle='" + getDetalle() + "'" +
            ", incidente=" + getIncidente() +
            ", paso=" + getPaso() +
            ", destinatario=" + getDestinatario() +
            "}";
    }
}
