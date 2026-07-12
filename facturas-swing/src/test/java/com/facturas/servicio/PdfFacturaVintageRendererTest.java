package com.facturas.servicio;

import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.Factura;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

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
}
