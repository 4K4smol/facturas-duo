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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PdfFacturaVintageRenderer extends PdfFacturaService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DecimalFormat DINERO = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(ES));
    private static final float H = PageSize.A4.getHeight();
    private static final float MIN_TEXT_SIZE = 8f;
    private static final float CHECKBOX_SIZE = 6.2f;
    private static final Color BLACK = Color.BLACK;
    private static final Color BLUE = new Color(6, 86, 179);
    private static final Color GOLD = new Color(242, 169, 0);
    private static final Color GRAY = new Color(235, 235, 235);

    private BaseFont regular;
    private BaseFont bold;

    @Override
    public Path generarPdf(Factura factura, Empresa empresa, Path carpetaSalida, String formaPago)
            throws IOException, DocumentException {
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
        rect(cb, 0f, 0f, PageSize.A4.getWidth(), PageSize.A4.getHeight(), Color.WHITE);
        rect(cb, 129.00f, 673.32f, 331.20f, 14.64f, GRAY);
        rect(cb, 192.60f, 716.88f, 267.60f, 14.64f, GRAY);
    }

    private void lineas(PdfContentByte cb) {
        float[][] black = {
                { 131.16f, 289.30f, 99.12f, .84f }, { 130.92f, 318.50f, 40.56f, .72f },
                { 130.92f, 333.00f, 16.92f, .72f },
                { 130.92f, 347.60f, 46.08f, .72f }, { 130.92f, 362.10f, 14.76f, .72f },
                { 131.16f, 424.20f, 62.88f, .84f }, { 131.16f, 631.20f, 54f, .84f },
                { 102.24f, 53.76f, 1.92f, 144.36f }, { 525.24f, 55.20f, 1.92f, 142.92f },
                { 104.16f, 53.76f, 423f, 1.44f }, { 104.16f, 196.20f, 423f, 1.92f },
                { 102.24f, 211.44f, 1.92f, 31.68f }, { 525.24f, 213.36f, 1.92f, 29.76f },
                { 104.16f, 211.44f, 423f, 1.92f }, { 104.16f, 241.20f, 423f, 1.92f },
                { 102.24f, 256.44f, 1.92f, 120.96f }, { 525.24f, 258.36f, 1.92f, 119.04f },
                { 104.16f, 256.44f, 423f, 1.92f }, { 104.16f, 375.48f, 423f, 1.92f },
                { 102.24f, 390.72f, 1.92f, 193.56f }, { 525.24f, 392.64f, 1.92f, 191.64f },
                { 104.16f, 390.72f, 423f, 1.92f }, { 104.16f, 582.36f, 423f, 1.92f },
                { 354.00f, 422.88f, .96f, 145.32f }, { 129.00f, 436.92f, 331.20f, .96f },
                { 192.60f, 553.08f, 267.60f, .96f },
                { 102.24f, 597.60f, 1.92f, 150f }, { 525.24f, 599.52f, 1.92f, 148.08f },
                { 104.16f, 597.60f, 423f, 1.92f }, { 104.16f, 745.68f, 423f, 1.92f },
                { 354.00f, 629.76f, .96f, 58.20f }, { 129.00f, 643.80f, 331.20f, .96f },
                { 129.00f, 672.84f, 331.20f, .96f },
                { 192.12f, 716.40f, .96f, 15.48f }, { 459.60f, 717.36f, .96f, 14.52f },
                { 193.08f, 716.40f, 267.48f, .96f }, { 193.08f, 730.92f, 267.48f, .96f }
        };
        for (float[] r : black) {
            rect(cb, r[0], r[1], r[2], r[3], BLACK);
        }
        rect(cb, 130.92f, 179.52f, 127.80f, .72f, BLUE);
        rect(cb, 388.20f, 71.16f, 105.24f, 1.08f, GOLD);
    }

    private void textos(PdfContentByte cb, Factura f, Empresa e, String pago) {
        Cliente c = f.getCliente();
        BigDecimal base = money(f.getBaseImponible());
        BigDecimal iva = money(f.getIva());
        BigDecimal total = money(f.getTotal());

        fitLeft(cb, val(e.getNombre()).toUpperCase(ES), 131.04f, 93.84f, 220f, bold, 11f, BLACK);
        fitLeft(cb, val(e.getNif()), 131.04f, 109.08f, 220f, bold, 11f, BLACK);
        fitLeft(cb, val(e.getDireccion()), 131.04f, 124.32f, 220f, bold, 11f, BLACK);
        fitLeft(cb, (val(e.getCodigoPostal()) + " " + val(e.getLocalidad())).trim(), 131.04f, 139.56f,
                220f, bold, 11f, BLACK);
        fitLeft(cb, val(e.getTelefono()), 135.48f, 154.80f, 215f, bold, 11f, BLACK);
        fitLeft(cb, val(e.getEmail()), 130.92f, 170.03f, 220f, regular, 11f, BLUE);
        if (!tieneLogo(e)) {
            fitRight(cb, "LIMPIEZAS DÚO", 515f, 57.38f, 145f, bold, 18f, GOLD);
        }

        LocalDate fecha = f.getFecha() == null ? LocalDate.now() : f.getFecha();
        fitRight(cb, (val(e.getLocalidad()) + ", " + fecha.format(FECHA)).replaceFirst("^,\\s*", ""),
                516.65f, 215.15f, 300f, bold, 11f, BLACK);
        fitRight(cb, "Nº Factura: " + val(f.getNumero()), 516.65f, 230.03f, 220f, bold, 11f, BLACK);

        text(cb, "Nombre comercial", 131.17f, 274.43f, bold, 13f, BLACK);
        fitLeft(cb, val(c == null ? "" : c.getNombre()), 194.52f, 290.88f, 305f, bold, 11f, BLACK);
        text(cb, "Nombre:", 130.92f, 305.76f, bold, 11f, BLACK);
        fitLeft(cb, nombreFiscal(c), 194.52f, 305.76f, 305f, regular, 11f, BLACK);
        text(cb, "CIF:", 130.92f, 320.27f, bold, 11f, BLACK);
        fitLeft(cb, val(c == null ? "" : c.getNif()), 194.52f, 320.27f, 305f, regular, 11f, BLACK);
        text(cb, "Dirección:", 130.92f, 334.79f, bold, 11f, BLACK);
        fitLeft(cb, val(c == null ? "" : c.getDireccion()), 194.52f, 334.79f, 305f, regular, 11f, BLACK);
        text(cb, "CP:", 130.92f, 349.31f, bold, 11f, BLACK);
        fitLeft(cb, (val(c == null ? "" : c.getCodigoPostal()) + ", "
                        + val(c == null ? "" : c.getLocalidad())).replaceFirst("^,\\s*", ""),
                194.52f, 349.31f, 305f, regular, 11f, BLACK);

        text(cb, "CONCEPTO:", 131.17f, 409.18f, bold, 13f, BLACK);
        text(cb, "Trabajo", 224.76f, 425.15f, bold, 11f, BLACK);
        text(cb, "Precio (sin IVA)", 388.21f, 425.63f, bold, 11f, BLACK);
        wrappedText(cb, concepto(f), 144f, 454.68f, 194f, regular, 11f, BLACK, 3);
        fitRight(cb, euros(base), 458.13f, 454.55f, 93f, regular, 11f, BLACK);
        text(cb, "Total", 329.17f, 556.31f, bold, 11f, BLACK);
        fitRight(cb, euros(base), 458.13f, 556.31f, 93f, regular, 11f, BLACK);

        text(cb, "IMPORTE:", 131.17f, 616.07f, bold, 13f, BLACK);
        fitRight(cb, "Total", 458.17f, 632.51f, 93f, bold, 11f, BLACK);
        text(cb, "Base imponible", 130.92f, 647.04f, regular, 11f, BLACK);
        fitRight(cb, euros(base), 458.13f, 647.04f, 93f, regular, 11f, BLACK);
        text(cb, "IVA", 130.92f, 661.55f, regular, 11f, BLACK);
        text(cb, "21%", 194.52f, 661.55f, regular, 11f, BLACK);
        fitRight(cb, euros(iva), 458.13f, 661.55f, 93f, regular, 11f, BLACK);
        text(cb, "TOTAL A COBRAR", 130.92f, 675.59f, bold, 11f, BLACK);
        fitRight(cb, euros(total), 458.13f, 676.07f, 93f, bold, 11f, BLACK);

        text(cb, "Tipo de pago:", 198f, 719.20f, bold, 10.5f, BLACK);
        text(cb, "Metálico", 300f, 719.20f, bold, 10.5f, BLACK);
        checkBox(cb, 349f, 720.20f, normaliza(pago).contains("metalico"));
        fitLeft(cb, "Cargo a cuenta", 373f, 719.20f, 71f, bold, 10.5f, BLACK);
        checkBox(cb, 451.5f, 720.20f, normaliza(pago).contains("cargo"));
    }

    private void logo(PdfContentByte cb, Empresa e) throws IOException, DocumentException {
        if (!tieneLogo(e)) {
            return;
        }
        Image logo = Image.getInstance(e.getLogoPath());
        logo.scaleToFit(135f, 108f);
        float x = 515f - logo.getScaledWidth();
        logo.setAbsolutePosition(x, y(68f, logo.getScaledHeight()));
        cb.addImage(logo);
    }

    private boolean tieneLogo(Empresa e) {
        return e != null && e.getLogoPath() != null && new File(e.getLogoPath()).isFile();
    }

    private void rect(PdfContentByte cb, float x, float top, float w, float h, Color c) {
        cb.saveState();
        cb.setColorFill(c);
        cb.rectangle(x, y(top, h), w, h);
        cb.fill();
        cb.restoreState();
    }

    private void checkBox(PdfContentByte cb, float x, float top, boolean checked) {
        float bottom = y(top, CHECKBOX_SIZE);
        cb.saveState();
        cb.setColorStroke(BLACK);
        cb.setLineWidth(.8f);
        cb.rectangle(x, bottom, CHECKBOX_SIZE, CHECKBOX_SIZE);
        cb.stroke();
        if (checked) {
            cb.setLineWidth(1f);
            cb.moveTo(x + 1.2f, bottom + 1.2f);
            cb.lineTo(x + CHECKBOX_SIZE - 1.2f, bottom + CHECKBOX_SIZE - 1.2f);
            cb.moveTo(x + 1.2f, bottom + CHECKBOX_SIZE - 1.2f);
            cb.lineTo(x + CHECKBOX_SIZE - 1.2f, bottom + 1.2f);
            cb.stroke();
        }
        cb.restoreState();
    }

    private void text(PdfContentByte cb, String s, float x, float top, BaseFont f, float size, Color c) {
        drawText(cb, val(s), x, top, f, size, c, Element.ALIGN_LEFT);
    }

    private void fitLeft(PdfContentByte cb, String s, float x, float top, float maxWidth,
                         BaseFont f, float size, Color c) {
        FitText fit = fit(s, f, size, maxWidth);
        drawText(cb, fit.text(), x, top, f, fit.size(), c, Element.ALIGN_LEFT);
    }

    private void fitRight(PdfContentByte cb, String s, float x, float top, float maxWidth,
                          BaseFont f, float size, Color c) {
        FitText fit = fit(s, f, size, maxWidth);
        drawText(cb, fit.text(), x, top, f, fit.size(), c, Element.ALIGN_RIGHT);
    }

    private void drawText(PdfContentByte cb, String s, float x, float top, BaseFont f, float size,
                          Color c, int alignment) {
        cb.saveState();
        cb.beginText();
        cb.setColorFill(c);
        cb.setFontAndSize(f, size);
        cb.showTextAligned(alignment, s, x, H - top - size, 0);
        cb.endText();
        cb.restoreState();
    }

    private FitText fit(String value, BaseFont font, float preferredSize, float maxWidth) {
        String text = val(value);
        float size = preferredSize;
        while (size > MIN_TEXT_SIZE && font.getWidthPoint(text, size) > maxWidth) {
            size -= .25f;
        }
        if (font.getWidthPoint(text, size) <= maxWidth) {
            return new FitText(text, size);
        }
        String suffix = "...";
        while (!text.isEmpty() && font.getWidthPoint(text + suffix, size) > maxWidth) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return new FitText(text.isEmpty() ? "" : text + suffix, size);
    }

    private void wrappedText(PdfContentByte cb, String value, float x, float top, float maxWidth,
                             BaseFont font, float size, Color color, int maxLines) {
        String text = val(value);
        if (text.isBlank()) {
            return;
        }

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.replace('\n', ' ').trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getWidthPoint(candidate, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        int visible = Math.min(lines.size(), maxLines);
        for (int i = 0; i < visible; i++) {
            String current = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) {
                current += "...";
            }
            fitLeft(cb, current, x, top + i * 13.5f, maxWidth, font, size, color);
        }
    }

    private float y(float top, float h) {
        return H - top - h;
    }

    private String euros(BigDecimal v) {
        return DINERO.format(money(v)) + " €";
    }

    private BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private String val(String v) {
        return v == null ? "" : v.trim();
    }

    private String concepto(Factura f) {
        return val(f.getConcepto()).isBlank() ? "Limpieza de cristales" : val(f.getConcepto());
    }

    private String nombreFiscal(Cliente c) {
        if (c == null) {
            return "";
        }
        return val(c.getRazonSocial()).isBlank() ? val(c.getNombre()) : val(c.getRazonSocial());
    }

    private String normaliza(String s) {
        return val(s).toLowerCase(ES).replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o')
                .replace('ú', 'u');
    }

    private String nombreArchivo(String n) {
        return val(n).isBlank() ? "sin_numero" : n.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private record FitText(String text, float size) {
    }
}
