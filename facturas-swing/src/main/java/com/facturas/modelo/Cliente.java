package com.facturas.modelo;

public class Cliente {
    private int id;
    private String nombre;
    private String razonSocial;
    private String nif;
    private String direccion;
    private String codigoPostal;
    private String localidad;
    private String telefono;
    private String email;

    public Cliente() {
    }

    public Cliente(String nombre, String razonSocial, String nif, String direccion, String codigoPostal, String localidad, String telefono) {
        this.nombre = nombre;
        this.razonSocial = razonSocial;
        this.nif = nif;
        this.direccion = direccion;
        this.codigoPostal = codigoPostal;
        this.localidad = localidad;
        this.telefono = telefono;
    }

    public Cliente(String nombre, String razonSocial, String nif, String direccion, String codigoPostal, String localidad, String telefono, String email) {
        this(nombre, razonSocial, nif, direccion, codigoPostal, localidad, telefono);
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
