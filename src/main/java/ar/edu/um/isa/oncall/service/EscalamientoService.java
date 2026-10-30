package ar.edu.um.isa.oncall.service;

import ar.edu.um.isa.oncall.domain.EjecucionDeEscalamiento;
import ar.edu.um.isa.oncall.domain.EventoDeIncidente;
import ar.edu.um.isa.oncall.domain.Incidente;
import ar.edu.um.isa.oncall.domain.Notificacion;
import ar.edu.um.isa.oncall.domain.PasoEscalamiento;
import ar.edu.um.isa.oncall.domain.PoliticaEscalamiento;
import ar.edu.um.isa.oncall.domain.Servicio;
import ar.edu.um.isa.oncall.domain.User;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoIncidente;
import ar.edu.um.isa.oncall.domain.enumeration.EstadoNotificacion;
import ar.edu.um.isa.oncall.domain.enumeration.ResultadoEscalamiento;
import ar.edu.um.isa.oncall.domain.enumeration.TipoEvento;
import ar.edu.um.isa.oncall.repository.EjecucionDeEscalamientoRepository;
import ar.edu.um.isa.oncall.repository.EscalamientoRepository;
import ar.edu.um.isa.oncall.repository.EventoDeIncidenteRepository;
import ar.edu.um.isa.oncall.repository.NotificacionRepository;
import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO;
import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO.Escalon;
import ar.edu.um.isa.oncall.service.dto.EstadoDeEscalamientoDTO.Estado;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regla de negocio D · Ejecucion de la cadena de escalamiento.
 *
 * <p>Un incidente abierto y sin reconocer recorre la {@code PoliticaEscalamiento} de su
 * servicio, paso por paso, en orden. Cada paso avisa a alguien (a quien este de guardia en su
 * rotacion, segun la regla C, o a un destinatario directo) y despues espera
 * {@code esperaMinutos}: si en ese tiempo nadie reconoce, se ejecuta el paso siguiente. Al
 * terminar los pasos se vuelve a empezar, {@code repetirVeces} veces mas. Cuando eso tambien
 * se termina, el escalamiento queda {@code AGOTADO}: se deja asentado y no se notifica mas.</p>
 *
 * <p>La espera se cuenta desde que el paso <em>efectivamente se ejecuto</em>, no desde cuando
 * le tocaba. Si el proceso estuvo caido media hora, al volver ejecuta un paso y espera; no
 * dispara de golpe todos los que se acumularon.</p>
 *
 * <p>Cortes: el incidente resuelto o cerrado, reconocido (o mitigado), silenciado (es una
 * pausa: al vencer el silencio la cadena sigue donde estaba) y el escalamiento agotado.</p>
 *
 * <p>Estado e idempotencia: cada escalon ejecutado deja una fila en
 * {@code EjecucionDeEscalamiento}, que es a la vez la auditoria y la memoria del proceso. La
 * fila del incidente se bloquea mientras se decide, asi que si el proceso corre dos veces la
 * segunda corrida ve lo que hizo la primera y no vuelve a notificar.</p>
 *
 * <p>Quien despierta al proceso es {@link EscalamientoProgramado}; esta clase no sabe de
 * relojes: recibe el {@code ahora} como parametro, lo que la hace probable sin esperar.</p>
 */
@Service
@Transactional
public class EscalamientoService {

    private static final Logger LOG = LoggerFactory.getLogger(EscalamientoService.class);

    private static final Set<EstadoIncidente> ESTADOS_TERMINADOS = EnumSet.of(EstadoIncidente.RESUELTO, EstadoIncidente.CERRADO);
    private static final Set<EstadoIncidente> ESTADOS_ATENDIDOS = EnumSet.of(EstadoIncidente.RECONOCIDO, EstadoIncidente.MITIGADO);

    /** Resultados que representan un paso de la politica, a diferencia de las marcas de corte. */
    private static final Set<ResultadoEscalamiento> ESCALONES = EnumSet.of(
        ResultadoEscalamiento.NOTIFICADO,
        ResultadoEscalamiento.SIN_DESTINATARIO
    );

    private static final Comparator<PasoEscalamiento> POR_ORDEN = Comparator.comparing(PasoEscalamiento::getOrden).thenComparing(
        PasoEscalamiento::getId,
        Comparator.nullsLast(Comparator.naturalOrder())
    );

