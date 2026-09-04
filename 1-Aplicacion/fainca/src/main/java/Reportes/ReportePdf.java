package Reportes;

import Objetos.Producto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plantilla de reporte de inventario en PDF. Misma estructura que la de Excel:
 * logo/empresa arriba, luego marca y fecha, y una tabla con columnas fijas
 * (Código/Cantidad/Ubicación), opcionalmente con Descripción y/o Foto.
 */
public final class ReportePdf {

    private static final Color AMARILLO = new Color(0xF1, 0xC4, 0x0F);
    private static final Color GRIS = new Color(0x44, 0x44, 0x44);
    private static final Color GRIS_CLARO = new Color(0xF4, 0xF4, 0xF4);
    private static final Color BORDE = new Color(0xDD, 0xDD, 0xDD);

    private enum Col { FOTO, CODIGO, DESCRIPCION, CANTIDAD, UBICACION }

    private ReportePdf() {
    }

    public static void generar(OutputStream out, String tituloMarca, List<Producto> productos,
                               byte[] logoPng, LocalDateTime generado, boolean incluirDescripcion,
                               Map<String, byte[]> miniaturas) throws Exception {
        boolean incluirFoto = miniaturas != null;

        List<Col> cols = new ArrayList<>();
        if (incluirFoto) cols.add(Col.FOTO);
        cols.add(Col.CODIGO);
        if (incluirDescripcion) cols.add(Col.DESCRIPCION);
        cols.add(Col.CANTIDAD);
        cols.add(Col.UBICACION);

        Document doc = new Document(PageSize.A4, 42, 42, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // --- Logo (empresa "en grande"), centrado ---
        if (logoPng != null && logoPng.length > 0) {
            Image logo = Image.getInstance(logoPng);
            logo.scaleToFit(230, 90);
            logo.setAlignment(Image.ALIGN_CENTER);
            doc.add(logo);
        }

        Paragraph subtitulo = new Paragraph("REPORTE DE INVENTARIO — BODEGA #1", fuente(12, true, GRIS));
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingBefore(8);
        doc.add(subtitulo);

        PdfPTable franja = new PdfPTable(1);
        franja.setWidthPercentage(100);
        franja.setSpacingBefore(8);
        PdfPCell celdaMarca = new PdfPCell(new Phrase(tituloMarca, fuente(17, true, GRIS)));
        celdaMarca.setBackgroundColor(AMARILLO);
        celdaMarca.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaMarca.setPadding(8);
        celdaMarca.setBorder(0);
        franja.addCell(celdaMarca);
        doc.add(franja);

        String fecha = generado.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        Paragraph pFecha = new Paragraph("Generado el " + fecha, fuente(9, false, GRIS));
        pFecha.setAlignment(Element.ALIGN_CENTER);
        pFecha.setSpacingBefore(4);
        pFecha.setSpacingAfter(12);
        doc.add(pFecha);

        // --- Tabla ---
        float[] proporciones = new float[cols.size()];
        for (int i = 0; i < cols.size(); i++) proporciones[i] = ancho(cols.get(i));

        PdfPTable tabla = new PdfPTable(cols.size());
        tabla.setWidthPercentage(100);
        tabla.setWidths(proporciones);
        tabla.setHeaderRows(1);

        for (Col col : cols) {
            PdfPCell c = new PdfPCell(new Phrase(encabezado(col), fuente(11, true, Color.WHITE)));
            c.setBackgroundColor(GRIS);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(6);
            c.setBorderColor(GRIS);
            tabla.addCell(c);
        }

        long totalUnidades = 0;
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            Color fondo = i % 2 == 1 ? GRIS_CLARO : Color.WHITE;
            for (Col col : cols) {
                switch (col) {
                    case FOTO -> tabla.addCell(celdaFoto(miniaturas.get(p.getCodigo()), fondo));
                    case CODIGO -> tabla.addCell(celdaDato(p.getCodigo(), Element.ALIGN_LEFT, fondo));
                    case DESCRIPCION -> tabla.addCell(celdaDato(
                            p.getDescripcion() == null ? "" : p.getDescripcion(), Element.ALIGN_LEFT, fondo));
                    case CANTIDAD -> tabla.addCell(celdaDato(
                            String.valueOf(p.getStockActual()), Element.ALIGN_CENTER, fondo));
                    case UBICACION -> tabla.addCell(celdaDato(
                            p.getUbicacion() == null ? "" : p.getUbicacion(), Element.ALIGN_CENTER, fondo));
                }
            }
            totalUnidades += p.getStockActual();
        }

        if (productos.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("No hay productos activos para esta selección.",
                    fuente(11, false, GRIS)));
            vacio.setColspan(cols.size());
            vacio.setPadding(10);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            vacio.setBorderColor(BORDE);
            tabla.addCell(vacio);
        }

        doc.add(tabla);

        Paragraph totales = new Paragraph(
                "Total de productos: " + productos.size() + "        Total de unidades: " + totalUnidades,
                fuente(11, true, GRIS));
        totales.setSpacingBefore(12);
        doc.add(totales);

        doc.close();
    }

    private static float ancho(Col col) {
        // Pesos elegidos para que "CANTIDAD" y "UBICACIÓN" quepan en su encabezado
        // incluso en el caso mas apretado (5 columnas: foto + descripcion).
        return switch (col) {
            case FOTO -> 1.0f;
            case CODIGO -> 2.0f;
            case DESCRIPCION -> 3.4f;
            case CANTIDAD -> 1.5f;
            case UBICACION -> 1.6f;
        };
    }

    private static String encabezado(Col col) {
        return switch (col) {
            case FOTO -> "FOTO";
            case CODIGO -> "CÓDIGO";
            case DESCRIPCION -> "DESCRIPCIÓN";
            case CANTIDAD -> "CANTIDAD";
            case UBICACION -> "UBICACIÓN";
        };
    }

    private static Font fuente(int puntos, boolean negrita, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED,
                puntos, negrita ? Font.BOLD : Font.NORMAL, color);
    }

    private static PdfPCell celdaDato(String texto, int alineacion, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto, fuente(10, false, GRIS)));
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
        if (jpg != null) {
            Image img = Image.getInstance(jpg);
            img.scaleToFit(46, 46);
            c.setImage(img);
        }
        return c;
    }
}
