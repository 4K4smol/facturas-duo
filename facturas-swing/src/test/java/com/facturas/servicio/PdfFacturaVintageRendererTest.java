package com.facturas.servicio;

import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.Factura;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfFacturaVintageRendererTest {
    @Test
    void generaA4ConElEstiloVintageYSinRetenciones() throws Exception {
        Path salida = Path.of("target", "test-output");
        Files.createDirectories(salida);

        Empresa empresa = new Empresa();
        empresa.setNombre("Claudio Samano Puebla");
        empresa.setNif("72147983-L");
        empresa.setDireccion("C/ General Ceballos 14 1D");
        empresa.setCodigoPostal("39300");
        empresa.setLocalidad("Torrelavega");
        empresa.setTelefono("692160710");
        empresa.setEmail("Limpiezasduo@hotmail.com");

        Cliente cliente = new Cliente("El salon vintage", "Rocio Gonzalez Martinez", "72135557-J",
                "C/ Julian Ceballos, 42 Bajo comercial", "39300", "Torrelavega", "");
        Factura factura = new Factura();
        factura.setNumero("1");
        factura.setFecha(LocalDate.of(2024, 1, 25));
        factura.setCliente(cliente);
        factura.setConcepto("Limpieza de cristales");
        factura.setBaseImponible(new BigDecimal("22.00"));
        factura.setIva(new BigDecimal("4.62"));
        factura.setTotal(new BigDecimal("26.62"));

        Path pdf = new PdfFacturaVintageRenderer().generarPdf(factura, empresa, salida, "Metalico");
        PdfReader reader = new PdfReader(pdf.toString());
        try {
            assertEquals(1, reader.getNumberOfPages());
            assertEquals(595, Math.round(reader.getPageSize(1).getWidth()));
            assertEquals(842, Math.round(reader.getPageSize(1).getHeight()));
            String texto = new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(texto.contains("Rocio Gonzalez Martinez"));
            assertTrue(texto.contains("Limpieza de cristales"));
            assertTrue(texto.contains("TOTAL A COBRAR"));
            assertTrue(texto.contains("26,62"));
            assertFalse(texto.toLowerCase().contains("retencion"));
        } finally {
            reader.close();
        }
    }

    @Test
    void factura269ConservaImportesImportadosYFormatoNif() throws Exception {
        Empresa empresa = empresaCompleta();
        Cliente cliente = new Cliente("Cliente normal", "Cliente normal", "72135557J",
                "Calle Mayor 1", "39300", "Torrelavega", "");
        Factura factura = factura("269", cliente, "Texto importado que no debe aparecer", "21.03", "99.99", "1.00");

        String texto = textoPdf(new PdfFacturaVintageRenderer().generarPdf(
                factura, empresa, Path.of("target", "test-output"), "Metálico"));

        assertTrue(texto.contains("Limpieza de cristales"));
        assertFalse(texto.contains("Texto importado"));
        assertTrue(texto.contains("72135557-J"));
        assertTrue(texto.contains("IVA 21%"));
        assertTrue(texto.contains("21,03 €"));
        assertTrue(texto.contains("99,99 €"));
        assertTrue(texto.contains("1,00 €"));
        assertFalse(texto.toLowerCase().contains("retencion"));
    }

    @Test
    void portalUsaUnicaLineaDeLimpiezaDeComunidad() throws Exception {
        Cliente cliente = new Cliente("Portal La Luz", "Comunidad de propietarios La Luz", "A12345678",
                "Calle Mayor 2", "39300", "Torrelavega", "");
        Factura factura = factura("270", cliente, "Cualquier descripción", "100", "21", "121");

        String texto = textoPdf(new PdfFacturaVintageRenderer().generarPdf(
                factura, empresaCompleta(), Path.of("target", "test-output"), "Cargo a cuenta"));

        assertTrue(texto.contains("Limpieza de comunidad"));
        assertFalse(texto.contains("Limpieza de cristales"));
    }

    @Test
    void formateaNifSinDuplicarGuion() {
        assertEquals("72135557-J", PdfFacturaVintageRenderer.formatearNif(" 72135557J "));
        assertEquals("72135557-J", PdfFacturaVintageRenderer.formatearNif("72135557-J"));
        assertEquals("A12345678", PdfFacturaVintageRenderer.formatearNif(" A12345678 "));
    }

    @Test
    void eliminaRectanguloYSombraDelLogoSinBorrarSuInterior() {
        BufferedImage logo = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 9; x++) {
                logo.setRGB(x, y, Color.WHITE.getRGB());
            }
        }
        for (int y = 2; y <= 7; y++) {
            for (int x = 2; x <= 7; x++) {
                logo.setRGB(x, y, new Color(170, 170, 170).getRGB());
            }
        }
        for (int y = 2; y <= 5; y++) {
            for (int x = 2; x <= 5; x++) {
                logo.setRGB(x, y, Color.BLACK.getRGB());
            }
        }
        logo.setRGB(3, 3, Color.WHITE.getRGB());

        BufferedImage limpio = PdfFacturaVintageRenderer.limpiarFondoLogo(logo);

        assertEquals(0, limpio.getRGB(0, 0) >>> 24);
        assertEquals(0, limpio.getRGB(7, 7) >>> 24);
        assertEquals(255, limpio.getRGB(2, 2) >>> 24);
        assertEquals(255, limpio.getRGB(3, 3) >>> 24);
    }

    private Empresa empresaCompleta() {
        Empresa empresa = new Empresa();
        empresa.setNombre("Claudio Samano Puebla");
        empresa.setNif("72147983-L");
        empresa.setDireccion("C/ General Ceballos 14 1D");
        empresa.setCodigoPostal("39300");
        empresa.setLocalidad("Torrelavega");
        empresa.setTelefono("692160710");
        empresa.setEmail("Limpiezasduo@hotmail.com");
        return empresa;
    }

    private Factura factura(String numero, Cliente cliente, String concepto, String base, String iva, String total) {
        Factura factura = new Factura();
        factura.setNumero(numero);
        factura.setFecha(LocalDate.of(2024, 1, 25));
        factura.setCliente(cliente);
        factura.setConcepto(concepto);
        factura.setBaseImponible(new BigDecimal(base));
        factura.setIva(new BigDecimal(iva));
        factura.setTotal(new BigDecimal(total));
        return factura;
    }

    private String textoPdf(Path pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf.toString());
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }
}