    private static final int MAX_DETALLE = 500;
    private static final int MAX_DESTINO = 120;

    private final EscalamientoRepository escalamientoRepository;
    private final EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository;
    private final NotificacionRepository notificacionRepository;
    private final EventoDeIncidenteRepository eventoDeIncidenteRepository;
    private final GuardiaVigenteService guardiaVigenteService;

    public EscalamientoService(
        EscalamientoRepository escalamientoRepository,
        EjecucionDeEscalamientoRepository ejecucionDeEscalamientoRepository,
        NotificacionRepository notificacionRepository,
        EventoDeIncidenteRepository eventoDeIncidenteRepository,
        GuardiaVigenteService guardiaVigenteService
    ) {
        this.escalamientoRepository = escalamientoRepository;
        this.ejecucionDeEscalamientoRepository = ejecucionDeEscalamientoRepository;
        this.notificacionRepository = notificacionRepository;
        this.eventoDeIncidenteRepository = eventoDeIncidenteRepository;
        this.guardiaVigenteService = guardiaVigenteService;
    }

    /**
     * Los incidentes que el proceso tiene que mirar en cada corrida.
     *
     * @return los ids de los incidentes abiertos y sin reconocer.
     */
    @Transactional(readOnly = true)
    public List<Long> incidentesParaEscalar() {
        return escalamientoRepository.buscarIdsSinReconocer(EstadoIncidente.ABIERTO);
    }

    /**
     * Avanza la cadena de un incidente todo lo que corresponda al instante {@code ahora}.
     *
     * @param incidenteId el incidente.
     * @param ahora el instante de la corrida.
     * @return como quedo la cadena, o vacio si el incidente no existe.
     */
    public Optional<EstadoDeEscalamientoDTO> procesar(Long incidenteId, Instant ahora) {
        Optional<Incidente> bloqueado = escalamientoRepository.bloquear(incidenteId);
        if (bloqueado.isEmpty()) {
            return Optional.empty();
        }
        Incidente incidente = bloqueado.orElseThrow();
        List<EjecucionDeEscalamiento> historial = new ArrayList<>(escalamientoRepository.buscarHistorial(incidenteId));
        List<EjecucionDeEscalamiento> nuevas = new ArrayList<>();

        Optional<PoliticaEscalamiento> politica = politicaAplicable(incidente);
        Estado corte = corte(incidente, historial, politica, ahora);

        if (corte == Estado.SIN_POLITICA && historial.stream().noneMatch(e -> e.getResultado() == ResultadoEscalamiento.SIN_POLITICA)) {
            nuevas.add(registrarSinPolitica(incidente, ahora));
        }
        if (corte == null) {
            avanzar(incidente, politica.orElseThrow(), historial, nuevas, ahora);
        }

        historial.addAll(nuevas);
        return Optional.of(aEstado(incidente, politica, historial, nuevas, ahora));
    }

    /**
     * Consulta, sin ejecutar nada, por donde va la cadena.
     *
     * @param incidenteId el incidente.
     * @param ahora el instante de la consulta.
     * @return el estado, o vacio si el incidente no existe.
     */
    @Transactional(readOnly = true)
    public Optional<EstadoDeEscalamientoDTO> consultar(Long incidenteId, Instant ahora) {
        return escalamientoRepository
            .buscar(incidenteId)
            .map(incidente ->
                aEstado(incidente, politicaAplicable(incidente), escalamientoRepository.buscarHistorial(incidenteId), List.of(), ahora)
            );
    }

    /**
     * El bucle de la regla. Ejecuta pasos mientras esten vencidos: un paso sin espera o sin
     * destinatario deja vencido al siguiente en el acto.
     */
    private void avanzar(
        Incidente incidente,
        PoliticaEscalamiento politica,
        List<EjecucionDeEscalamiento> historial,
        List<EjecucionDeEscalamiento> nuevas,
        Instant ahora
    ) {
        List<PasoEscalamiento> pasos = pasosOrdenados(politica);
        int vueltas = politica.getRepetirVeces() + 1;
        // tope defensivo: nunca hay mas escalones que pasos por vueltas, mas la marca de agotado
        int tope = pasos.size() * vueltas + 1;

        for (int i = 0; i < tope; i++) {
            Optional<EjecucionDeEscalamiento> ultimo = ultimoEscalon(historial, nuevas);
            if (ultimo.isPresent() && ultimo.orElseThrow().getProximoEn() != null && ultimo.orElseThrow().getProximoEn().isAfter(ahora)) {
                return;
            }

            Optional<Siguiente> siguiente = siguiente(ultimo, pasos, politica.getRepetirVeces());
            if (siguiente.isEmpty()) {
                nuevas.add(registrarAgotado(incidente, politica, ultimo.orElseThrow(), pasos.size(), ahora));
                return;
            }
            nuevas.add(ejecutarPaso(incidente, siguiente.orElseThrow().vuelta(), siguiente.orElseThrow().paso(), ahora));
        }
        LOG.warn("El escalamiento del incidente {} llego al tope de {} escalones en una corrida", incidente.getId(), tope);
    }

