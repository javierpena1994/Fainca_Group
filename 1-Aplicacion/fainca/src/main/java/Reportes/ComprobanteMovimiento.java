package Reportes;

import Objetos.Movimiento;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Comprobante en PDF de UN movimiento de bodega (ingreso, salida o ajuste) con todos
 * los productos que lo componen.
 *
 * El formato es el mismo en todas las exportaciones: logo, tipo de movimiento y numero
 * de documento, los datos de cabecera (fecha, responsable, observacion) y la lista de
 * productos numerada por item. La foto de cada producto es opcional y por defecto
 * NO se incluye, para que el archivo sea liviano.
 */
public final class ComprobanteMovimiento {

    private static final Color AMARILLO = new Color(0xF1, 0xC4, 0x0F);
    private static final Color GRIS = new Color(0x44, 0x44, 0x44);
    private static final Color GRIS_CLARO = new Color(0xF4, 0xF4, 0xF4);
    private static final Color BORDE = new Color(0xDD, 0xDD, 0xDD);
    private static final Color VERDE = new Color(0x1E, 0x7E, 0x34);
    private static final Color ROJO = new Color(0xC8, 0x23, 0x33);

    private ComprobanteMovimiento() {
    }

    /**
     * @param lineas     productos del movimiento (todas comparten fecha, usuario y observacion)
     * @param numero     numero de documento visible (ej. "ING-000042")
     * @param miniaturas fotos por codigo de producto, o null para no incluirlas
     */
    public static void generar(OutputStream out, List<Movimiento> lineas, String numero,
                               byte[] logoPng, LocalDateTime generado, String generadoPor,
                               Map<String, byte[]> miniaturas) throws Exception {

        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("El movimiento no tiene productos");
        }
        boolean conFoto = miniaturas != null;
        Movimiento cab = lineas.get(0);

        Document doc = new Document(PageSize.A4, 40, 40, 36, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ---------- Encabezado: logo a la izquierda, datos del documento a la derecha ----------
        PdfPTable cabecera = new PdfPTable(2);
        cabecera.setWidthPercentage(100);
        cabecera.setWidths(new float[]{1.4f, 2.0f});

        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setBorder(0);
        celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (logoPng != null && logoPng.length > 0) {
            Image logo = Image.getInstance(logoPng);
            logo.scaleToFit(170, 62);
            celdaLogo.setImage(logo);
        }
        cabecera.addCell(celdaLogo);

        PdfPCell celdaTitulo = new PdfPCell();
        celdaTitulo.setBorder(0);
        celdaTitulo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph tit = new Paragraph("COMPROBANTE DE " + etiquetaTipo(cab.getTipo()), fuente(16, true, GRIS));
        tit.setAlignment(Element.ALIGN_RIGHT);
        Paragraph num = new Paragraph("Documento N.° " + numero, fuente(11, false, GRIS));
        num.setAlignment(Element.ALIGN_RIGHT);
        num.setSpacingBefore(3);
        Paragraph bod = new Paragraph("Inventario — Bodega #1", fuente(9, false, new Color(0x88, 0x88, 0x88)));
        bod.setAlignment(Element.ALIGN_RIGHT);
        celdaTitulo.addElement(tit);
        celdaTitulo.addElement(num);
        celdaTitulo.addElement(bod);
        cabecera.addCell(celdaTitulo);
        doc.add(cabecera);

        // Franja de color segun el tipo de movimiento
        PdfPTable franja = new PdfPTable(1);
        franja.setWidthPercentage(100);
        franja.setSpacingBefore(10);
        PdfPCell fc = new PdfPCell(new Phrase(etiquetaTipo(cab.getTipo()), fuente(13, true, GRIS)));
        fc.setBackgroundColor(AMARILLO);
        fc.setHorizontalAlignment(Element.ALIGN_CENTER);
        fc.setPadding(7);
        fc.setBorder(0);
        franja.addCell(fc);
        doc.add(franja);

        // ---------- Datos de la operacion ----------
        PdfPTable datos = new PdfPTable(2);
        datos.setWidthPercentage(100);
        datos.setWidths(new float[]{1f, 3f});
        datos.setSpacingBefore(12);
        agregarDato(datos, "Fecha y hora", formatearFecha(cab.getFecha()));
        agregarDato(datos, "Responsable", cab.getUsuario());
        agregarDato(datos, "Observación",
                cab.getObservaciones() == null || cab.getObservaciones().isBlank()
                        ? "(sin observación)" : cab.getObservaciones());
        doc.add(datos);

        // ---------- Detalle de productos ----------
        int columnas = conFoto ? 7 : 6;
        PdfPTable tabla = new PdfPTable(columnas);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(14);
        tabla.setWidths(conFoto
                ? new float[]{0.5f, 1.0f, 1.7f, 1.3f, 2.6f, 1.0f, 1.1f}
                : new float[]{0.5f, 1.7f, 1.3f, 2.9f, 1.0f, 1.1f});
        tabla.setHeaderRows(1);

        encabezado(tabla, "ÍTEM");
        if (conFoto) encabezado(tabla, "FOTO");
        encabezado(tabla, "CÓDIGO");
        encabezado(tabla, "MARCA");
        encabezado(tabla, "DESCRIPCIÓN");
        encabezado(tabla, "CANT.");
        encabezado(tabla, "STOCK");

        long totalUnidades = 0;
        for (int i = 0; i < lineas.size(); i++) {
            Movimiento l = lineas.get(i);
            Color fondo = i % 2 == 1 ? GRIS_CLARO : Color.WHITE;

            tabla.addCell(celda(String.valueOf(i + 1), Element.ALIGN_CENTER, fondo, true));
            if (conFoto) tabla.addCell(celdaFoto(miniaturas.get(l.getProductoCodigo()), fondo));
            tabla.addCell(celda(l.getProductoCodigo(), Element.ALIGN_LEFT, fondo, true));
            tabla.addCell(celda(l.getMarca(), Element.ALIGN_LEFT, fondo, false));
            tabla.addCell(celda(resumir(l.getDescripcion()), Element.ALIGN_LEFT, fondo, false));
            tabla.addCell(celdaCantidad(l, fondo));
            tabla.addCell(celda(String.valueOf(l.getStockResultante()), Element.ALIGN_CENTER, fondo, true));

            totalUnidades += Math.abs(l.getCantidad());
        }
        doc.add(tabla);

        // ---------- Totales ----------
        PdfPTable totales = new PdfPTable(1);
        totales.setWidthPercentage(100);
        totales.setSpacingBefore(10);
        PdfPCell tc = new PdfPCell(new Phrase(
                "TOTAL:  " + lineas.size() + " ítem(s)        " + totalUnidades + " unidad(es)",
                fuente(11, true, GRIS)));
        tc.setBackgroundColor(GRIS_CLARO);
        tc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tc.setPadding(7);
        tc.setBorderColor(BORDE);
        totales.addCell(tc);
        doc.add(totales);

        // ---------- Pie ----------
        Paragraph pie = new Paragraph(
                "Documento generado por " + generadoPor + " el "
                        + generado.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"))
                        + "  ·  Sistema de Inventario FAINCA",
                fuente(8, false, new Color(0x99, 0x99, 0x99)));
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(16);
        doc.add(pie);

        doc.close();
    }

