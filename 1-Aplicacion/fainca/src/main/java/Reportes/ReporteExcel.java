package Reportes;

import Objetos.Producto;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plantilla de reporte de inventario en Excel (.xlsx). El logo/empresa y el orden de
 * las columnas son fijos; la marca, la fecha y las filas se rellenan segun el reporte.
 * Opcionalmente incluye la descripcion y/o una foto (miniatura) de cada producto.
 */
public final class ReporteExcel {

    private static final byte[] AMARILLO = {(byte) 0xF1, (byte) 0xC4, 0x0F};
    private static final byte[] GRIS = {0x44, 0x44, 0x44};
    private static final byte[] GRIS_CLARO = {(byte) 0xF4, (byte) 0xF4, (byte) 0xF4};
    private static final byte[] BLANCO = {-1, -1, -1};

    private enum Col { FOTO, CODIGO, DESCRIPCION, CANTIDAD, UBICACION }

    private ReporteExcel() {
    }

    /**
     * @param miniaturas mapa codigo->miniatura JPEG cuadrada. Si es null, no se incluye
     *                   la columna de foto; si no lo es, se incluye (aunque algun producto
     *                   no tenga foto).
     */
    public static void generar(OutputStream out, String tituloMarca, List<Producto> productos,
                               byte[] logoPng, LocalDateTime generado, boolean incluirDescripcion,
                               Map<String, byte[]> miniaturas) throws IOException {
        boolean incluirFoto = miniaturas != null;

        // Orden fijo de columnas segun las opciones elegidas.
        List<Col> cols = new ArrayList<>();
        if (incluirFoto) cols.add(Col.FOTO);
        cols.add(Col.CODIGO);
        if (incluirDescripcion) cols.add(Col.DESCRIPCION);
        cols.add(Col.CANTIDAD);
        cols.add(Col.UBICACION);
        int ultima = cols.size() - 1;

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet hoja = wb.createSheet("Inventario");
            hoja.setDisplayGridlines(false);

            for (int c = 0; c <= ultima; c++) hoja.setColumnWidth(c, anchoCol(cols.get(c)) * 256);

            XSSFDrawing lienzo = hoja.createDrawingPatriarch();

            // --- Logo (empresa "en grande"), centrado sobre la tabla ---
            for (int f = 0; f <= 2; f++) hoja.createRow(f).setHeightInPoints(24);
            if (logoPng != null && logoPng.length > 0) {
                colocarLogo(wb, hoja, lienzo, logoPng, ultima);
            }

            bandaFusionada(hoja, 3, "REPORTE DE INVENTARIO — BODEGA #1",
                    base(wb, 13, true, BLANCO, GRIS, HorizontalAlignment.CENTER), 24, ultima);
            bandaFusionada(hoja, 4, tituloMarca,
                    base(wb, 16, true, GRIS, AMARILLO, HorizontalAlignment.CENTER), 28, ultima);
            String fecha = generado.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            bandaFusionada(hoja, 5, "Generado el " + fecha,
                    base(wb, 10, false, GRIS, null, HorizontalAlignment.CENTER), 16, ultima);

            // --- Encabezados de la tabla ---
            XSSFCellStyle estiloEncabezado = base(wb, 11, true, BLANCO, GRIS, HorizontalAlignment.CENTER);
            bordes(estiloEncabezado);
            int filaEncabezado = 7;
            XSSFRow cab = hoja.createRow(filaEncabezado);
            cab.setHeightInPoints(20);
            for (int c = 0; c <= ultima; c++) celda(cab, c, encabezado(cols.get(c)), estiloEncabezado);

            // --- Estilos de datos ---
            XSSFCellStyle txt = bordeado(base(wb, 11, false, GRIS, null, HorizontalAlignment.LEFT));
            XSSFCellStyle txtZebra = bordeado(base(wb, 11, false, GRIS, GRIS_CLARO, HorizontalAlignment.LEFT));
            XSSFCellStyle cen = bordeado(base(wb, 11, false, GRIS, null, HorizontalAlignment.CENTER));
            XSSFCellStyle cenZebra = bordeado(base(wb, 11, false, GRIS, GRIS_CLARO, HorizontalAlignment.CENTER));
            XSSFCellStyle desc = bordeado(base(wb, 10, false, GRIS, null, HorizontalAlignment.LEFT));
            desc.setWrapText(true);
            desc.setVerticalAlignment(VerticalAlignment.TOP);
            XSSFCellStyle descZebra = bordeado(base(wb, 10, false, GRIS, GRIS_CLARO, HorizontalAlignment.LEFT));
            descZebra.setWrapText(true);
            descZebra.setVerticalAlignment(VerticalAlignment.TOP);

            float altoFila = incluirFoto ? 44 : (incluirDescripcion ? 42 : 18);
            int fotoColPx = incluirFoto ? Math.round(hoja.getColumnWidthInPixels(cols.indexOf(Col.FOTO))) : 0;
            int filaAltoPx = (int) Math.round(altoFila * 96.0 / 72.0);

            int fila = filaEncabezado + 1;
            long totalUnidades = 0;
            for (int i = 0; i < productos.size(); i++) {
                Producto p = productos.get(i);
                boolean z = i % 2 == 1;
                XSSFRow r = hoja.createRow(fila);
                r.setHeightInPoints(altoFila);
                for (int c = 0; c <= ultima; c++) {
                    switch (cols.get(c)) {
                        case FOTO -> {
                            celda(r, c, "", z ? cenZebra : cen);
                            byte[] mini = miniaturas.get(p.getCodigo());
                            if (mini != null) anclarFoto(wb, lienzo, c, fila, fotoColPx, filaAltoPx, mini);
                        }
                        case CODIGO -> celda(r, c, p.getCodigo(), z ? txtZebra : txt);
                        case DESCRIPCION -> celda(r, c, p.getDescripcion() == null ? "" : p.getDescripcion(),
                                z ? descZebra : desc);
                        case CANTIDAD -> celdaNumero(r, c, p.getStockActual(), z ? cenZebra : cen);
                        case UBICACION -> celda(r, c, p.getUbicacion() == null ? "" : p.getUbicacion(),
                                z ? cenZebra : cen);
                    }
                }
                totalUnidades += p.getStockActual();
                fila++;
            }

            if (productos.isEmpty()) {
                XSSFRow r = hoja.createRow(fila++);
                celda(r, 0, "No hay productos activos para esta selección.", txt);
                hoja.addMergedRegion(new CellRangeAddress(r.getRowNum(), r.getRowNum(), 0, ultima));
                for (int c = 1; c <= ultima; c++) celda(r, c, "", txt);
            }

            // --- Totales ---
            fila++;
            XSSFCellStyle etTotal = base(wb, 11, true, GRIS, null, HorizontalAlignment.LEFT);
            XSSFCellStyle etTotalCen = base(wb, 11, true, GRIS, null, HorizontalAlignment.CENTER);
            XSSFRow filaTotales = hoja.createRow(fila);
            celda(filaTotales, cols.indexOf(Col.CODIGO), "Total de productos: " + productos.size(), etTotal);
            celda(filaTotales, cols.indexOf(Col.CANTIDAD), String.valueOf(totalUnidades), etTotalCen);
            celda(filaTotales, cols.indexOf(Col.UBICACION), "unidades", etTotal);

            hoja.createFreezePane(0, filaEncabezado + 1);
            wb.write(out);
        }
    }

    private static int anchoCol(Col col) {
        return switch (col) {
            case FOTO -> 11;
            case CODIGO -> 24;
            case DESCRIPCION -> 58;
            case CANTIDAD -> 13;
            case UBICACION -> 16;
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

    /** Ancla una miniatura cuadrada centrada dentro de la celda de foto de una fila. */
    private static void anclarFoto(XSSFWorkbook wb, XSSFDrawing lienzo, int col, int fila,
                                   int colPx, int filaPx, byte[] jpg) {
        int disp = Math.min(colPx, filaPx) - 8;   // margen dentro de la celda
        if (disp < 8) disp = Math.min(colPx, filaPx);
        int dx = Math.max(0, (colPx - disp) / 2);
        int dy = Math.max(0, (filaPx - disp) / 2);

        int idx = wb.addPicture(jpg, Workbook.PICTURE_TYPE_JPEG);
        XSSFClientAnchor a = new XSSFClientAnchor();
        a.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
        a.setCol1(col);
        a.setRow1(fila);
        a.setDx1(Units.pixelToEMU(dx));
        a.setDy1(Units.pixelToEMU(dy));
        a.setCol2(col);
        a.setRow2(fila);
        a.setDx2(Units.pixelToEMU(dx + disp));
        a.setDy2(Units.pixelToEMU(dy + disp));
        lienzo.createPicture(a, idx);
    }

    /**
     * Coloca el logo con tamaño fijo (independiente del numero/ancho de columnas) y
     * centrado sobre la tabla. Se usa un anclaje absoluto en pixeles porque resize()
     * calcula mal el tamaño con este PNG (deja la imagen en 0x0 y no se ve).
     */
    private static void colocarLogo(XSSFWorkbook wb, XSSFSheet hoja, XSSFDrawing lienzo,
                                    byte[] logoPng, int ultima) {
        final int LOGO_W = 300;
        final int LOGO_H = 77;   // conserva la proporcion ~3.9:1 del logo (2338x600)
        final int MARGEN_SUP = 8;

        int[] anchoCol = new int[ultima + 1];
        int totalPx = 0;
        for (int c = 0; c <= ultima; c++) {
            anchoCol[c] = Math.round(hoja.getColumnWidthInPixels(c));
            totalPx += anchoCol[c];
        }
        int offsetX = Math.max(0, (totalPx - LOGO_W) / 2);

        int idx = wb.addPicture(logoPng, Workbook.PICTURE_TYPE_PNG);
        XSSFClientAnchor a = new XSSFClientAnchor();
        a.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);

        int[] izq = pxACol(anchoCol, offsetX);
        int[] der = pxACol(anchoCol, offsetX + LOGO_W);
        a.setCol1(izq[0]);
        a.setDx1(Units.pixelToEMU(izq[1]));
        a.setCol2(der[0]);
        a.setDx2(Units.pixelToEMU(der[1]));

        int[] arriba = pxAFila(MARGEN_SUP);
        int[] abajo = pxAFila(MARGEN_SUP + LOGO_H);
        a.setRow1(arriba[0]);
        a.setDy1(Units.pixelToEMU(arriba[1]));
        a.setRow2(abajo[0]);
        a.setDy2(Units.pixelToEMU(abajo[1]));

        lienzo.createPicture(a, idx);
    }

    private static int[] pxACol(int[] anchos, int x) {
        int c = 0;
        while (c < anchos.length - 1 && x >= anchos[c]) {
            x -= anchos[c];
            c++;
        }
        return new int[]{c, Math.min(x, anchos[c])};
    }

    private static int[] pxAFila(int y) {
        final int ALTO_FILA_PX = 32;
        int r = 0;
        while (r < 2 && y >= ALTO_FILA_PX) {
            y -= ALTO_FILA_PX;
            r++;
        }
        return new int[]{r, Math.min(y, ALTO_FILA_PX)};
    }

    private static XSSFCellStyle base(XSSFWorkbook wb, int puntos, boolean negrita, byte[] colorTexto,
                                      byte[] relleno, HorizontalAlignment alineacion) {
        XSSFCellStyle estilo = wb.createCellStyle();
        estilo.setAlignment(alineacion);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setFont(fuente(wb, puntos, negrita, new XSSFColor(colorTexto, null)));
        if (relleno != null) {
            estilo.setFillForegroundColor(new XSSFColor(relleno, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return estilo;
    }

    private static XSSFCellStyle bordeado(XSSFCellStyle estilo) {
        bordes(estilo);
        return estilo;
    }

    private static void bandaFusionada(XSSFSheet hoja, int filaIdx, String texto, XSSFCellStyle estilo,
                                       int altoPt, int ultima) {
        XSSFRow fila = hoja.createRow(filaIdx);
        fila.setHeightInPoints(altoPt);
        celda(fila, 0, texto, estilo);
        hoja.addMergedRegion(new CellRangeAddress(filaIdx, filaIdx, 0, ultima));
        for (int c = 1; c <= ultima; c++) celda(fila, c, "", estilo);
    }

    private static XSSFFont fuente(XSSFWorkbook wb, int puntos, boolean negrita, XSSFColor color) {
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short) puntos);
        f.setBold(negrita);
        f.setColor(color);
        f.setFontName("Calibri");
        return f;
    }

    private static void bordes(XSSFCellStyle estilo) {
        XSSFColor borde = new XSSFColor(new byte[]{(byte) 0xDD, (byte) 0xDD, (byte) 0xDD}, null);
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
        estilo.setTopBorderColor(borde);
        estilo.setBottomBorderColor(borde);
        estilo.setLeftBorderColor(borde);
        estilo.setRightBorderColor(borde);
    }

    private static void celda(XSSFRow fila, int col, String valor, XSSFCellStyle estilo) {
        XSSFCell c = fila.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private static void celdaNumero(XSSFRow fila, int col, int valor, XSSFCellStyle estilo) {
        XSSFCell c = fila.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }
}
