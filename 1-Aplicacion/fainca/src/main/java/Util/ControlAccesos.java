package Util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frena los ataques de fuerza bruta contra el login.
 *
 * Sin esto, alguien puede probar miles de contrasenas por minuto contra la cuenta
 * de Administrador hasta acertar. Con esto, tras {@value #MAX_INTENTOS} fallos
 * seguidos la cuenta queda bloqueada {@value #MINUTOS_BLOQUEO} minutos: un ataque
 * automatizado pasa de minutos a anios, mientras que una persona que simplemente
 * se equivoco solo espera un rato.
 *
 * El conteo se lleva en memoria (se reinicia si se reinicia el servidor). Para una
 * bodega en red local es suficiente y evita depender de la base de datos justo
 * cuando el atacante la esta golpeando.
 *
 * El bloqueo es por NOMBRE DE USUARIO, no por IP: en la bodega varias personas
 * comparten la misma red, y bloquear por IP dejaria fuera a todos.
 */
public final class ControlAccesos {

    /** Fallos seguidos permitidos antes de bloquear. */
    public static final int MAX_INTENTOS = 5;

    /** Cuanto dura el bloqueo una vez alcanzado el limite. */
    public static final int MINUTOS_BLOQUEO = 5;

    /** Si no hay actividad en este tiempo, el registro se olvida (limpieza de memoria). */
    private static final Duration CADUCIDAD = Duration.ofHours(2);

    private static final Map<String, Registro> REGISTROS = new ConcurrentHashMap<>();

    private ControlAccesos() {
    }

    private static final class Registro {
        int fallos;
        Instant ultimoFallo;
        Instant bloqueadoHasta;
    }

    private static String clave(String usuario) {
        return usuario == null ? "" : usuario.trim().toLowerCase();
    }

    /** Minutos que faltan para poder reintentar; 0 si no esta bloqueado. */
    public static long minutosRestantes(String usuario) {
        Registro r = REGISTROS.get(clave(usuario));
        if (r == null || r.bloqueadoHasta == null) return 0;
        long segundos = Duration.between(Instant.now(), r.bloqueadoHasta).getSeconds();
        if (segundos <= 0) return 0;
        // Redondeo hacia arriba real: 300s -> 5 min (no 6), y 30s -> 1 min (no 0).
        return (segundos + 59) / 60;
    }

    public static boolean estaBloqueado(String usuario) {
        return minutosRestantes(usuario) > 0;
    }

    /** Anota un intento fallido y bloquea la cuenta si se alcanzo el limite. */
    public static void registrarFallo(String usuario) {
        limpiarViejos();
        REGISTROS.compute(clave(usuario), (k, r) -> {
            if (r == null) r = new Registro();
            r.fallos++;
            r.ultimoFallo = Instant.now();
            if (r.fallos >= MAX_INTENTOS) {
                r.bloqueadoHasta = Instant.now().plus(Duration.ofMinutes(MINUTOS_BLOQUEO));
                r.fallos = 0; // tras cumplir el bloqueo vuelve a tener sus intentos
            }
            return r;
        });
    }

    /** Entro bien: se borra el historial de fallos de esa cuenta. */
    public static void registrarExito(String usuario) {
        REGISTROS.remove(clave(usuario));
    }

    /** Intentos que le quedan antes del bloqueo (para avisar al usuario). */
    public static int intentosRestantes(String usuario) {
        Registro r = REGISTROS.get(clave(usuario));
        if (r == null) return MAX_INTENTOS;
        return Math.max(0, MAX_INTENTOS - r.fallos);
    }

    /** Evita que el mapa crezca sin limite con usuarios que ya no reintentan. */
    private static void limpiarViejos() {
        Instant corte = Instant.now().minus(CADUCIDAD);
        REGISTROS.entrySet().removeIf(e -> {
            Registro r = e.getValue();
            boolean sinBloqueoVigente = r.bloqueadoHasta == null || r.bloqueadoHasta.isBefore(Instant.now());
            return sinBloqueoVigente && r.ultimoFallo != null && r.ultimoFallo.isBefore(corte);
        });
    }
}
