package Reportes;

import Objetos.ActaHerramienta;
import Objetos.ActaLineaHerramienta;
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

/**
 * Acta de entrega de herramientas en PDF: el documento que el tecnico firma al
 * retirar. Mismo estilo que el comprobante de movimientos (logo, franja amarilla,
 * lista numerada), mas la seccion de firmas ENTREGADO POR / RECIBIDO POR.
 */
public final class ActaHerramientasPdf {

    private static final Color AMARILLO = new Color(0xF1, 0xC4, 0x0F);
    private static final Color GRIS = new Color(0x44, 0x44, 0x44);
    private static final Color GRIS_CLARO = new Color(0xF4, 0xF4, 0xF4);
    private static final Color BORDE = new Color(0xDD, 0xDD, 0xDD);

    private ActaHerramientasPdf() {
    }

    public static void generar(OutputStream out, ActaHerramienta acta, byte[] logoPng,
                               LocalDateTime generado, String generadoPor) throws Exception {

        if (acta == null || acta.getLineas() == null || acta.getLineas().isEmpty()) {
            throw new IllegalArgumentException("El acta no tiene líneas");
        }

        Document doc = new Document(PageSize.A4, 40, 40, 36, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        // ---------- Encabezado: logo + titulo ----------
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
        Paragraph tit = new Paragraph("ACTA DE ENTREGA DE HERRAMIENTAS", fuente(15, true, GRIS));
        tit.setAlignment(Element.ALIGN_RIGHT);
        Paragraph num = new Paragraph("Acta N.° " + acta.getNumero(), fuente(11, false, GRIS));
        num.setAlignment(Element.ALIGN_RIGHT);
        num.setSpacingBefore(3);
        Paragraph bod = new Paragraph("Bodega de Herramientas", fuente(9, false, new Color(0x88, 0x88, 0x88)));
        bod.setAlignment(Element.ALIGN_RIGHT);
        celdaTitulo.addElement(tit);
        celdaTitulo.addElement(num);
        celdaTitulo.addElement(bod);
        cabecera.addCell(celdaTitulo);
        doc.add(cabecera);

        // Franja amarilla con el estado
        PdfPTable franja = new PdfPTable(1);
        franja.setWidthPercentage(100);
        franja.setSpacingBefore(10);
        String estado = "cerrada".equals(acta.getEstado())
                ? "ENTREGA DE HERRAMIENTAS Y MATERIALES — ACTA CERRADA"
                : "ENTREGA DE HERRAMIENTAS Y MATERIALES";
        PdfPCell fc = new PdfPCell(new Phrase(estado, fuente(12, true, GRIS)));
        fc.setBackgroundColor(AMARILLO);
        fc.setHorizontalAlignment(Element.ALIGN_CENTER);
        fc.setPadding(7);
        fc.setBorder(0);
        franja.addCell(fc);
        doc.add(franja);

        // ---------- Datos de la entrega ----------
        PdfPTable datos = new PdfPTable(2);
        datos.setWidthPercentage(100);
        datos.setWidths(new float[]{1f, 3f});
        datos.setSpacingBefore(12);
        agregarDato(datos, "Fecha y hora", formatearFecha(acta.getFecha()));
        agregarDato(datos, "Solicitante", acta.getSolicitante());
        agregarDato(datos, "Proyecto", acta.getProyecto());
        agregarDato(datos, "Destino", acta.getDestino() == null || acta.getDestino().isBlank()
                ? "—" : acta.getDestino());
        agregarDato(datos, "Entregado por", acta.getUsuario());
        agregarDato(datos, "Observación",
                acta.getObservaciones() == null || acta.getObservaciones().isBlank()
                        ? "(sin observación)" : acta.getObservaciones());
        doc.add(datos);

        // ---------- Detalle ----------
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(14);
        tabla.setWidths(new float[]{0.55f, 0.8f, 3.6f, 1.2f, 2.6f});
        tabla.setHeaderRows(1);

        encabezado(tabla, "ÍTEM");
        encabezado(tabla, "CANT.");
        encabezado(tabla, "DETALLE");
        encabezado(tabla, "TIPO");
        encabezado(tabla, "OBSERVACIÓN");

        long totalUnidades = 0;
        for (int i = 0; i < acta.getLineas().size(); i++) {
            ActaLineaHerramienta l = acta.getLineas().get(i);
            Color fondo = i % 2 == 1 ? GRIS_CLARO : Color.WHITE;

            tabla.addCell(celda(String.valueOf(i + 1), Element.ALIGN_CENTER, fondo, true));
            tabla.addCell(celda(String.valueOf(l.getCantidad()), Element.ALIGN_CENTER, fondo, true));
            tabla.addCell(celda(l.getNombre(), Element.ALIGN_LEFT, fondo, false));
            tabla.addCell(celda("consumible".equals(l.getTipo()) ? "Consumible" : "Herramienta",
                    Element.ALIGN_CENTER, fondo, false));
            tabla.addCell(celda(l.getObservacion(), Element.ALIGN_LEFT, fondo, false));

            totalUnidades += l.getCantidad();
        }
        doc.add(tabla);

        // ---------- Totales ----------
        PdfPTable totales = new PdfPTable(1);
        totales.setWidthPercentage(100);
        totales.setSpacingBefore(10);
        PdfPCell tc = new PdfPCell(new Phrase(
                "TOTAL:  " + acta.getLineas().size() + " ítem(s)        " + totalUnidades + " unidad(es)",
                fuente(11, true, GRIS)));
        tc.setBackgroundColor(GRIS_CLARO);
        tc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tc.setPadding(7);
        tc.setBorderColor(BORDE);
        totales.addCell(tc);
        doc.add(totales);

        // Nota de responsabilidad
        Paragraph nota = new Paragraph(
                "El solicitante declara recibir conforme lo detallado y se compromete a devolver las "
                        + "herramientas en buen estado al finalizar el trabajo. Los consumibles no se devuelven.",
                fuente(8.5f, false, new Color(0x77, 0x77, 0x77)));
        nota.setSpacingBefore(12);
        doc.add(nota);

        // ---------- Firmas ----------
        PdfPTable firmas = new PdfPTable(2);
        firmas.setWidthPercentage(88);
        firmas.setSpacingBefore(48);
        firmas.setWidths(new float[]{1f, 1f});

        firmas.addCell(celdaFirma("ENTREGADO POR", acta.getUsuario()));
        firmas.addCell(celdaFirma("RECIBIDO POR (SOLICITANTE)", acta.getSolicitante()));
        doc.add(firmas);

        // ---------- Pie ----------
        Paragraph pie = new Paragraph(
                "Documento generado por " + generadoPor + " el "
                        + generado.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"))
                        + "  ·  Sistema de Inventario FAINCA — Bodega de Herramientas",
                fuente(8, false, new Color(0x99, 0x99, 0x99)));
        pie.setAlignment(Element.ALIGN_CENTER);
        pie.setSpacingBefore(20);
        doc.add(pie);

        doc.close();
    }

    /** Linea de firma: raya arriba, etiqueta y nombre debajo. */
    private static PdfPCell celdaFirma(String etiqueta, String nombre) {
        PdfPCell c = new PdfPCell();
        c.setBorder(0);
        c.setPaddingLeft(18);
        c.setPaddingRight(18);

        PdfPTable interna = new PdfPTable(1);
        interna.setWidthPercentage(100);

        PdfPCell raya = new PdfPCell(new Phrase(" "));
        raya.setBorder(PdfPCell.BOTTOM);
        raya.setBorderColor(GRIS);
        raya.setFixedHeight(30);
        interna.addCell(raya);

        PdfPCell texto = new PdfPCell();
        texto.setBorder(0);
        Paragraph pe = new Paragraph(etiqueta, fuente(8.5f, true, GRIS));
        pe.setAlignment(Element.ALIGN_CENTER);
        Paragraph pn = new Paragraph(nombre == null ? "" : nombre, fuente(9.5f, false, GRIS));
        pn.setAlignment(Element.ALIGN_CENTER);
        texto.addElement(pe);
        texto.addElement(pn);
        texto.setPaddingTop(4);
        interna.addCell(texto);

        c.addElement(interna);
        return c;
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

    private static Font fuente(float puntos, boolean negrita, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED,
                puntos, negrita ? Font.BOLD : Font.NORMAL, color);
    }
}
