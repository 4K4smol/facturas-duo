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
import java.awt.image.BufferedImage;
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
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Renderiza la factura con la misma distribución del modelo "vintage".
 *
 * La retención se ignora deliberadamente: el total se calcula siempre como
 * base imponible + IVA, aunque Factura#getTotal() contenga un valor antiguo
 * calculado con retención.
 */
public class PdfFacturaVintageRenderer extends PdfFacturaService {
    private static final BigDecimal TIPO_IVA = new BigDecimal("0.21");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = Locale.forLanguageTag("es-ES");
    private static final DecimalFormat DINERO = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(ES));

    private static final float H = PageSize.A4.getHeight();
    private static final float MIN_TEXT_SIZE = 8f;
    private static final float CHECKBOX_SIZE = 6.2f;

    private static final float FONT_EMPRESA = 12f;
    private static final float FONT_CUERPO = 11.04f;
    private static final float FONT_TITULO_SECCION = 12.96f;
    private static final float FONT_MARCA = 15.96f;

    private static final Color BLACK = Color.BLACK;
    private static final Color BLUE = new Color(0, 0, 255);
    private static final Color GOLD = new Color(255, 192, 0);
    private static final Color GRAY = new Color(191, 191, 191);

    private BaseFont regular;
    private BaseFont bold;

    @Override
    public Path generarPdf(Factura factura, Empresa empresa, Path carpetaSalida, String formaPago)
            throws IOException, DocumentException {
        validar(factura, empresa);

        Files.createDirectories(carpetaSalida);
        Path salida = carpetaSalida.resolve("factura_" + nombreArchivo(factura.getNumero()) + ".pdf");

        Document documento = new Document(PageSize.A4, 0, 0, 0, 0);
        try (FileOutputStream out = new FileOutputStream(salida.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(documento, out);
            documento.open();
            try {
                regular = cargarFuente(false);
                bold = cargarFuente(true);

                PdfContentByte cb = writer.getDirectContent();
                fondos(cb);
                lineas(cb);
                textos(cb, factura, empresa);
                logo(cb, empresa);
            } finally {
                if (documento.isOpen()) {
                    documento.close();
                }
            }
        }

        return salida;
    }

    private BaseFont cargarFuente(boolean negrita) throws IOException, DocumentException {
        String[] rutas = negrita
                ? new String[] {
                        "C:/Windows/Fonts/calibrib.ttf",
                        "C:/Windows/Fonts/CALIBRIB.TTF",
                        "/usr/share/fonts/truetype/crosextra/Caladea-Bold.ttf"
                }
                : new String[] {
                        "C:/Windows/Fonts/calibri.ttf",
                        "C:/Windows/Fonts/CALIBRI.TTF",
                        "/usr/share/fonts/truetype/crosextra/Caladea-Regular.ttf"
                };

        for (String ruta : rutas) {
            if (new File(ruta).isFile()) {
                return BaseFont.createFont(ruta, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }

        return BaseFont.createFont(
                negrita ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA,
                BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED);
    }

    private void fondos(PdfContentByte cb) {
        rect(cb, 0f, 0f, PageSize.A4.getWidth(), PageSize.A4.getHeight(), Color.WHITE);

        // Fila de total desplazada una línea hacia arriba al eliminar la retención.
        rect(cb, 129.00f, 673.32f, 331.20f, 14.64f, GRAY);
        rect(cb, 192.60f, 716.88f, 267.60f, 14.64f, GRAY);
    }

    private void lineas(PdfContentByte cb) {
        float[][] black = {
                // Subrayados del documento original.
                { 131.16f, 285.60f, 99.12f, .84f },
                { 130.92f, 315.24f, 40.56f, .72f },
                { 130.92f, 329.76f, 16.92f, .72f },
                { 130.92f, 344.28f, 46.08f, .72f },
                { 130.92f, 358.80f, 14.76f, .72f },
                { 131.16f, 420.36f, 62.88f, .84f },
                { 131.16f, 627.24f, 54.00f, .84f },

                // Cajas exteriores.
                { 102.24f, 53.76f, 1.92f, 144.36f },
                { 525.24f, 55.20f, 1.92f, 142.92f },
                { 104.16f, 53.76f, 423.00f, 1.44f },
                { 104.16f, 196.20f, 423.00f, 1.92f },

                { 102.24f, 211.44f, 1.92f, 31.68f },
                { 525.24f, 213.36f, 1.92f, 29.76f },
                { 104.16f, 211.44f, 423.00f, 1.92f },
                { 104.16f, 241.20f, 423.00f, 1.92f },

                { 102.24f, 256.44f, 1.92f, 120.96f },
                { 525.24f, 258.36f, 1.92f, 119.04f },
                { 104.16f, 256.44f, 423.00f, 1.92f },
                { 104.16f, 375.48f, 423.00f, 1.92f },

                { 102.24f, 390.72f, 1.92f, 193.56f },
                { 525.24f, 392.64f, 1.92f, 191.64f },
                { 104.16f, 390.72f, 423.00f, 1.92f },
                { 104.16f, 582.36f, 423.00f, 1.92f },
                { 354.00f, 422.88f, .96f, 145.32f },
                { 129.00f, 436.92f, 331.20f, .96f },
                { 192.60f, 553.08f, 267.60f, .96f },

                { 102.24f, 597.60f, 1.92f, 150.00f },
                { 525.24f, 599.52f, 1.92f, 148.08f },
                { 104.16f, 597.60f, 423.00f, 1.92f },
                { 104.16f, 745.68f, 423.00f, 1.92f },

                // Importe sin fila de retención.
                { 354.00f, 629.76f, .96f, 58.20f },
                { 129.00f, 643.80f, 331.20f, .96f },
                { 129.00f, 672.84f, 331.20f, .96f },

                // Caja del tipo de pago.
                { 192.12f, 716.40f, .96f, 15.48f },
                { 459.60f, 717.36f, .96f, 14.52f },
                { 193.08f, 716.40f, 267.48f, .96f },
                { 193.08f, 730.92f, 267.48f, .96f }
        };

        for (float[] r : black) {
            rect(cb, r[0], r[1], r[2], r[3], BLACK);
        }

        rect(cb, 130.92f, 179.52f, 127.80f, .72f, BLUE);
        rect(cb, 388.20f, 71.16f, 105.24f, 1.08f, GOLD);
    }

    private void textos(PdfContentByte cb, Factura f, Empresa e) {
        Cliente c = f.getCliente();
        BigDecimal base = money(f.getBaseImponible());
        BigDecimal iva = money(base.multiply(TIPO_IVA));

        // No se usa f.getTotal(): así nunca se arrastra una retención antigua.
        BigDecimal totalSinRetencion = money(base.add(iva));

        fitLeft(cb, val(e.getNombre()).toUpperCase(ES), 131.04f, 93.84f,
                220f, bold, FONT_EMPRESA, BLACK);
        fitLeft(cb, val(e.getNif()), 131.04f, 109.08f,
                220f, bold, FONT_EMPRESA, BLACK);
        fitLeft(cb, val(e.getDireccion()), 131.04f, 124.32f,
                220f, bold, FONT_EMPRESA, BLACK);
        fitLeft(cb, (val(e.getCodigoPostal()) + " " + val(e.getLocalidad())).trim(),
                131.04f, 139.56f, 220f, bold, FONT_EMPRESA, BLACK);
        fitLeft(cb, val(e.getTelefono()), 135.48f, 154.80f,
                215f, bold, FONT_EMPRESA, BLACK);
        fitLeft(cb, val(e.getEmail()), 130.92f, 170.03f,
                220f, regular, FONT_CUERPO, BLUE);

        // En el vintage el texto de marca y el isotipo son elementos separados.
        fitLeft(cb, "LIMPIEZAS DÚO", 388.21f, 57.38f,
                105.24f, bold, FONT_MARCA, GOLD);

        LocalDate fecha = f.getFecha() == null ? LocalDate.now() : f.getFecha();
        fitRight(cb,
                (val(e.getLocalidad()) + ", " + fecha.format(FECHA)).replaceFirst("^,\\s*", ""),
                516.65f, 215.15f, 300f, bold, FONT_CUERPO, BLACK);
        fitLeft(cb, "Nº Factura: " + val(f.getNumero()),
                406.33f, 230.03f, 110f, bold, FONT_CUERPO, BLACK);

        text(cb, "Nombre comercial", 131.17f, 274.43f,
                bold, FONT_TITULO_SECCION, BLACK);
        fitLeft(cb, val(c == null ? "" : c.getNombre()),
                194.52f, 290.88f, 305f, bold, FONT_CUERPO, BLACK);

        text(cb, "Nombre:", 130.92f, 305.76f, bold, FONT_CUERPO, BLACK);
        nombreFiscalEnDosColumnas(cb, nombreFiscal(c));

        text(cb, "CIF:", 130.92f, 320.27f, bold, FONT_CUERPO, BLACK);
        fitLeft(cb, formatearNif(c == null ? "" : c.getNif()),
                194.52f, 320.27f, 305f, regular, FONT_CUERPO, BLACK);

        text(cb, "Dirección:", 130.92f, 334.79f, bold, FONT_CUERPO, BLACK);
        fitLeft(cb, val(c == null ? "" : c.getDireccion()),
                194.52f, 334.79f, 305f, regular, FONT_CUERPO, BLACK);

        text(cb, "CP:", 130.92f, 349.31f, bold, FONT_CUERPO, BLACK);
        fitLeft(cb,
                (val(c == null ? "" : c.getCodigoPostal()) + ", "
                        + val(c == null ? "" : c.getLocalidad())).replaceFirst("^,\\s*", ""),
                194.52f, 349.31f, 305f, regular, FONT_CUERPO, BLACK);

        text(cb, "CONCEPTO:", 131.17f, 409.18f,
                bold, FONT_TITULO_SECCION, BLACK);
        text(cb, "Trabajo", 224.76f, 425.15f,
                bold, FONT_CUERPO, BLACK);
        text(cb, "Precio (sin IVA)", 388.21f, 425.63f,
                bold, FONT_CUERPO, BLACK);

        fitRight(cb, concepto(f), 352.52f, 454.68f,
                194f, regular, FONT_CUERPO, BLACK);
        fitRight(cb, euros(base), 458.13f, 454.55f,
                93f, regular, FONT_CUERPO, BLACK);

        text(cb, "Total", 329.17f, 556.31f,
                bold, FONT_CUERPO, BLACK);
        fitRight(cb, euros(base), 458.13f, 556.31f,
                93f, regular, FONT_CUERPO, BLACK);

        text(cb, "IMPORTE:", 131.17f, 616.07f,
                bold, FONT_TITULO_SECCION, BLACK);
        fitRight(cb, "Total", 458.17f, 632.51f,
                93f, bold, FONT_CUERPO, BLACK);

        text(cb, "Base imponible", 130.92f, 647.04f,
                regular, FONT_CUERPO, BLACK);
        fitRight(cb, euros(base), 458.13f, 647.04f,
                93f, regular, FONT_CUERPO, BLACK);

        text(cb, "IVA 21%", 130.92f, 661.55f,
                regular, FONT_CUERPO, BLACK);
        fitRight(cb, euros(iva), 458.13f, 661.55f,
                93f, regular, FONT_CUERPO, BLACK);

        // La fila de retenciones se elimina por completo.
        text(cb, "TOTAL A COBRAR", 130.92f, 675.59f,
                bold, FONT_CUERPO, BLACK);
        fitRight(cb, euros(totalSinRetencion), 458.13f, 676.07f,
                93f, bold, FONT_CUERPO, BLACK);

        text(cb, "Tipo de pago:", 194.52f, 719.64f,
                bold, FONT_CUERPO, BLACK);
        text(cb, "Metálico", 291.96f, 719.64f,
                bold, FONT_CUERPO, BLACK);
        checkBox(cb, 335.15f, 721.45f);

        fitLeft(cb, "Cargo a cuenta", 380.89f, 719.64f,
                68f, bold, FONT_CUERPO, BLACK);
        checkBox(cb, 451.55f, 721.45f);
    }

    private void nombreFiscalEnDosColumnas(PdfContentByte cb, String nombreCompleto) {
        String nombre = val(nombreCompleto);

        if (nombre.isBlank()) {
            return;
        }

        int separador = nombre.indexOf(' ');
        if (separador < 0) {
            fitLeft(cb, nombre, 194.52f, 305.76f,
                    305f, regular, FONT_CUERPO, BLACK);
            return;
        }

        String primerNombre = nombre.substring(0, separador).trim();
        String resto = nombre.substring(separador + 1).trim();

        fitLeft(cb, primerNombre, 194.52f, 305.76f,
                88f, regular, FONT_CUERPO, BLACK);
        fitLeft(cb, resto, 291.96f, 305.76f,
                207f, regular, FONT_CUERPO, BLACK);
    }

    private void logo(PdfContentByte cb, Empresa e) throws IOException, DocumentException {
        if (!tieneLogo(e)) {
            return;
        }

        Image logo = logoSinFondo(e.getLogoPath());
        logo.scaleAbsolute(120.24f, 109.44f);
        logo.setAbsolutePosition(382.44f, y(72.72f, 109.44f));
        cb.addImage(logo);
    }

    private Image logoSinFondo(String ruta) throws IOException, DocumentException {
        BufferedImage original = ImageIO.read(new File(ruta));
        if (original == null) {
            return Image.getInstance(ruta);
        }

        return Image.getInstance(limpiarFondoLogo(original), null, false);
    }

    /**
     * Hace transparente únicamente el fondo neutro conectado al borde. De este
     * modo desaparecen tanto el rectángulo como su sombra gris, sin borrar blancos
     * que formen parte del propio logotipo.
     */
    static BufferedImage limpiarFondoLogo(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        BufferedImage resultado = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        boolean[][] visitado = new boolean[alto][ancho];
        ArrayDeque<Integer> pendientes = new ArrayDeque<>();

        for (int x = 0; x < ancho; x++) {
            encolarFondo(original, x, 0, visitado, pendientes);
            encolarFondo(original, x, alto - 1, visitado, pendientes);
        }
        for (int y = 1; y < alto - 1; y++) {
            encolarFondo(original, 0, y, visitado, pendientes);
            encolarFondo(original, ancho - 1, y, visitado, pendientes);
        }

        while (!pendientes.isEmpty()) {
            int posicion = pendientes.removeFirst();
            int x = posicion % ancho;
            int y = posicion / ancho;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    encolarFondo(original, x + dx, y + dy, visitado, pendientes);
                }
            }
        }

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int argb = original.getRGB(x, y);
                resultado.setRGB(x, y, visitado[y][x] ? argb & 0x00ffffff : argb);
            }
        }
        return resultado;
    }

    private static void encolarFondo(BufferedImage imagen, int x, int y,
            boolean[][] visitado, ArrayDeque<Integer> pendientes) {
        if (x < 0 || y < 0 || x >= imagen.getWidth() || y >= imagen.getHeight()
                || visitado[y][x] || !esFondoOSombra(imagen.getRGB(x, y))) {
            return;
        }
        visitado[y][x] = true;
        pendientes.addLast(y * imagen.getWidth() + x);
    }

    private static boolean esFondoOSombra(int argb) {
        int alfa = (argb >>> 24) & 0xff;
        int rojo = (argb >>> 16) & 0xff;
        int verde = (argb >>> 8) & 0xff;
        int azul = argb & 0xff;
        int minimo = Math.min(rojo, Math.min(verde, azul));
        int maximo = Math.max(rojo, Math.max(verde, azul));
        return alfa == 0 || (minimo >= 135 && maximo - minimo <= 28);
    }

    private boolean tieneLogo(Empresa e) {
        return e != null && e.getLogoPath() != null && new File(e.getLogoPath()).isFile();
    }

    private void rect(PdfContentByte cb, float x, float top, float w, float h, Color color) {
        cb.saveState();
        cb.setColorFill(color);
        cb.rectangle(x, y(top, h), w, h);
        cb.fill();
        cb.restoreState();
    }

    private void checkBox(PdfContentByte cb, float x, float top) {
        float bottom = y(top, CHECKBOX_SIZE);
        cb.saveState();
        cb.setColorStroke(BLACK);
        cb.setLineWidth(.7f);
        cb.rectangle(x, bottom, CHECKBOX_SIZE, CHECKBOX_SIZE);
        cb.stroke();
        cb.restoreState();
    }

    private void text(PdfContentByte cb, String value, float x, float top,
            BaseFont font, float size, Color color) {
        drawText(cb, val(value), x, top, font, size, color, Element.ALIGN_LEFT);
    }

    private void fitLeft(PdfContentByte cb, String value, float x, float top, float maxWidth,
            BaseFont font, float size, Color color) {
        FitText fit = fit(value, font, size, maxWidth);
        drawText(cb, fit.text(), x, top, font, fit.size(), color, Element.ALIGN_LEFT);
    }

    private void fitRight(PdfContentByte cb, String value, float x, float top, float maxWidth,
            BaseFont font, float size, Color color) {
        FitText fit = fit(value, font, size, maxWidth);
        drawText(cb, fit.text(), x, top, font, fit.size(), color, Element.ALIGN_RIGHT);
    }

    private void drawText(PdfContentByte cb, String value, float x, float top,
            BaseFont font, float size, Color color, int alignment) {
        float ascent = font.getFontDescriptor(BaseFont.ASCENT, size);
        float baseline = H - top - ascent;

        cb.saveState();
        cb.beginText();
        cb.setColorFill(color);
        cb.setFontAndSize(font, size);
        cb.showTextAligned(alignment, value, x, baseline, 0);
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

    private void wrappedTextRight(PdfContentByte cb, String value, float rightX, float top,
            float maxWidth, BaseFont font, float size, Color color,
            int maxLines) {
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
            fitRight(cb, current, rightX, top + i * 13.5f,
                    maxWidth, font, size, color);
        }
    }

    private float y(float top, float height) {
        return H - top - height;
    }

    private String euros(BigDecimal value) {
        return DINERO.format(money(value)) + " €";
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String val(String value) {
        return value == null ? "" : value.trim();
    }

    private String concepto(Factura factura) {
        String indicador = normaliza(val(factura.getConcepto()) + " "
                + val(factura.getCliente() == null ? "" : factura.getCliente().getNombre()) + " "
                + val(factura.getCliente() == null ? "" : factura.getCliente().getRazonSocial()));
        return indicador.matches(".*\\b(portal|comunidad|comunidades)\\b.*")
                ? "Limpieza de comunidad"
                : "Limpieza de cristales";
    }

    /** Conserva los NIF ya formateados y añade el guion a un DNI de 8 cifras. */
    static String formatearNif(String nif) {
        String limpio = nif == null ? "" : nif.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (limpio.matches("\\d{8}-?[A-Z]")) {
            String sinGuion = limpio.replace("-", "");
            return sinGuion.substring(0, 8) + "-" + sinGuion.substring(8);
        }
        return limpio;
    }

    private void validar(Factura factura, Empresa empresa) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura no puede ser nula");
        }
        if (empresa == null) {
            throw new IllegalArgumentException("La empresa no puede ser nula");
        }
        if (factura.getCliente() == null) {
            throw new IllegalArgumentException("La factura debe tener un cliente");
        }
        if (factura.getBaseImponible() == null || factura.getBaseImponible().signum() <= 0) {
            throw new IllegalArgumentException("La base imponible debe ser válida y mayor que cero");
        }
        // El concepto definitivo se obtiene antes de dibujar y nunca puede quedar vacío.
        if (concepto(factura).isBlank()) {
            throw new IllegalArgumentException("El concepto del trabajo no puede estar vacío");
        }
        validarDatoEmpresa("nombre", empresa.getNombre());
        validarDatoEmpresa("nif", empresa.getNif());
        validarDatoEmpresa("direccion", empresa.getDireccion());
        validarDatoEmpresa("codigoPostal", empresa.getCodigoPostal());
        validarDatoEmpresa("localidad", empresa.getLocalidad());
        validarDatoEmpresa("telefono", empresa.getTelefono());
        validarDatoEmpresa("email", empresa.getEmail());
    }

    private void validarDatoEmpresa(String campo, String valor) {
        if (val(valor).isBlank()) {
            throw new IllegalArgumentException("Falta el dato del emisor: " + campo);
        }
    }

    private String nombreFiscal(Cliente cliente) {
        if (cliente == null) {
            return "";
        }

        return val(cliente.getRazonSocial()).isBlank()
                ? val(cliente.getNombre())
                : val(cliente.getRazonSocial());
    }

    private String normaliza(String value) {
        return Normalizer.normalize(val(value).toLowerCase(ES), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private String nombreArchivo(String numero) {
        return val(numero).isBlank()
                ? "sin_numero"
                : numero.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private record FitText(String text, float size) {
    }
}