    /** Cantidad con signo y color: verde si suma, rojo si resta. */
    private static PdfPCell celdaCantidad(Movimiento l, Color fondo) {
        int c = l.getCantidad();
        String texto;
        Color color;
        if ("egreso".equals(l.getTipo())) {
            texto = "-" + c;
            color = ROJO;
        } else if ("ajuste".equals(l.getTipo())) {
            texto = c > 0 ? "+" + c : String.valueOf(c);
            color = c < 0 ? ROJO : VERDE;
        } else {
            texto = "+" + c;
            color = VERDE;
        }
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente(10, true, color)));
        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(5);
        celda.setBorderColor(BORDE);
        return celda;
    }

    private static String etiquetaTipo(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "ingreso" -> "INGRESO DE INVENTARIO";
            case "egreso" -> "SALIDA DE INVENTARIO";
            case "ajuste" -> "AJUSTE POR RECONTEO FÍSICO";
            default -> "MOVIMIENTO";
        };
    }

    /** La descripcion puede traer parrafos enteros; en el comprobante se acorta. */
    private static String resumir(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String limpio = texto.replaceAll("\\s+", " ").trim();
        return limpio.length() <= 90 ? limpio : limpio.substring(0, 87) + "...";
    }

    private static String formatearFecha(String sql) {
        if (sql == null || sql.length() < 16) return sql == null ? "" : sql;
        String f = sql.substring(0, 10);
        String h = sql.substring(11, 16);
        return f.substring(8, 10) + "/" + f.substring(5, 7) + "/" + f.substring(0, 4) + "  " + h;
    }

    private static void agregarDato(PdfPTable t, String etiqueta, String valor) {
        PdfPCell e = new PdfPCell(new Phrase(etiqueta.toUpperCase(), fuente(9, true, new Color(0x88, 0x88, 0x88))));
        e.setBorder(0);
        e.setPaddingBottom(5);
        t.addCell(e);
        PdfPCell v = new PdfPCell(new Phrase(valor == null ? "" : valor, fuente(10, false, GRIS)));
        v.setBorder(0);
        v.setPaddingBottom(5);
        t.addCell(v);
    }

    private static void encabezado(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente(9, true, Color.WHITE)));
        c.setBackgroundColor(GRIS);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        c.setBorderColor(GRIS);
        t.addCell(c);
    }

    private static PdfPCell celda(String texto, int alineacion, Color fondo, boolean negrita) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, fuente(9.5f, negrita, GRIS)));
        c.setBackgroundColor(fondo);
        c.setHorizontalAlignment(alineacion);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5);
        c.setBorderColor(BORDE);
        return c;
    }

    private static PdfPCell celdaFoto(byte[] jpg, Color fondo) throws Exception {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(fondo);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3);
        c.setBorderColor(BORDE);
        c.setFixedHeight(52);
        if (jpg != null) {
            Image img = Image.getInstance(jpg);
            img.scaleToFit(44, 44);
            c.setImage(img);
        }
        return c;
    }

    private static Font fuente(float puntos, boolean negrita, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED,
                puntos, negrita ? Font.BOLD : Font.NORMAL, color);
    }
}