    /** El proximo escalon: a que vuelta y a que paso le toca. */
    record Siguiente(int vuelta, PasoEscalamiento paso) {}

    /**
     * Despues del ultimo escalon, el paso de orden siguiente en la misma vuelta; si no hay, el
     * primero de la vuelta siguiente; si ya no quedan vueltas, nada.
     *
     * Se avanza por {@code orden} y no por posicion: si alguien edita la politica en medio de
     * un incidente, la cadena sigue desde el nivel en el que estaba.
     */
    static Optional<Siguiente> siguiente(Optional<EjecucionDeEscalamiento> ultimo, List<PasoEscalamiento> pasos, int repetirVeces) {
        if (ultimo.isEmpty()) {
            return Optional.of(new Siguiente(0, pasos.get(0)));
        }
        int vuelta = ultimo.orElseThrow().getVuelta();
        int nivel = ultimo.orElseThrow().getNivel();
        Optional<PasoEscalamiento> mismaVuelta = pasos
            .stream()
            .filter(paso -> paso.getOrden() > nivel)
            .findFirst();
        if (mismaVuelta.isPresent()) {
            return Optional.of(new Siguiente(vuelta, mismaVuelta.orElseThrow()));
        }
        if (vuelta < repetirVeces) {
            return Optional.of(new Siguiente(vuelta + 1, pasos.get(0)));
        }
        return Optional.empty();
    }

    /**
     * Por que no se escala, o {@code null} si hay que seguir.
     */
    private Estado corte(
        Incidente incidente,
        List<EjecucionDeEscalamiento> historial,
        Optional<PoliticaEscalamiento> politica,
        Instant ahora
    ) {
        if (ESTADOS_TERMINADOS.contains(incidente.getEstado())) {
            return Estado.RESUELTO;
        }
        if (incidente.getReconocidoEn() != null || ESTADOS_ATENDIDOS.contains(incidente.getEstado())) {
            return Estado.RECONOCIDO;
        }
        if (historial.stream().anyMatch(e -> e.getResultado() == ResultadoEscalamiento.AGOTADO)) {
            return Estado.AGOTADO;
        }
        if (incidente.getSilenciadoHasta() != null && incidente.getSilenciadoHasta().isAfter(ahora)) {
            return Estado.SILENCIADO;
        }
        if (incidente.getDetectadoEn() != null && incidente.getDetectadoEn().isAfter(ahora)) {
            return Estado.PENDIENTE;
        }
        if (politica.isEmpty()) {
            return Estado.SIN_POLITICA;
        }
        return null;
    }

