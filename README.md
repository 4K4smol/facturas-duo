# Facturas Dúo

Aplicación de escritorio en Java Swing para generar facturas en PDF con el estilo del modelo `1-vintage.pdf`.

## Objetivo actual

La generación de factura debe respetar el diseño del ejemplo:

- Página A4 vertical.
- Marcos negros gruesos por secciones.
- Cabecera con datos del emisor a la izquierda y logo a la derecha.
- Bloque de fecha y número de factura.
- Bloque de cliente.
- Tabla de concepto y precio sin IVA.
- Bloque de importe con base imponible, IVA, retenciones y total.
- Franja gris en `TOTAL A COBRAR` y en `Tipo de pago`.
- Tipografía tipo Calibri cuando el sistema la tenga disponible.

## Ejecutar

```bash
cd facturas-swing
mvn clean compile exec:java
```

Al abrir la ventana, pulsa **Generar factura PDF estilo vintage** para crear una factura demo igual al ejemplo.

## Nota sobre exactitud visual

La clase `VintageFacturaPdfGenerator` usa coordenadas fijas medidas del PDF de referencia. El recurso `logo_limpiezas_duo.png.base64` contiene el símbolo dorado extraído del modelo para poder colocarlo en el mismo lugar del documento.

En Windows se intenta cargar Calibri desde `C:/Windows/Fonts`. Si no existe, se usa una fuente alternativa compatible.
