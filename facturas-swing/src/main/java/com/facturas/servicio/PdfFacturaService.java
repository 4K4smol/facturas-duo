package com.facturas.servicio;

import com.facturas.modelo.Empresa;
import com.facturas.modelo.Factura;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PdfFacturaService {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font CABECERA = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font BRAND = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(255, 193, 7));

    public Path generarPdf(Factura factura, Empresa empresa, Path carpetaSalida) throws IOException, DocumentException {
        return generarPdf(factura, empresa, carpetaSalida, "Transferencia");
    }

    public Path generarPdf(Factura factura, Empresa empresa, Path carpetaSalida, String formaPago)
            throws IOException, DocumentException {
        Files.createDirectories(carpetaSalida);
        String nombreArchivo = "factura_" + limpiarNombreArchivo(factura.getNumero()) + ".pdf";
        Path archivoSalida = carpetaSalida.resolve(nombreArchivo);

        Document documento = new Document(PageSize.A4, 40, 40, 42, 42);
        PdfWriter.getInstance(documento, new FileOutputStream(archivoSalida.toFile()));
        documento.open();

        documento.add(titulo("FACTURA"));
        documento.add(espacio(8));
        documento.add(seccionEmpresa(empresa));
        documento.add(espacio(8));
        documento.add(franjaFactura(factura, empresa));
        documento.add(espacio(8));
        documento.add(seccionCliente(factura));
        documento.add(espacio(8));
        documento.add(seccionConcepto(factura));
        documento.add(espacio(8));
        documento.add(seccionImporte(factura));
        documento.add(espacio(10));
        documento.add(parrafo("Tipo de pago: " + valor(formaPago)));

        documento.close();
        return archivoSalida;
    }

    private Paragraph titulo(String texto) {
        Paragraph paragraph = new Paragraph(texto, TITULO);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private Paragraph parrafo(String texto) {
        return new Paragraph(texto, NORMAL);
    }

    private Paragraph espacio(int puntos) {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(puntos);
        return paragraph;
    }

    private PdfPTable seccionEmpresa(Empresa empresa) throws IOException, DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] { 65, 35 });
        PdfPCell datos = celdaBorde();
        datos.addElement(new Phrase(valor(empresa.getNombre()), CABECERA));
        datos.addElement(new Phrase("DNI/NIF/CIF: " + valor(empresa.getNif()), NORMAL));
        datos.addElement(new Phrase(valor(empresa.getDireccion()), NORMAL));
        datos.addElement(new Phrase(valor(empresa.getCodigoPostal()) + " " + valor(empresa.getLocalidad()), NORMAL));
        datos.addElement(new Phrase("Telefono: " + valor(empresa.getTelefono()), NORMAL));
        datos.addElement(new Phrase("Email: " + valor(empresa.getEmail()), NORMAL));

        PdfPCell logo = celdaBorde();
        logo.setHorizontalAlignment(Element.ALIGN_CENTER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (empresa.getLogoPath() != null && new File(empresa.getLogoPath()).isFile()) {
            Image image = Image.getInstance(empresa.getLogoPath());
            image.scaleToFit(150, 90);
            logo.addElement(image);
        } else {
            // show brand-styled name when no logo is provided
            Paragraph p = new Paragraph(valor(empresa.getNombre()), BRAND);
            p.setAlignment(Element.ALIGN_RIGHT);
            logo.addElement(p);
        }

        tabla.addCell(datos);
        tabla.addCell(logo);
        return tabla;
    }

    private PdfPTable franjaFactura(Factura factura, Empresa empresa) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        PdfPCell izquierda = celdaTexto(valor(empresa.getLocalidad()) + ", " + factura.getFecha().format(FORMATO_FECHA),
                NORMAL);
        izquierda.setBorderWidth(1f);
        PdfPCell derecha = celdaTexto("Nº Factura: " + valor(factura.getNumero()), NEGRITA);
        derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
        derecha.setBorderWidth(1f);
        tabla.addCell(izquierda);
        tabla.addCell(derecha);
        return tabla;
    }

    private PdfPTable seccionCliente(Factura factura) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        PdfPCell celda = celdaBorde();
        celda.addElement(new Phrase("Cliente", CABECERA));
        celda.addElement(new Phrase("Nombre comercial: " + valor(factura.getCliente().getNombre()), NORMAL));
        celda.addElement(new Phrase("Nombre / razon social: " + nombreFiscal(factura), NORMAL));
        celda.addElement(new Phrase("CIF/NIF: " + valor(factura.getCliente().getNif()), NORMAL));
        celda.addElement(new Phrase(valor(factura.getCliente().getDireccion()), NORMAL));
        celda.addElement(new Phrase(
                valor(factura.getCliente().getCodigoPostal()) + " " + valor(factura.getCliente().getLocalidad()),
                NORMAL));
        if (factura.getCliente().getTelefono() != null && !factura.getCliente().getTelefono().isBlank()) {
            celda.addElement(new Phrase("Telefono: " + factura.getCliente().getTelefono(), NORMAL));
        }
        tabla.addCell(celda);
        return tabla;
    }

    private PdfPTable seccionConcepto(Factura factura) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[] { 75f, 25f });
        tabla.addCell(celdaCabecera("Trabajo"));
        tabla.addCell(celdaCabecera("Precio (sin IVA)"));

        // concepto cell with larger minimum height to create space like the sample
        PdfPCell trabajo = new PdfPCell(new Phrase(concepto(factura), NORMAL));
        trabajo.setPadding(12);
        trabajo.setBorderWidth(1f);
        trabajo.setMinimumHeight(120f);
        tabla.addCell(trabajo);

        PdfPCell precio = celdaTexto(moneda(factura.getBaseImponible()), NORMAL);
        precio.setVerticalAlignment(Element.ALIGN_MIDDLE);
        precio.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(precio);

        // total row
        PdfPCell etiquetaTotal = celdaTexto("Total", NEGRITA);
        etiquetaTotal.setBorderWidthTop(0f);
        etiquetaTotal.setBorderWidthBottom(1f);
        etiquetaTotal.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell valorTotal = celdaTexto(moneda(factura.getBaseImponible()), NEGRITA);
        valorTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabla.addCell(etiquetaTotal);
        tabla.addCell(valorTotal);
        return tabla;
    }

    private PdfPTable seccionImporte(Factura factura) {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.addCell(celdaCabecera("Concepto"));
        tabla.addCell(celdaCabecera("Total"));
        tabla.addCell(celdaTexto("Base imponible", NORMAL));
        tabla.addCell(celdaTexto(moneda(factura.getBaseImponible()), NORMAL));
        tabla.addCell(celdaTexto("IVA 21%", NORMAL));
        tabla.addCell(celdaTexto(moneda(factura.getIva()), NORMAL));
        tabla.addCell(celdaTotal("TOTAL A COBRAR"));
        tabla.addCell(celdaTotal(moneda(factura.getTotal())));
        return tabla;
    }

    private PdfPCell celdaBorde() {
        PdfPCell celda = new PdfPCell();
        celda.setPadding(9);
        celda.setBorderWidth(1f);
        return celda;
    }

    private PdfPCell celdaCabecera(String texto) {
        PdfPCell celda = celdaTexto(texto, NEGRITA);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        return celda;
    }

    private PdfPCell celdaTexto(String texto, Font font) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, font));
        celda.setPadding(8);
        celda.setBorderWidth(1f);
        return celda;
    }

    private PdfPCell celdaTotal(String texto) {
        PdfPCell celda = celdaTexto(texto, NEGRITA);
        celda.setBackgroundColor(new Color(235, 235, 235));
        return celda;
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private String nombreFiscal(Factura factura) {
        String razonSocial = factura.getCliente().getRazonSocial();
        return razonSocial == null || razonSocial.isBlank() ? valor(factura.getCliente().getNombre()) : razonSocial;
    }

    private String concepto(Factura factura) {
        String concepto = factura.getConcepto();
        return concepto == null || concepto.isBlank() ? "Limpieza de cristales" : concepto;
    }

    private String moneda(BigDecimal cantidad) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));
        return format.format(cantidad == null ? BigDecimal.ZERO : cantidad);
    }

    private String limpiarNombreArchivo(String nombre) {
        return nombre == null || nombre.isBlank() ? "sin_numero" : nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
