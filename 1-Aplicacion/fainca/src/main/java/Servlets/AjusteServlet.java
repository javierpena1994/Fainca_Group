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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ajuste de inventario por reconteo fisico: recibe la cantidad REAL contada en bodega
 * y el sistema calcula y registra la diferencia como movimiento tipo 'ajuste'.
 *
 * Admite VARIOS productos en una sola peticion (un reconteo suele abarcar toda una
 * percha): comparten un mismo lote y en el historial aparecen como un solo documento.
 * Igual que en ingresos y salidas, si una linea falla las demas se registran.
 *
 * Solo admin/superadmin (el filtro ya lo garantiza).
 */
@WebServlet("/AjusteServlet")
public class AjusteServlet extends BaseServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        JsonObject json = leerJson(request);
        String observaciones = texto(json, "observaciones");

        if (observaciones == null || observaciones.isBlank()) {
            responderError(response, 400, "La observacion es obligatoria en un ajuste (explique el motivo)");
            return;
        }

        // Lineas del reconteo; se acepta tambien el formato antiguo de un solo producto.
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

        String lote = UUID.randomUUID().toString();
        MovimientoDAO dao = new MovimientoDAO();

        List<Map<String, Object>> resultados = new ArrayList<>();
        int registradas = 0;

        for (JsonObject linea : lineas) {
            String codigo = texto(linea, "producto_codigo");
            int cantidadReal = entero(linea, "cantidad_real", -1);
            // null = no tocar la ubicacion; solo se envia cuando la pantalla cargo el
            // producto, para no borrarla por accidente si no viene informada.
            String ubicacion = texto(linea, "ubicacion");

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("codigo", codigo == null ? "" : codigo);

            if (codigo == null || codigo.isBlank() || cantidadReal < 0) {
                r.put("ok", false);
                r.put("error", "Código y cantidad real (0 o más) son obligatorios");
                resultados.add(r);
                continue;
            }
            try {
                // La fecha va siempre en null: un reconteo fisico se registra con la fecha
                // y hora del momento, sin excepcion. Aunque la peticion traiga una "fecha",
                // se ignora -- la regla vive aqui y no solo en la pantalla.
                int[] x = dao.ajustar(codigo, cantidadReal, ubicacion, observaciones,
                        null, usuarioActual(request).getId(), lote);
                r.put("ok", true);
                r.put("stock_anterior", x[0]);
                r.put("diferencia", x[1]);
                r.put("stock_actual", x[2]);
                registradas++;
            } catch (IllegalArgumentException e) {
                r.put("ok", false);
                r.put("error", e.getMessage());
            }
            resultados.add(r);
        }

        if (registradas == 0) {
            responderError(response, 400, resultados.get(0).get("error") == null
                    ? "No se pudo ajustar ningún producto"
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
}
