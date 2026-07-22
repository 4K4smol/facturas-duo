package com.facturas.servicio;

import com.facturas.modelo.FacturaExcel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExcelServiceTest {
    @TempDir
    Path temporal;

    private final ExcelService service = new ExcelService();

    @Test
    void importaCsvConPuntoYComaSinRecalcularImportes() throws Exception {
        Path csv = temporal.resolve("facturas.csv");
        Files.writeString(csv, "\uFEFFnumero;cliente;base_imponible;iva;total;concepto;fecha;forma_pago\n"
                + "1;El salon vintage;22,00;4,62;25,08;Limpieza de cristales;25/01/2024;Metalico\n",
                StandardCharsets.UTF_8);

        List<FacturaExcel> facturas = service.leerFacturas(csv.toFile());

        assertEquals(1, facturas.size());
        FacturaExcel factura = facturas.get(0);
        assertEquals("1", factura.getNumero());
        assertEquals("El salon vintage", factura.getNombreCliente());
        assertEquals(new BigDecimal("22.00"), factura.getBaseImponible());
        assertEquals(new BigDecimal("4.62"), factura.getIva());
        assertEquals(new BigDecimal("25.08"), factura.getTotalConIva());
        assertEquals(LocalDate.of(2024, 1, 25), factura.getFecha());
    }

    @Test
    void importaCsvConComasCamposEntreComillasYColumnasEnOtroOrden() throws Exception {
        Path csv = temporal.resolve("facturas-comas.csv");
        Files.writeString(csv, "cliente,numero,descripcion,total,base,iva\n"
                + "\"Bar, Restaurante y Mas\",2,\"Limpieza, cristales y terraza\",121.00,100.00,21.00\n",
                StandardCharsets.UTF_8);

        List<FacturaExcel> facturas = service.leerFacturas(csv.toFile());

        assertEquals(1, facturas.size());
        FacturaExcel factura = facturas.get(0);
        assertEquals("Bar, Restaurante y Mas", factura.getNombreCliente());
        assertEquals("Limpieza, cristales y terraza", factura.getConcepto());
        assertEquals(new BigDecimal("121.00"), factura.getTotalConIva());
    }

    @Test
    void noCalculaIvaSiElCsvNoLoIncluye() throws Exception {
        Path csv = temporal.resolve("sin-iva.csv");
        Files.writeString(csv, "numero;cliente;base;total\n3;Cliente prueba;50,00;46,50\n", StandardCharsets.UTF_8);

        FacturaExcel factura = service.leerFacturas(csv.toFile()).get(0);

        assertNull(factura.getIva());
        assertEquals(new BigDecimal("46.50"), factura.getTotalConIva());
    }

    @Test
    void importaBaseIvaYTotalDeLasColumnasDeHoja3() throws Exception {
        Path excel = temporal.resolve("facturas.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var hoja = workbook.createSheet("Hoja3");
            var cabecera = hoja.createRow(0);
            cabecera.createCell(0).setCellValue("N");
            cabecera.createCell(1).setCellValue("Cliente");
            cabecera.createCell(2).setCellValue("Base imponible");
            cabecera.createCell(3).setCellValue("IVA");
            cabecera.createCell(4).setCellValue("Tarifa");
            cabecera.createCell(5).setCellValue("Total");

            var fila = hoja.createRow(1);
            fila.createCell(0).setCellValue("269");
            fila.createCell(1).setCellValue("El salon vintage");
            fila.createCell(2).setCellValue("21,03 €");
            fila.createCell(3).setCellValue("4,42 €");
            fila.createCell(4).setCellValue("99,99 €");
            fila.createCell(5).setCellValue("25,45 €");

            try (OutputStream salida = Files.newOutputStream(excel)) {
                workbook.write(salida);
            }
        }

        FacturaExcel factura = service.leerFacturas(excel.toFile()).get(0);

        assertEquals(new BigDecimal("21.03"), factura.getBaseImponible());
        assertEquals(new BigDecimal("4.42"), factura.getIva());
        assertEquals(new BigDecimal("25.45"), factura.getTotalConIva());
        assertEquals("Limpieza de cristales", factura.getConcepto());
    }
}
