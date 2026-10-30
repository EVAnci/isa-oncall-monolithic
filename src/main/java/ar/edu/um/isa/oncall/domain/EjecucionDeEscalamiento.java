package ar.edu.um.isa.oncall.domain;

import ar.edu.um.isa.oncall.domain.enumeration.ResultadoEscalamiento;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Cada escalon que la cadena de escalamiento ejecuto sobre un incidente.
 * Es la auditoria del escalamiento y tambien su memoria: el proceso que
 * despierta mira la ultima fila para saber por donde va.
 */
@Entity
@Table(name = "ejecucion_de_escalamiento")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EjecucionDeEscalamiento implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 0)
    @Max(value = 5)
    @Column(name = "vuelta", nullable = false)
    private Integer vuelta;

    @NotNull
    @Min(value = 0)
    @Max(value = 10)
    @Column(name = "nivel", nullable = false)
    private Integer nivel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false)
    private ResultadoEscalamiento resultado;

    @NotNull
    @Column(name = "ejecutado_en", nullable = false)
    private Instant ejecutadoEn;

    @Column(name = "proximo_en")
    private Instant proximoEn;

    @Size(max = 500)
    @Column(name = "detalle", length = 500)
    private String detalle;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = { "comandante", "servicios", "postmortem", "alertas", "eventos", "ejecucionDeEscalamientos", "notificacions" },
        allowSetters = true
    )
    private Incidente incidente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "politica", "rotacion", "destinatarioDirecto", "ejecucions" }, allowSetters = true)
    private PasoEscalamiento paso;

    @ManyToOne(fetch = FetchType.LAZY)
    private User destinatario;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public EjecucionDeEscalamiento id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVuelta() {
        return this.vuelta;
    }

    public EjecucionDeEscalamiento vuelta(Integer vuelta) {
        this.setVuelta(vuelta);
        return this;
    }

    public void setVuelta(Integer vuelta) {
        this.vuelta = vuelta;
    }

    public Integer getNivel() {
        return this.nivel;
    }

    public EjecucionDeEscalamiento nivel(Integer nivel) {
        this.setNivel(nivel);
        return this;
    }

    public void setNivel(Integer nivel) {
        this.nivel = nivel;
    }

    public ResultadoEscalamiento getResultado() {
        return this.resultado;
    }

    public EjecucionDeEscalamiento resultado(ResultadoEscalamiento resultado) {
        this.setResultado(resultado);
        return this;
    }

    public void setResultado(ResultadoEscalamiento resultado) {
        this.resultado = resultado;
    }

    public Instant getEjecutadoEn() {
        return this.ejecutadoEn;
    }

    public EjecucionDeEscalamiento ejecutadoEn(Instant ejecutadoEn) {
        this.setEjecutadoEn(ejecutadoEn);
        return this;
    }

    public void setEjecutadoEn(Instant ejecutadoEn) {
        this.ejecutadoEn = ejecutadoEn;
    }

    public Instant getProximoEn() {
        return this.proximoEn;
    }

    public EjecucionDeEscalamiento proximoEn(Instant proximoEn) {
        this.setProximoEn(proximoEn);
        return this;
    }

    public void setProximoEn(Instant proximoEn) {
        this.proximoEn = proximoEn;
    }

    public String getDetalle() {
        return this.detalle;
    }

    public EjecucionDeEscalamiento detalle(String detalle) {
        this.setDetalle(detalle);
        return this;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Incidente getIncidente() {
        return this.incidente;
    }

    public void setIncidente(Incidente incidente) {
        this.incidente = incidente;
    }

    public EjecucionDeEscalamiento incidente(Incidente incidente) {
        this.setIncidente(incidente);
        return this;
    }

    public PasoEscalamiento getPaso() {
        return this.paso;
    }

    public void setPaso(PasoEscalamiento pasoEscalamiento) {
        this.paso = pasoEscalamiento;
    }

    public EjecucionDeEscalamiento paso(PasoEscalamiento pasoEscalamiento) {
        this.setPaso(pasoEscalamiento);
        return this;
    }

    public User getDestinatario() {
        return this.destinatario;
    }

    public void setDestinatario(User user) {
        this.destinatario = user;
    }

    public EjecucionDeEscalamiento destinatario(User user) {
        this.setDestinatario(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EjecucionDeEscalamiento)) {
            return false;
        }
        return getId() != null && getId().equals(((EjecucionDeEscalamiento) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EjecucionDeEscalamiento{" +
            "id=" + getId() +
            ", vuelta=" + getVuelta() +
            ", nivel=" + getNivel() +
            ", resultado='" + getResultado() + "'" +
            ", ejecutadoEn='" + getEjecutadoEn() + "'" +
            ", proximoEn='" + getProximoEn() + "'" +
            ", detalle='" + getDetalle() + "'" +
            "}";
    }
}
