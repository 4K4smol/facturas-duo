package com.facturas.servicio;

import java.math.BigDecimal;
import java.nio.file.Path;

public class GeneracionFacturaResultado {
    private final String numeroFactura;
    private final String referenciaCliente;
    private final String clienteNombre;
    private final BigDecimal baseImponible;
    private final BigDecimal iva;
    private final BigDecimal total;
    private final Path archivoPdf;
    private final String mensaje;
    private final boolean correcta;

    private GeneracionFacturaResultado(
            String numeroFactura,
            String referenciaCliente,
            String clienteNombre,
            BigDecimal baseImponible,
            BigDecimal iva,
            BigDecimal total,
            Path archivoPdf,
            String mensaje,
            boolean correcta
    ) {
        this.numeroFactura = numeroFactura;
        this.referenciaCliente = referenciaCliente;
        this.clienteNombre = clienteNombre;
        this.baseImponible = baseImponible;
        this.iva = iva;
        this.total = total;
        this.archivoPdf = archivoPdf;
        this.mensaje = mensaje;
        this.correcta = correcta;
    }

    public static GeneracionFacturaResultado correcta(String numeroFactura, String nifCliente, Path archivoPdf) {
        return new GeneracionFacturaResultado(numeroFactura, nifCliente, nifCliente, null, null, null, archivoPdf, "PDF generado", true);
    }

    public static GeneracionFacturaResultado correcta(
            String numeroFactura,
            String referenciaCliente,
            String clienteNombre,
            BigDecimal baseImponible,
            BigDecimal iva,
            BigDecimal total,
            Path archivoPdf
    ) {
        return new GeneracionFacturaResultado(
                numeroFactura,
                referenciaCliente,
                clienteNombre,
                baseImponible,
                iva,
                total,
                archivoPdf,
                "PDF generado",
                true
        );
    }

    public static GeneracionFacturaResultado error(String numeroFactura, String referenciaCliente, String mensaje) {
        return new GeneracionFacturaResultado(numeroFactura, referenciaCliente, referenciaCliente, null, null, null, null, mensaje, false);
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public String getNifCliente() {
        return referenciaCliente;
    }

    public String getReferenciaCliente() {
        return referenciaCliente;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public BigDecimal getBaseImponible() {
        return baseImponible;
    }

    public BigDecimal getIva() {
        return iva;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Path getArchivoPdf() {
        return archivoPdf;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isCorrecta() {
        return correcta;
    }
}
