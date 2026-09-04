package Servlets;

import Dao.MovimientoDAO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registra un ingreso o egreso de stock.
 *
 * Acepta VARIOS productos en una sola peticion: todas las lineas comparten un mismo
 * "lote", de modo que en el historial aparecen como UN documento con su lista de items
 * (antes cada producto generaba un registro suelto con la observacion repetida).
 *
 * Se conserva el comportamiento de exito parcial: si una linea falla (stock insuficiente,
 * codigo inexistente), las demas se registran igual y se devuelve el detalle de cada una.
 *
 * La fecha es opcional (sin ella queda la fecha y hora actual); si se indica, solo se
 * acepta hasta 2 dias hacia atras y nunca a futuro. Los ajustes NO pasan por aqui:
 * tienen su propio servlet con su propia regla de fecha (siempre la actual).
 */
@WebServlet("/MovimientoServlet")
public class MovimientoServlet extends BaseServlet {

    // Cuantos dias hacia atras se permite fechar un ingreso/salida.
    private static final int DIAS_MAXIMO_ATRAS = 2;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String tipo = texto(json, "tipo");
        String observaciones = texto(json, "observaciones");
        String fecha = texto(json, "fecha");

        if (!List.of("ingreso", "egreso").contains(tipo)) {
            responderError(response, 400, "Tipo de movimiento invalido");
            return;
        }

        // Lineas del documento. Se admite tambien el formato antiguo de un solo
        // producto suelto, para no romper integraciones ya existentes.
        List<JsonObject> lineas = new ArrayList<>();
        if (json.has("productos") && json.get("productos").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("productos")) {
                if (e.isJsonObject()) lineas.add(e.getAsJsonObject());
            }
        } else if (json.has("producto_codigo")) {
            lineas.add(json);
        }
        if (lineas.isEmpty()) {
            responderError(response, 400, "No se recibio ningun producto");
            return;
        }

        String errorFecha = validarFecha(fecha);
        if (errorFecha != null) {
            responderError(response, 400, errorFecha);
            return;
        }
        String fechaFinal = fecha == null ? null : fecha.replace('T', ' ');

        // Un unico lote para todo el envio: es lo que agrupa las lineas en el historial.
        String lote = UUID.randomUUID().toString();
        MovimientoDAO dao = new MovimientoDAO();

        List<Map<String, Object>> resultados = new ArrayList<>();
        int registradas = 0;

        for (JsonObject linea : lineas) {
            String codigo = texto(linea, "producto_codigo");
            int cantidad = entero(linea, "cantidad", 0);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("codigo", codigo == null ? "" : codigo);

            if (codigo == null || codigo.isBlank() || cantidad <= 0) {
                r.put("ok", false);
                r.put("error", "Código y cantidad mayor a cero son obligatorios");
                resultados.add(r);
                continue;
            }
            try {
                int stock = dao.registrar(codigo, tipo, cantidad, observaciones,
                        fechaFinal, usuarioActual(request).getId(), lote);
                r.put("ok", true);
                r.put("cantidad", cantidad);
                r.put("stock_actual", stock);
                registradas++;
            } catch (IllegalArgumentException e) {
                r.put("ok", false);
                r.put("error", e.getMessage());
            }
            resultados.add(r);
        }

        // Si no se registro ni una sola linea, no hay documento que mostrar.
        if (registradas == 0) {
            responderError(response, 400, resultados.get(0).get("error") == null
                    ? "No se pudo registrar ningún producto"
                    : String.valueOf(resultados.get(0).get("error")));
            return;
        }

        response.setStatus(201);
        responderJson(response, Map.of(
                "lote", lote,
                "registradas", registradas,
                "total", lineas.size(),
                "resultados", resultados));
    }

    /** Devuelve el mensaje de error si la fecha no es valida, o null si esta bien. */
    private String validarFecha(String fecha) {
        if (fecha == null) return null;
        if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2})?)?")) {
            return "Fecha invalida";
        }
        LocalDate dia;
        try {
            dia = LocalDate.parse(fecha.substring(0, 10));
        } catch (DateTimeParseException e) {
            return "Fecha invalida";
        }
        LocalDate hoy = LocalDate.now();
        if (dia.isBefore(hoy.minusDays(DIAS_MAXIMO_ATRAS))) {
            return "La fecha solo puede modificarse hasta " + DIAS_MAXIMO_ATRAS
                    + " días atrás (desde el " + hoy.minusDays(DIAS_MAXIMO_ATRAS) + ")";
        }
        if (dia.isAfter(hoy)) return "La fecha no puede ser futura";
        return null;
    }
}
