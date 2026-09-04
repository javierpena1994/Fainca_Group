package Util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Comprime las fotos de referencia de los productos antes de guardarlas en disco,
 * para que la carpeta compartida por LAN se mantenga liviana y las paginas carguen rapido.
 */
public class ImagenUtil {

    private static final int LADO_MAXIMO = 800;
    private static final float CALIDAD_JPEG = 0.8f;
    private static final String CARPETA_POR_DEFECTO = "C:/fainca-inventario/imagenes";

    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream in = ImagenUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) CONFIG.load(in);
        } catch (IOException e) {
            // Si falla, se usa la carpeta por defecto.
        }
    }

    public static String carpetaConfigurada() {
        String configurada = CONFIG.getProperty("app.imagenes.carpeta");
        if (configurada != null && !configurada.isBlank()) {
            File f = new File(configurada.trim());
            if (f.exists() && f.isDirectory()) {
                return f.getAbsolutePath();
            }
        }

        // Si la ruta configurada no existe (por ejemplo, si se movio la carpeta del proyecto),
        // buscamos automaticamente '3-Imagenes-de-productos' en ubicaciones relativas.
        String[] candidatos = {
            "../../3-Imagenes-de-productos",
            "../3-Imagenes-de-productos",
            "3-Imagenes-de-productos",
            "../imagenes",
            "imagenes"
        };
        for (String c : candidatos) {
            File dir = new File(c);
            if (dir.exists() && dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        }

        try {
            File base = new File(ImagenUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            while (base != null && base.exists()) {
                File dir = new File(base, "3-Imagenes-de-productos");
                if (dir.exists() && dir.isDirectory()) {
                    return dir.getAbsolutePath();
                }
                base = base.getParentFile();
            }
        } catch (Exception ignored) {
        }

        return configurada != null && !configurada.isBlank() ? configurada : CARPETA_POR_DEFECTO;
    }

    /** Nombre de archivo que le corresponde a un codigo de producto (sin tocar disco). */
    public static String nombreArchivo(String codigo) {
        return codigo.replaceAll("[^A-Za-z0-9_-]", "_") + ".jpg";
    }

    /** Redimensiona (max 800px de lado), comprime a JPEG ~80% y guarda como "<codigo>.jpg". Devuelve el nombre de archivo. */
    public static String comprimirYGuardar(InputStream entrada, String codigo, String carpetaDestino) throws IOException {
        BufferedImage original = ImageIO.read(entrada);
        if (original == null) {
            throw new IOException("El archivo no es una imagen valida");
        }
        BufferedImage redimensionada = escalar(original, LADO_MAXIMO);

        File carpeta = new File(carpetaDestino);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de imagenes: " + carpetaDestino);
        }
        String nombreArchivo = nombreArchivo(codigo);
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(new File(carpeta, nombreArchivo))) {
            escribirJpeg(redimensionada, salida);
        }
        return nombreArchivo;
    }

    /**
     * Miniatura CUADRADA (lado x lado) de una foto: la imagen se escala conservando su
     * proporcion y se centra sobre un lienzo blanco cuadrado. Asi, en el reporte todas
     * las fotos ocupan el mismo cuadro sin deformarse. Devuelve null si no se puede leer.
     */
    public static byte[] miniaturaCuadrada(File origen, int lado) {
        if (origen == null || !origen.exists()) return null;
        try {
            BufferedImage original = ImageIO.read(origen);
            if (original == null) return null;

            double escala = Math.min((double) lado / original.getWidth(), (double) lado / original.getHeight());
            int w = Math.max(1, (int) Math.round(original.getWidth() * escala));
            int h = Math.max(1, (int) Math.round(original.getHeight() * escala));

            BufferedImage lienzo = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = lienzo.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, lado, lado);
            g.drawImage(original.getScaledInstance(w, h, Image.SCALE_SMOOTH), (lado - w) / 2, (lado - h) / 2, null);
            g.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ImageOutputStream salida = ImageIO.createImageOutputStream(bos)) {
                escribirJpeg(lienzo, salida);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** Redimensiona proporcionalmente a un lado maximo, sobre fondo blanco, en formato RGB. */
    private static BufferedImage escalar(BufferedImage original, int ladoMax) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        double escala = Math.min(1.0, (double) ladoMax / Math.max(ancho, alto));
        int anchoFinal = Math.max(1, (int) Math.round(ancho * escala));
        int altoFinal = Math.max(1, (int) Math.round(alto * escala));

        BufferedImage salida = new BufferedImage(anchoFinal, altoFinal, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = salida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE); // fondo blanco por si el original tenia transparencia
        g.fillRect(0, 0, anchoFinal, altoFinal);
        g.drawImage(original.getScaledInstance(anchoFinal, altoFinal, Image.SCALE_SMOOTH), 0, 0, null);
        g.dispose();
        return salida;
    }

    private static void escribirJpeg(BufferedImage imagen, ImageOutputStream salida) throws IOException {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpg");
        if (!escritores.hasNext()) throw new IOException("No hay codificador JPEG disponible en esta JVM");
        ImageWriter escritor = escritores.next();
        try {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(CALIDAD_JPEG);
            escritor.setOutput(salida);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
    }
}
