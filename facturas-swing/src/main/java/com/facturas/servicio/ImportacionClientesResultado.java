package com.facturas.servicio;

public class ImportacionClientesResultado {
    private final int creados;
    private final int actualizados;
    private final int omitidos;

    public ImportacionClientesResultado(int creados, int actualizados, int omitidos) {
        this.creados = creados;
        this.actualizados = actualizados;
        this.omitidos = omitidos;
    }

    public int getCreados() {
        return creados;
    }

    public int getActualizados() {
        return actualizados;
    }

    public int getOmitidos() {
        return omitidos;
    }

    public int getTotalProcesados() {
        return creados + actualizados;
    }
}
