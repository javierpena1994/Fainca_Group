package Filtros;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Controla el cache de los archivos estaticos.
 *
 * Sin esto, Jetty solo manda Last-Modified y el navegador aplica su propio criterio:
 * puede quedarse con un .js viejo aunque el archivo ya haya cambiado en el servidor.
 * Eso obligaba a ir renombrando a mano "app.js?v=6", "?v=7"... y si uno se olvidaba de
 * subir ese numero, las PCs seguian ejecutando codigo viejo (y aparecian "bugs"
 * que en el servidor no existian).
 *
 * Solucion: nuestro propio JS/CSS se marca "no-cache", que NO significa "no guardar"
 * sino "preguntar siempre si cambio". El navegador guarda el archivo y en cada visita
 * pregunta con If-Modified-Since; si no cambio, Jetty responde 304 (sin cuerpo, unos
 * pocos bytes). En una LAN eso es instantaneo y garantiza que todos los equipos corran
 * siempre la ultima version.
 *
 * Las librerias de terceros (/vendor/) si se cachean por mucho tiempo: no cambian nunca
 * y son las mas pesadas, asi las PCs de ventas no las vuelven a descargar.
 */
@WebFilter(urlPatterns = { "/js/*", "/css/*", "/images/*", "/vendor/*" })
public class CacheFilter implements Filter {

    private static final String UN_ANIO = "public, max-age=31536000, immutable";
    private static final String REVALIDAR = "no-cache";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String ruta = req.getServletPath();

        if (ruta.startsWith("/vendor/")) {
            res.setHeader("Cache-Control", UN_ANIO);
        } else {
            res.setHeader("Cache-Control", REVALIDAR);
        }

        chain.doFilter(request, response);
    }
}
