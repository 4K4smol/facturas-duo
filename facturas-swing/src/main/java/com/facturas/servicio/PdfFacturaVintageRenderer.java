package com.facturas.servicio;

import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.Factura;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PdfFacturaVintageRenderer extends PdfFacturaService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DecimalFormat DINERO = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(ES));
    private static final float H = PageSize.A4.getHeight();
    private static final Color BLACK = Color.BLACK;
    private static final Color BLUE = Color.BLUE;
    private static final Color GOLD = new Color(255, 192, 0);
    private static final Color GRAY = new Color(191, 191, 191);

    private BaseFont regular;
    private BaseFont bold;

    @Override
    public Path generarPdf(Factura factura, Empresa empresa, Path carpetaSalida, String formaPago) throws IOException, DocumentException {
        Files.createDirectories(carpetaSalida);
        Path salida = carpetaSalida.resolve("factura_" + nombreArchivo(factura.getNumero()) + ".pdf");
        Document doc = new Document(PageSize.A4, 0, 0, 0, 0);
        try (FileOutputStream out = new FileOutputStream(salida.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();
            regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            PdfContentByte cb = writer.getDirectContent();
            fondos(cb);
            lineas(cb);
            textos(cb, factura, empresa, formaPago);
            logo(cb, empresa);
            doc.close();
        }
        return salida;
    }

    private void fondos(PdfContentByte cb) {
        rect(cb, 129.00f, 687.84f, 331.20f, 14.64f, GRAY);
        rect(cb, 192.60f, 716.88f, 267.60f, 14.64f, GRAY);
    }

    private void lineas(PdfContentByte cb) {
        float[][] black = {
                {131.16f,285.60f,99.12f,.84f},{130.92f,315.24f,40.56f,.72f},{130.92f,329.76f,16.92f,.72f},
                {130.92f,344.28f,46.08f,.72f},{130.92f,358.80f,14.76f,.72f},{131.16f,420.36f,62.88f,.84f},{131.16f,627.24f,54f,.84f},
                {102.24f,53.76f,1.92f,144.36f},{525.24f,55.20f,1.92f,142.92f},{104.16f,53.76f,423f,1.44f},{104.16f,196.20f,423f,1.92f},
                {102.24f,211.44f,1.92f,31.68f},{525.24f,213.36f,1.92f,29.76f},{104.16f,211.44f,423f,1.92f},{104.16f,241.20f,423f,1.92f},
                {102.24f,256.44f,1.92f,120.96f},{525.24f,258.36f,1.92f,119.04f},{104.16f,256.44f,423f,1.92f},{104.16f,375.48f,423f,1.92f},
                {102.24f,390.72f,1.92f,193.56f},{525.24f,392.64f,1.92f,191.64f},{104.16f,390.72f,423f,1.92f},{104.16f,582.36f,423f,1.92f},
                {354.00f,422.88f,.96f,145.32f},{129.00f,436.92f,331.20f,.96f},{192.60f,553.08f,267.60f,.96f},
                {102.24f,597.60f,1.92f,150f},{525.24f,599.52f,1.92f,148.08f},{104.16f,597.60f,423f,1.92f},{104.16f,745.68f,423f,1.92f},
                {354.00f,629.76f,.96f,72.72f},{129.00f,643.80f,331.20f,.96f},{129.00f,687.36f,331.20f,.96f},
                {192.12f,716.40f,.96f,15.48f},{459.60f,717.36f,.96f,14.52f},{193.08f,716.40f,267.48f,.96f},{193.08f,730.92f,267.48f,.96f}
        };
        for (float[] r : black) rect(cb, r[0], r[1], r[2], r[3], BLACK);
        rect(cb, 130.92f, 179.52f, 127.80f, .72f, BLUE);
        rect(cb, 388.20f, 71.16f, 105.24f, 1.08f, GOLD);
    }

    private void textos(PdfContentByte cb, Factura f, Empresa e, String pago) {
        Cliente c = f.getCliente();
        BigDecimal base = money(f.getBaseImponible());
        BigDecimal iva = money(f.getIva());
        BigDecimal total = money(f.getTotal());
        text(cb, val(e.getNombre()).toUpperCase(ES), 131.04f, 93.84f, bold, 12f, BLACK);
        text(cb, val(e.getNif()), 131.04f, 109.08f, bold, 12f, BLACK);
        text(cb, val(e.getDireccion()), 131.04f, 124.32f, bold, 12f, BLACK);
        text(cb, val(e.getCodigoPostal()) + " " + val(e.getLocalidad()), 131.04f, 139.56f, bold, 12f, BLACK);
        text(cb, val(e.getTelefono()), 135.48f, 154.80f, bold, 12f, BLACK);
        text(cb, val(e.getEmail()), 130.92f, 170.03f, regular, 11f, BLUE);
        text(cb, "LIMPIEZAS DÚO", 388.21f, 57.38f, bold, 16f, GOLD);
        LocalDate fecha = f.getFecha() == null ? LocalDate.now() : f.getFecha();
        right(cb, val(e.getLocalidad()) + ", " + fecha.format(FECHA), 516.65f, 215.15f, bold, 11f, BLACK);
        text(cb, "Nº Factura: " + val(f.getNumero()), 406.33f, 230.03f, bold, 11f, BLACK);
        String[] nf = split(nombreFiscal(c));
        text(cb, "Nombre comercial", 131.17f, 274.43f, bold, 13f, BLACK);
        text(cb, val(c == null ? "" : c.getNombre()), 194.52f, 290.88f, bold, 11f, BLACK);
        text(cb, "Nombre:", 130.92f, 305.76f, bold, 11f, BLACK);
        text(cb, nf[0], 194.52f, 305.76f, regular, 11f, BLACK);
        text(cb, nf[1], 291.96f, 305.76f, regular, 11f, BLACK);
        text(cb, "CIF:", 130.92f, 320.27f, bold, 11f, BLACK);
        text(cb, val(c == null ? "" : c.getNif()), 194.52f, 320.27f, regular, 11f, BLACK);
        text(cb, "Dirección:", 130.92f, 334.79f, bold, 11f, BLACK);
        text(cb, val(c == null ? "" : c.getDireccion()), 194.52f, 334.79f, regular, 11f, BLACK);
        text(cb, "CP:", 130.92f, 349.31f, bold, 11f, BLACK);
        text(cb, val(c == null ? "" : c.getCodigoPostal()) + ", " + val(c == null ? "" : c.getLocalidad()), 194.52f, 349.31f, regular, 11f, BLACK);
        text(cb, "CONCEPTO:", 131.17f, 409.18f, bold, 13f, BLACK);
        text(cb, "Trabajo", 224.76f, 425.15f, bold, 11f, BLACK);
        text(cb, "Precio (sin IVA)", 388.21f, 425.63f, bold, 11f, BLACK);
        right(cb, concepto(f), 352.52f, 454.68f, regular, 11f, BLACK);
        right(cb, euros(base), 458.13f, 454.55f, regular, 11f, BLACK);
        text(cb, "Total", 329.17f, 556.31f, bold, 11f, BLACK);
        right(cb, euros(base), 458.13f, 556.31f, regular, 11f, BLACK);
        text(cb, "IMPORTE:", 131.17f, 616.07f, bold, 13f, BLACK);
        right(cb, "Total", 458.17f, 632.51f, bold, 11f, BLACK);
        text(cb, "Base imponible", 130.92f, 647.04f, regular, 11f, BLACK);
        right(cb, euros(base), 458.13f, 647.04f, regular, 11f, BLACK);
        text(cb, "IVA", 130.92f, 661.55f, regular, 11f, BLACK);
        text(cb, "21%", 194.52f, 661.55f, regular, 11f, BLACK);
        right(cb, euros(iva), 458.13f, 661.55f, regular, 11f, BLACK);
        text(cb, "Retenciones", 130.92f, 676.07f, regular, 11f, BLACK);
        text(cb, "0%", 194.52f, 676.07f, regular, 11f, BLACK);
        right(cb, euros(BigDecimal.ZERO), 458.13f, 676.07f, regular, 11f, BLACK);
        text(cb, "TOTAL A COBRAR", 130.92f, 690.11f, bold, 11f, BLACK);
        right(cb, euros(total), 458.13f, 690.59f, bold, 11f, BLACK);
        text(cb, "Tipo de pago:", 194.52f, 719.64f, bold, 11f, BLACK);
        text(cb, normaliza(pago).contains("metalico") ? "Metálico ■" : "Metálico □", 291.96f, 719.64f, bold, 11f, BLACK);
        text(cb, normaliza(pago).contains("cargo") ? "Cargo a cuenta ■" : "Cargo a cuenta □", 380.89f, 719.64f, bold, 11f, BLACK);
    }

    private void logo(PdfContentByte cb, Empresa e) throws IOException, DocumentException {
        if (e != null && e.getLogoPath() != null && new File(e.getLogoPath()).isFile()) {
            Image logo = Image.getInstance(e.getLogoPath());
            logo.scaleAbsolute(120.24f, 109.44f);
            logo.setAbsolutePosition(382.44f, y(72.72f, 109.44f));
            cb.addImage(logo);
        }
    }

    private void rect(PdfContentByte cb, float x, float top, float w, float h, Color c) { cb.saveState(); cb.setColorFill(c); cb.rectangle(x, y(top, h), w, h); cb.fill(); cb.restoreState(); }
    private void text(PdfContentByte cb, String s, float x, float top, BaseFont f, float size, Color c) { cb.saveState(); cb.beginText(); cb.setColorFill(c); cb.setFontAndSize(f, size); cb.showTextAligned(Element.ALIGN_LEFT, s, x, H - top - size, 0); cb.endText(); cb.restoreState(); }
    private void right(PdfContentByte cb, String s, float x, float top, BaseFont f, float size, Color c) { cb.saveState(); cb.beginText(); cb.setColorFill(c); cb.setFontAndSize(f, size); cb.showTextAligned(Element.ALIGN_RIGHT, s, x, H - top - size, 0); cb.endText(); cb.restoreState(); }
    private float y(float top, float h) { return H - top - h; }
    private String euros(BigDecimal v) { return DINERO.format(money(v)) + " €"; }
    private BigDecimal money(BigDecimal v) { return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP); }
    private String val(String v) { return v == null ? "" : v.trim(); }
    private String concepto(Factura f) { return val(f.getConcepto()).isBlank() ? "Limpieza de cristales" : val(f.getConcepto()); }
    private String nombreFiscal(Cliente c) { if (c == null) return ""; return val(c.getRazonSocial()).isBlank() ? val(c.getNombre()) : val(c.getRazonSocial()); }
    private String[] split(String s) { int i = val(s).indexOf(' '); return i < 0 ? new String[]{val(s), ""} : new String[]{s.substring(0, i), s.substring(i + 1)}; }
    private String normaliza(String s) { return val(s).toLowerCase(ES).replace('á','a').replace('é','e').replace('í','i').replace('ó','o').replace('ú','u'); }
    private String nombreArchivo(String n) { return val(n).isBlank() ? "sin_numero" : n.replaceAll("[^a-zA-Z0-9._-]", "_"); }
}
