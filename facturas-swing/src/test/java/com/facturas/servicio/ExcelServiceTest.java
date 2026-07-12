package com.facturas.servicio;

import com.facturas.modelo.FacturaExcel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelServiceTest {
    @TempDir
    Path temporal;

    private final ExcelService service = new ExcelService();

    @Test
    void importaCsvConPuntoYComaYRecalculaElTotalSinRetencion() throws Exception {
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
        assertEquals(new BigDecimal("26.62"), factura.getTotalConIva());
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
    void calculaIvaDelVeintiunoPorCientoSiElCsvNoLoIncluye() throws Exception {
        Path csv = temporal.resolve("sin-iva.csv");
        Files.writeString(csv, "numero;cliente;base;total\n3;Cliente prueba;50,00;46,50\n", StandardCharsets.UTF_8);

        FacturaExcel factura = service.leerFacturas(csv.toFile()).get(0);

        assertEquals(new BigDecimal("10.50"), factura.getIva());
        assertEquals(new BigDecimal("60.50"), factura.getTotalConIva());
    }
}
