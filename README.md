# Facturas Dúo

Aplicación de escritorio en Java Swing para generar facturas en PDF con el estilo del modelo `1-vintage.pdf`.

## Objetivo actual

La generación de factura debe respetar el diseño del ejemplo: A4 vertical, marcos negros por secciones, cabecera con emisor y logo, bloque de cliente, tabla de concepto, bloque de importe y franjas grises en el total y forma de pago.

## Ejecutar

```bash
cd facturas-swing
mvn clean compile exec:java
```

## Nota

El PDF de referencia marca el estilo visual que debe imitarse al generar facturas.
