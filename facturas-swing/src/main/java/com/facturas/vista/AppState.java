package com.facturas.vista;

import com.facturas.modelo.Cliente;
import com.facturas.modelo.Empresa;
import com.facturas.modelo.FacturaExcel;
import com.facturas.servicio.GeneracionFacturaResultado;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class AppState {
    private File archivoExcel;
    private List<FacturaExcel> filasExcel = new ArrayList<>();
    private List<ValidacionFacturaExcel> validaciones = new ArrayList<>();
    private List<GeneracionFacturaResultado> resultados = new ArrayList<>();
    private Path carpetaSalida = Path.of("facturas_generadas");

    File getArchivoExcel() {
        return archivoExcel;
    }

    void setArchivoExcel(File archivoExcel) {
        this.archivoExcel = archivoExcel;
    }

    List<FacturaExcel> getFilasExcel() {
        return filasExcel;
    }

    void setFilasExcel(List<FacturaExcel> filasExcel) {
        this.filasExcel = filasExcel == null ? new ArrayList<>() : filasExcel;
    }

    List<ValidacionFacturaExcel> getValidaciones() {
        return validaciones;
    }

    void setValidaciones(List<ValidacionFacturaExcel> validaciones) {
        this.validaciones = validaciones == null ? new ArrayList<>() : validaciones;
    }

    List<GeneracionFacturaResultado> getResultados() {
        return resultados;
    }

    void setResultados(List<GeneracionFacturaResultado> resultados) {
        this.resultados = resultados == null ? new ArrayList<>() : resultados;
    }

    Path getCarpetaSalida() {
        return carpetaSalida;
    }

    void setCarpetaSalida(Path carpetaSalida) {
        if (carpetaSalida != null) {
            this.carpetaSalida = carpetaSalida;
        }
    }

    long contarValidas() {
        return validaciones.stream().filter(ValidacionFacturaExcel::isCorrecta).count();
    }

    boolean puedeGenerar() {
        return archivoExcel != null && validaciones.stream().anyMatch(ValidacionFacturaExcel::isCorrecta);
    }

    Optional<ValidacionFacturaExcel> primeraValida() {
        return validaciones.stream().filter(ValidacionFacturaExcel::isCorrecta).findFirst();
    }
}

final class ValidacionFacturaExcel {
    private final FacturaExcel fila;
    private final Cliente cliente;
    private final Empresa empresa;
    private final String resultado;
    private final boolean correcta;

    ValidacionFacturaExcel(FacturaExcel fila, Cliente cliente, Empresa empresa, String resultado, boolean correcta) {
        this.fila = fila;
        this.cliente = cliente;
        this.empresa = empresa;
        this.resultado = resultado;
        this.correcta = correcta;
    }

    FacturaExcel getFila() {
        return fila;
    }

    Cliente getCliente() {
        return cliente;
    }

    Empresa getEmpresa() {
        return empresa;
    }

    String getResultado() {
        return resultado;
    }

    boolean isCorrecta() {
        return correcta;
    }
}
