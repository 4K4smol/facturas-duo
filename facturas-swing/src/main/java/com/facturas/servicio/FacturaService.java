package com.facturas.servicio;

import com.facturas.dao.ClienteDAO;
import com.facturas.dao.EmpresaDAO;
import com.facturas.dao.FacturaDAO;
import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.Factura;
import com.facturas.modelo.FacturaExcel;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class FacturaService {
    private final ExcelService excelService;
    private final PdfFacturaService pdfFacturaService;
    private final ClienteDAO clienteDAO;
    private final EmpresaDAO empresaDAO;
    private final FacturaDAO facturaDAO;

    public FacturaService() {
        this(new ExcelService(), new PdfFacturaService(), new ClienteDAO(), new EmpresaDAO(), new FacturaDAO());
    }

    public FacturaService(
            ExcelService excelService,
            PdfFacturaService pdfFacturaService,
            ClienteDAO clienteDAO,
            EmpresaDAO empresaDAO,
            FacturaDAO facturaDAO
    ) {
        this.excelService = excelService;
        this.pdfFacturaService = pdfFacturaService;
        this.clienteDAO = clienteDAO;
        this.empresaDAO = empresaDAO;
        this.facturaDAO = facturaDAO;
    }

    public List<GeneracionFacturaResultado> generarFacturasDesdeExcel(File archivoExcel, Path carpetaSalida) throws Exception {
        return generarFacturas(excelService.leerFacturas(archivoExcel), carpetaSalida);
    }

    public List<GeneracionFacturaResultado> generarFacturas(List<FacturaExcel> filas, Path carpetaSalida) throws Exception {
        return generarFacturas(filas, carpetaSalida, "Transferencia");
    }

    public List<GeneracionFacturaResultado> generarFacturas(List<FacturaExcel> filas, Path carpetaSalida, String formaPago) throws Exception {
        Optional<Empresa> empresa = empresaDAO.obtenerPrincipal();
        if (empresa.isEmpty()) {
            throw new IllegalStateException("No hay datos de empresa guardados.");
        }

        List<GeneracionFacturaResultado> resultados = new ArrayList<>();
        Set<String> facturasVistas = new HashSet<>();

        for (FacturaExcel fila : filas) {
            try {
                String error = validarFila(fila, facturasVistas);
                if (error != null) {
                    resultados.add(GeneracionFacturaResultado.error(fila.getNumero(), fila.getNombreCliente(), error));
                    continue;
                }

                List<Cliente> clientes = clienteDAO.buscarPorNombreFiscal(fila.getNombreCliente());
                if (clientes.isEmpty()) {
                    resultados.add(GeneracionFacturaResultado.error(
                            fila.getNumero(),
                            fila.getNombreCliente(),
                            "Cliente no encontrado"
                    ));
                    continue;
                }
                if (clientes.size() > 1) {
                    resultados.add(GeneracionFacturaResultado.error(
                            fila.getNumero(),
                            fila.getNombreCliente(),
                            "Cliente ambiguo"
                    ));
                    continue;
                }

                Cliente cliente = clientes.get(0);
                Factura factura = crearFactura(fila, cliente);
                Path pdf = pdfFacturaService.generarPdf(factura, empresa.get(), carpetaSalida, formaPago);
                factura.setPdfPath(pdf.toAbsolutePath().toString());
                facturaDAO.guardar(factura);
                resultados.add(GeneracionFacturaResultado.correcta(
                        factura.getNumero(),
                        fila.getNombreCliente(),
                        nombreFiscal(cliente),
                        factura.getBaseImponible(),
                        factura.getIva(),
                        factura.getTotal(),
                        pdf
                ));
            } catch (SQLException e) {
                resultados.add(GeneracionFacturaResultado.error(fila.getNumero(), fila.getNombreCliente(), e.getMessage()));
            } catch (Exception e) {
                resultados.add(GeneracionFacturaResultado.error(fila.getNumero(), fila.getNombreCliente(), "No se pudo generar la factura"));
            }
        }

        return resultados;
    }

    private Factura crearFactura(FacturaExcel fila, Cliente cliente) {
        BigDecimal total = valor(fila.getTotalConIva()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseImponible = fila.getBaseImponible() == null
                ? total.divide(new BigDecimal("1.21"), 2, RoundingMode.HALF_UP)
                : fila.getBaseImponible().setScale(2, RoundingMode.HALF_UP);
        BigDecimal iva = fila.getIva() == null
                ? total.subtract(baseImponible).setScale(2, RoundingMode.HALF_UP)
                : fila.getIva().setScale(2, RoundingMode.HALF_UP);

        Factura factura = new Factura();
        factura.setNumero(fila.getNumero());
        factura.setFecha(fila.getFecha());
        factura.setCliente(cliente);
        factura.setConcepto(fila.getConcepto());
        factura.setBaseImponible(baseImponible);
        factura.setIva(iva);
        factura.setTotal(total);
        return factura;
    }

    private BigDecimal valor(BigDecimal cantidad) {
        return cantidad == null ? BigDecimal.ZERO : cantidad;
    }

    private String validarFila(FacturaExcel fila, Set<String> facturasVistas) {
        if (fila.getNumero() == null || fila.getNumero().isBlank()) {
            return "Numero de factura vacio";
        }
        if (!fila.getNumero().matches("\\d+")) {
            return "Numero de factura invalido";
        }
        if (!facturasVistas.add(fila.getNumero())) {
            return "Numero de factura repetido";
        }
        if (fila.getNombreCliente() == null || fila.getNombreCliente().isBlank()) {
            return "Razon social vacia";
        }
        if (fila.getTotalConIva() == null || fila.getTotalConIva().compareTo(BigDecimal.ZERO) <= 0) {
            return "Precio invalido";
        }
        if (fila.getBaseImponible() == null || fila.getBaseImponible().compareTo(BigDecimal.ZERO) <= 0) {
            return "Base imponible invalida";
        }
        if (fila.getIva() == null || fila.getIva().compareTo(BigDecimal.ZERO) < 0) {
            return "IVA invalido";
        }
        return null;
    }

    private String nombreFiscal(Cliente cliente) {
        String razonSocial = cliente.getRazonSocial();
        return razonSocial == null || razonSocial.isBlank() ? cliente.getNombre() : razonSocial;
    }
}