    /**
     * La politica del servicio mas critico afectado por el incidente que tenga al menos un
     * paso. Empates de criticidad: el servicio de menor id. Si el servicio tiene varias
     * politicas: la de menor id.
     */
    Optional<PoliticaEscalamiento> politicaAplicable(Incidente incidente) {
        List<Servicio> servicios = incidente
            .getServicios()
            .stream()
            .sorted(
                Comparator.comparing(Servicio::getCriticidad, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(
                    Servicio::getId
                )
            )
            .toList();
        if (servicios.isEmpty()) {
            return Optional.empty();
        }
        List<PoliticaEscalamiento> politicas = escalamientoRepository.buscarPoliticasDeServicios(
            servicios.stream().map(Servicio::getId).toList()
        );
        for (Servicio servicio : servicios) {
            Optional<PoliticaEscalamiento> politica = politicas
                .stream()
                .filter(p -> p.getServicio() != null && servicio.getId().equals(p.getServicio().getId()))
                .filter(p -> !p.getPasos().isEmpty())
                .min(Comparator.comparing(PoliticaEscalamiento::getId));
            if (politica.isPresent()) {
                return politica;
            }
        }
        return Optional.empty();
    }

    /**
     * Ejecuta un paso: decide a quien avisar, deja la notificacion y anota el escalon.
     */
    private EjecucionDeEscalamiento ejecutarPaso(Incidente incidente, int vuelta, PasoEscalamiento paso, Instant ahora) {
        String escalon = "Escalamiento nivel " + paso.getOrden() + " (vuelta " + (vuelta + 1) + ")";

        User destinatario = null;
        String criterio;
        if (paso.getDestinatarioDirecto() != null) {
            destinatario = paso.getDestinatarioDirecto();
            criterio =
                paso.getRotacion() != null
                    ? "destinatario directo; el paso tambien apunta a una rotacion, pero manda el destinatario directo"
                    : "destinatario directo";
        } else if (paso.getRotacion() != null) {
            GuardiaVigenteService.Resolucion guardia = guardiaVigenteService.resolver(paso.getRotacion(), ahora);
            destinatario = guardia.responsable();
            criterio = "guardia de la rotacion '" + paso.getRotacion().getNombre() + "': " + guardia.guardia().motivo();
        } else {
            criterio = "el paso no apunta ni a una rotacion ni a una persona";
        }
        if (destinatario != null && !destinatario.isActivated()) {
            criterio = criterio + "; el usuario " + destinatario.getLogin() + " no esta activo";
            destinatario = null;
        }

        EjecucionDeEscalamiento ejecucion = new EjecucionDeEscalamiento()
            .vuelta(vuelta)
            .nivel(paso.getOrden())
            .ejecutadoEn(ahora)
            .incidente(incidente)
            .paso(paso);

        String detalle;
        if (destinatario != null) {
            Instant proximoEn = ahora.plus(Duration.ofMinutes(paso.getEsperaMinutos()));
            notificacionRepository.save(
                new Notificacion()
                    .canal(paso.getCanal())
                    .destino(recortar(destinatario.getEmail() != null ? destinatario.getEmail() : destinatario.getLogin(), MAX_DESTINO))
                    .estado(EstadoNotificacion.PENDIENTE)
                    .intentos(0)
                    .incidente(incidente)
                    .destinatario(destinatario)
            );
            detalle =
                escalon +
                ": se notifico a " +
                destinatario.getLogin() +
                " por " +
                paso.getCanal() +
                " (" +
                criterio +
                "). Si nadie reconoce, el proximo escalon sale a las " +
                proximoEn +
                ".";
            ejecucion.resultado(ResultadoEscalamiento.NOTIFICADO).proximoEn(proximoEn).destinatario(destinatario);
        } else {
            detalle = escalon + ": no hay a quien avisar (" + criterio + "). Se pasa al escalon siguiente sin esperar.";
            ejecucion.resultado(ResultadoEscalamiento.SIN_DESTINATARIO).proximoEn(ahora);
        }

        ejecucion.detalle(recortar(detalle, MAX_DETALLE));
        registrarEnLineaDeTiempo(incidente, detalle, ahora);
        LOG.debug("Incidente {}: {}", incidente.getId(), detalle);
        return ejecucionDeEscalamientoRepository.save(ejecucion);
    }

    private EjecucionDeEscalamiento registrarAgotado(
        Incidente incidente,
        PoliticaEscalamiento politica,
        EjecucionDeEscalamiento ultimo,
        int cantidadDePasos,
        Instant ahora
    ) {
        String detalle =
            "Escalamiento agotado: se recorrieron los " +
            cantidadDePasos +
            " pasos de la politica '" +
            politica.getNombre() +
            "' en " +
            (politica.getRepetirVeces() + 1) +
            " vuelta(s) y nadie reconocio el incidente. No se notifica mas: queda en manos de una persona.";
        registrarEnLineaDeTiempo(incidente, detalle, ahora);
        LOG.info("Incidente {}: escalamiento agotado", incidente.getId());
        return ejecucionDeEscalamientoRepository.save(
            new EjecucionDeEscalamiento()
                .vuelta(ultimo.getVuelta())
                .nivel(ultimo.getNivel())
                .resultado(ResultadoEscalamiento.AGOTADO)
                .ejecutadoEn(ahora)
                .detalle(recortar(detalle, MAX_DETALLE))
                .incidente(incidente)
        );
    }

    private EjecucionDeEscalamiento registrarSinPolitica(Incidente incidente, Instant ahora) {
        String detalle =
            "No se puede escalar: ninguno de los servicios del incidente tiene una politica de escalamiento con pasos. " +
            "Si se carga una, la cadena arranca en la proxima corrida.";
        registrarEnLineaDeTiempo(incidente, detalle, ahora);
        return ejecucionDeEscalamientoRepository.save(
            new EjecucionDeEscalamiento()
                .vuelta(0)
                .nivel(0)
                .resultado(ResultadoEscalamiento.SIN_POLITICA)
                .ejecutadoEn(ahora)
                .detalle(recortar(detalle, MAX_DETALLE))
                .incidente(incidente)
        );
    }

    private EstadoDeEscalamientoDTO aEstado(
        Incidente incidente,
        Optional<PoliticaEscalamiento> politica,
        List<EjecucionDeEscalamiento> historial,
        List<EjecucionDeEscalamiento> nuevas,
        Instant ahora
    ) {
        Estado corte = corte(incidente, historial, politica, ahora);
        Optional<EjecucionDeEscalamiento> ultimo = ultimoEscalon(historial, List.of());

        Instant proximo = null;
        if (corte == null) {
            proximo = ultimo.map(EjecucionDeEscalamiento::getProximoEn).orElse(ahora);
        } else if (corte == Estado.SILENCIADO) {
            proximo = incidente.getSilenciadoHasta();
        } else if (corte == Estado.PENDIENTE) {
            proximo = incidente.getDetectadoEn();
        }

        return new EstadoDeEscalamientoDTO(
            incidente.getId(),
            corte == null ? Estado.EN_CURSO : corte,
            politica.map(PoliticaEscalamiento::getId).orElse(null),
            politica.map(PoliticaEscalamiento::getNombre).orElse(null),
            ultimo.map(EjecucionDeEscalamiento::getVuelta).orElse(null),
            ultimo.map(EjecucionDeEscalamiento::getNivel).orElse(null),
            proximo,
            nuevas.stream().map(EscalamientoService::aEscalon).toList(),
            historial.stream().map(EscalamientoService::aEscalon).toList()
        );
    }

    private static Optional<EjecucionDeEscalamiento> ultimoEscalon(
        List<EjecucionDeEscalamiento> historial,
        List<EjecucionDeEscalamiento> nuevas
    ) {
        Optional<EjecucionDeEscalamiento> ultimaNueva = ultimoDe(nuevas);
        return ultimaNueva.isPresent() ? ultimaNueva : ultimoDe(historial);
    }

    private static Optional<EjecucionDeEscalamiento> ultimoDe(List<EjecucionDeEscalamiento> ejecuciones) {
        for (int i = ejecuciones.size() - 1; i >= 0; i--) {
            if (ESCALONES.contains(ejecuciones.get(i).getResultado())) {
                return Optional.of(ejecuciones.get(i));
            }
        }
        return Optional.empty();
    }

    private static List<PasoEscalamiento> pasosOrdenados(PoliticaEscalamiento politica) {
        return politica.getPasos().stream().sorted(POR_ORDEN).toList();
    }

    private static Escalon aEscalon(EjecucionDeEscalamiento ejecucion) {
        return new Escalon(
            ejecucion.getId(),
            ejecucion.getVuelta(),
            ejecucion.getNivel(),
            ejecucion.getResultado(),
            ejecucion.getEjecutadoEn(),
            ejecucion.getProximoEn(),
            ejecucion.getDestinatario() != null ? ejecucion.getDestinatario().getLogin() : null,
            ejecucion.getDetalle()
        );
    }

    private void registrarEnLineaDeTiempo(Incidente incidente, String detalle, Instant ocurridoEn) {
        eventoDeIncidenteRepository.save(
            new EventoDeIncidente()
                .tipo(TipoEvento.ESCALAMIENTO)
                .detalle(recortar(detalle, MAX_DETALLE))
                .ocurridoEn(ocurridoEn)
                .automatico(true)
                .incidente(incidente)
        );
    }

    private static String recortar(String texto, int largoMaximo) {
        if (texto == null || texto.length() <= largoMaximo) {
            return texto;
        }
        return texto.substring(0, largoMaximo - 3) + "...";
    }
}
