package com.facturas.servicio;

import com.facturas.modelo.Cliente;
import com.facturas.modelo.FacturaExcel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExcelService {
    private static final String HOJA_FACTURAS = "Hoja3";
    private static final String HOJA_COMERCIOS = "comercios";
    private static final String HOJA_SOCIEDADES = "sociedades";
    private static final String TABLE_NS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String TEXT_NS = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static final String OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

    public List<FacturaExcel> leerFacturas(File archivoExcel) throws IOException {
        if (esCsv(archivoExcel)) {
            return leerFacturasCsv(archivoExcel);
        }
        if (esOds(archivoExcel)) {
            return leerFacturasHoja3(leerHojasOds(archivoExcel).get(HOJA_FACTURAS));
        }
        return leerFacturasExcel(archivoExcel);
    }

    public List<Cliente> leerClientes(File archivoExcel) throws IOException {
        if (esOds(archivoExcel)) {
            Map<String, List<List<String>>> hojas = leerHojasOds(archivoExcel);
            return leerClientes(hojas.get(HOJA_COMERCIOS), hojas.get(HOJA_SOCIEDADES));
        }

        try (FileInputStream inputStream = new FileInputStream(archivoExcel);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            return leerClientes(
                    filas(workbook.getSheet(HOJA_COMERCIOS), formatter, evaluator),
                    filas(workbook.getSheet(HOJA_SOCIEDADES), formatter, evaluator)
            );
        }
    }

    private List<FacturaExcel> leerFacturasCsv(File archivoCsv) throws IOException {
        List<List<String>> filas = leerFilasCsv(archivoCsv);
        if (filas.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> columnas = columnasCsv(filas.get(0));
        if (columnas.containsKey("numero") && columnas.containsKey("cliente")) {
            return leerFacturasCsvConCabecera(filas, columnas);
        }
        if (tieneFormatoHoja3(filas)) {
            return leerFacturasHoja3(filas);
        }
        return leerFacturasLegacy(filas);
    }

    private List<FacturaExcel> leerFacturasExcel(File archivoExcel) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(archivoExcel);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet hoja3 = workbook.getSheet(HOJA_FACTURAS);
            if (hoja3 != null) {
                return leerFacturasHoja3(filas(hoja3, formatter, evaluator));
            }
            return leerFacturasLegacy(workbook.getSheetAt(0), formatter, evaluator);
        }
    }

    private List<FacturaExcel> leerFacturasHoja3(List<List<String>> filas) {
        if (filas == null || filas.isEmpty()) {
            return List.of();
        }

        int indiceCabecera = indiceCabeceraHoja3(filas);
        int inicio = indiceCabecera >= 0 ? indiceCabecera + 1 : 1;
        List<FacturaExcel> facturas = new ArrayList<>();
        for (int i = inicio; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            String numero = normalizarNumeroFactura(valor(fila, 0));
            String cliente = valor(fila, 1);
            BigDecimal base = decimal(valor(fila, 2));
            BigDecimal iva = decimal(valor(fila, 3));
            BigDecimal total = decimal(valor(fila, 5));

            if (positivo(base) && iva == null) {
                iva = calcularIva21(base);
            }

            if (!debeImportarFilaFactura(numero, cliente, base, iva, total)) {
                continue;
            }

            String descripcion = valor(fila, 8);
            FacturaExcel factura = new FacturaExcel();
            factura.setNumero(numero);
            factura.setNombreCliente(cliente);
            factura.setBaseImponible(base);
            factura.setIva(iva);
            factura.setTotalConIva(totalSinRetencion(base, iva, total));
            factura.setCobro(valor(fila, 7));
            factura.setDescripcion(descripcion);
            factura.setConcepto(descripcion.isBlank() ? "Limpieza de cristales" : descripcion);
            factura.setFecha(LocalDate.now());
            facturas.add(factura);
        }
        return facturas;
    }

    private List<FacturaExcel> leerFacturasCsvConCabecera(List<List<String>> filas, Map<String, Integer> columnas) {
        List<FacturaExcel> facturas = new ArrayList<>();
        for (int i = 1; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            String numero = normalizarNumeroFactura(celdaCsv(fila, columnas, "numero"));
            String cliente = celdaCsv(fila, columnas, "cliente");
            BigDecimal base = decimal(celdaCsv(fila, columnas, "base"));
            BigDecimal iva = decimal(celdaCsv(fila, columnas, "iva"));
            BigDecimal totalOriginal = decimal(celdaCsv(fila, columnas, "total"));

            if (base == null && positivo(totalOriginal)) {
                base = totalOriginal.divide(new BigDecimal("1.21"), 2, java.math.RoundingMode.HALF_UP);
            }
            if (positivo(base) && iva == null) {
                iva = calcularIva21(base);
            }
            BigDecimal total = totalSinRetencion(base, iva, totalOriginal);

            if (!debeImportarFilaFactura(numero, cliente, base, iva, total)) {
                continue;
            }

            String concepto = celdaCsv(fila, columnas, "concepto");
            FacturaExcel factura = new FacturaExcel();
            factura.setNumero(numero);
            factura.setNombreCliente(cliente);
            factura.setBaseImponible(base);
            factura.setIva(iva);
            factura.setTotalConIva(total);
            factura.setCobro(celdaCsv(fila, columnas, "cobro"));
            factura.setDescripcion(concepto);
            factura.setConcepto(concepto.isBlank() ? "Limpieza de cristales" : concepto);
            factura.setFecha(fechaCsv(celdaCsv(fila, columnas, "fecha")));
            facturas.add(factura);
        }
        return facturas;
    }

    private List<FacturaExcel> leerFacturasLegacy(List<List<String>> filas) {
        List<FacturaExcel> facturas = new ArrayList<>();
        for (int i = 1; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            if (filaVacia(fila)) {
                continue;
            }

            BigDecimal total = decimal(valor(fila, 3));
            FacturaExcel factura = new FacturaExcel();
            factura.setNumero(normalizarNumeroFactura(valor(fila, 0)));
            factura.setNombreCliente(valor(fila, 1));
            factura.setConcepto(valor(fila, 2));
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal base = total.divide(new BigDecimal("1.21"), 2, java.math.RoundingMode.HALF_UP);
                factura.setBaseImponible(base);
                factura.setIva(total.subtract(base));
            }
            factura.setTotalConIva(total);
            factura.setFecha(LocalDate.now());
            facturas.add(factura);
        }
        return facturas;
    }

    private List<FacturaExcel> leerFacturasLegacy(Sheet hoja, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<FacturaExcel> facturas = new ArrayList<>();
        for (int i = 1; i <= hoja.getLastRowNum(); i++) {
            Row fila = hoja.getRow(i);
            if (fila == null || filaVacia(fila, formatter, evaluator)) {
                continue;
            }

            BigDecimal total = decimal(texto(fila.getCell(3), formatter, evaluator));
            FacturaExcel factura = new FacturaExcel();
            factura.setNumero(normalizarNumeroFactura(texto(fila.getCell(0), formatter, evaluator)));
            factura.setNombreCliente(texto(fila.getCell(1), formatter, evaluator));
            factura.setConcepto(texto(fila.getCell(2), formatter, evaluator));
            if (total != null && total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal base = total.divide(new BigDecimal("1.21"), 2, java.math.RoundingMode.HALF_UP);
                factura.setBaseImponible(base);
                factura.setIva(total.subtract(base));
            }
            factura.setTotalConIva(total);
            factura.setFecha(LocalDate.now());
            facturas.add(factura);
        }
        return facturas;
    }

    private List<Cliente> leerClientes(List<List<String>> comercios, List<List<String>> sociedades) {
        List<Cliente> clientes = new ArrayList<>();
        leerComercios(comercios, clientes);
        leerSociedades(sociedades, clientes);
        return clientes;
    }

    private void leerComercios(List<List<String>> filas, List<Cliente> clientes) {
        if (filas == null) {
            return;
        }
        for (int i = 1; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            String nombreComercial = limpiarTexto(valor(fila, 0));
            String nif = normalizarNif(valor(fila, 3));
            if (nombreComercial.isBlank() || nif.isBlank()) {
                continue;
            }

            Cliente cliente = new Cliente();
            cliente.setNombre(nombreComercial);
            cliente.setRazonSocial(limpiarTexto((valor(fila, 1) + " " + valor(fila, 2)).trim()));
            cliente.setNif(nif);
            cliente.setDireccion(limpiarTexto(valor(fila, 4)));
            aplicarPoblacion(cliente, valor(fila, 5));
            cliente.setTelefono(limpiarTexto(valor(fila, 6)));
            cliente.setEmail(email(valor(fila, 7)));
            clientes.add(cliente);
        }
    }

    private void leerSociedades(List<List<String>> filas, List<Cliente> clientes) {
        if (filas == null) {
            return;
        }
        for (int i = 1; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            String razonSocial = limpiarTexto(valor(fila, 0));
            String nif = normalizarNif(valor(fila, 1));
            if (razonSocial.isBlank() || nif.isBlank()) {
                continue;
            }

            Cliente cliente = new Cliente();
            cliente.setNombre(razonSocial);
            cliente.setRazonSocial(razonSocial);
            cliente.setNif(nif);
            cliente.setDireccion(limpiarTexto(valor(fila, 2)));
            aplicarPoblacion(cliente, valor(fila, 3));
            cliente.setTelefono(limpiarTexto(valor(fila, 4)));
            cliente.setEmail(email(valor(fila, 5)));
            clientes.add(cliente);
        }
    }

    private boolean debeImportarFilaFactura(String numero, String cliente, BigDecimal base, BigDecimal iva, BigDecimal total) {
        boolean tieneImportes = positivo(base) || positivo(iva) || positivo(total);
        if (!tieneImportes) {
            return false;
        }
        return !numero.isBlank() || !cliente.isBlank();
    }

    private int indiceCabeceraHoja3(List<List<String>> filas) {
        for (int i = 0; i < filas.size(); i++) {
            String primera = normalizarCabecera(valor(filas.get(i), 0));
            String segunda = normalizarCabecera(valor(filas.get(i), 1));
            if ((primera.equals("n") || primera.equals("no")) && segunda.equals("cliente")) {
                return i;
            }
        }
        return -1;
    }

    private List<List<String>> filas(Sheet hoja, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (hoja == null) {
            return List.of();
        }

        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i <= hoja.getLastRowNum(); i++) {
            Row row = hoja.getRow(i);
            if (row == null) {
                continue;
            }
            List<String> celdas = new ArrayList<>();
            for (int j = 0; j < Math.max(9, row.getLastCellNum()); j++) {
                celdas.add(texto(row.getCell(j), formatter, evaluator));
            }
            if (!filaVacia(celdas)) {
                filas.add(celdas);
            }
        }
        return filas;
    }

    private Map<String, Integer> columnasCsv(List<String> cabecera) {
        Map<String, Integer> columnas = new HashMap<>();
        for (int i = 0; i < cabecera.size(); i++) {
            String nombre = normalizarCabecera(cabecera.get(i));
            if (nombre.matches("n|no|numero|nfactura|numerofactura|factura")) {
                columnas.putIfAbsent("numero", i);
            } else if (nombre.matches("cliente|nombrecliente|razonsocial|nombreorazonsocial")) {
                columnas.putIfAbsent("cliente", i);
            } else if (nombre.matches("base|baseimponible|preciosiniva|importe")) {
                columnas.putIfAbsent("base", i);
            } else if (nombre.matches("iva|importeiva|cuotaiva")) {
                columnas.putIfAbsent("iva", i);
            } else if (nombre.matches("total|totalconiva|totalfactura|totalacobrar")) {
                columnas.putIfAbsent("total", i);
            } else if (nombre.matches("cobro|formapago|tipopago|pago")) {
                columnas.putIfAbsent("cobro", i);
            } else if (nombre.matches("concepto|descripcion|trabajo|servicio")) {
                columnas.putIfAbsent("concepto", i);
            } else if (nombre.matches("fecha|fechafactura")) {
                columnas.putIfAbsent("fecha", i);
            }
        }
        return columnas;
    }

    private String celdaCsv(List<String> fila, Map<String, Integer> columnas, String columna) {
        Integer indice = columnas.get(columna);
        return indice == null ? "" : valor(fila, indice);
    }

    private List<List<String>> leerFilasCsv(File archivo) throws IOException {
        String contenido = leerTextoCsv(archivo);
        if (contenido.isEmpty()) {
            return List.of();
        }

        char separador = detectarSeparadorCsv(contenido);
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < contenido.length(); i++) {
            char caracter = contenido.charAt(i);
            if (caracter == '"') {
                if (entreComillas && i + 1 < contenido.length() && contenido.charAt(i + 1) == '"') {
                    celda.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (caracter == separador && !entreComillas) {
                fila.add(celda.toString().trim());
                celda.setLength(0);
            } else if ((caracter == '\n' || caracter == '\r') && !entreComillas) {
                if (caracter == '\r' && i + 1 < contenido.length() && contenido.charAt(i + 1) == '\n') {
                    i++;
                }
                fila.add(celda.toString().trim());
                if (!filaVacia(fila)) {
                    filas.add(fila);
                }
                fila = new ArrayList<>();
                celda.setLength(0);
            } else {
                celda.append(caracter);
            }
        }

        fila.add(celda.toString().trim());
        if (!filaVacia(fila)) {
            filas.add(fila);
        }
        return filas;
    }

    private String leerTextoCsv(File archivo) throws IOException {
        byte[] bytes = Files.readAllBytes(archivo.toPath());
        try {
            return quitarBom(decodificar(bytes, StandardCharsets.UTF_8));
        } catch (CharacterCodingException e) {
            return quitarBom(new String(bytes, Charset.forName("windows-1252")));
        }
    }

    private String decodificar(byte[] bytes, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private String quitarBom(String texto) {
        return texto != null && !texto.isEmpty() && texto.charAt(0) == '\uFEFF'
                ? texto.substring(1)
                : texto;
    }

    private char detectarSeparadorCsv(String contenido) {
        String primeraLinea = contenido.lines()
                .filter(linea -> !linea.isBlank())
                .findFirst()
                .orElse("");
        char separador = ';';
        int maximo = contarSeparadorCsv(primeraLinea, separador);
        for (char candidato : new char[]{',', '\t'}) {
            int cantidad = contarSeparadorCsv(primeraLinea, candidato);
            if (cantidad > maximo) {
                separador = candidato;
                maximo = cantidad;
            }
        }
        return separador;
    }

    private int contarSeparadorCsv(String linea, char separador) {
        int total = 0;
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char caracter = linea.charAt(i);
            if (caracter == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (caracter == separador && !entreComillas) {
                total++;
            }
        }
        return total;
    }

    private boolean tieneFormatoHoja3(List<List<String>> filas) {
        if (indiceCabeceraHoja3(filas) >= 0) {
            return true;
        }

        return filas.stream().limit(10).anyMatch(fila -> {
            BigDecimal base = decimal(valor(fila, 2));
            BigDecimal iva = decimal(valor(fila, 3));
            BigDecimal total = decimal(valor(fila, 5));
            return (positivo(base) || positivo(iva))
                    && positivo(total)
                    && (!valor(fila, 0).isBlank() || !valor(fila, 1).isBlank());
        });
    }

    private LocalDate fechaCsv(String texto) {
        if (texto == null || texto.isBlank()) {
            return LocalDate.now();
        }

        for (DateTimeFormatter formato : List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu")
        )) {
            try {
                return LocalDate.parse(texto.trim(), formato);
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDate.now();
    }

    private BigDecimal totalSinRetencion(BigDecimal base, BigDecimal iva, BigDecimal totalOriginal) {
        if (base != null && iva != null) {
            return base.add(iva).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return totalOriginal == null ? null : totalOriginal.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal calcularIva21(BigDecimal base) {
        return base.multiply(new BigDecimal("0.21")).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Map<String, List<List<String>>> leerHojasOds(File archivo) throws IOException {
        try (ZipFile zipFile = new ZipFile(archivo)) {
            ZipEntry content = zipFile.getEntry("content.xml");
            if (content == null) {
                throw new IOException("El archivo ODS no contiene content.xml");
            }

            try (InputStream inputStream = zipFile.getInputStream(content)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                Document document = factory.newDocumentBuilder().parse(inputStream);
                NodeList tablas = document.getElementsByTagNameNS(TABLE_NS, "table");
                Map<String, List<List<String>>> hojas = new HashMap<>();
                for (int i = 0; i < tablas.getLength(); i++) {
                    Element tabla = (Element) tablas.item(i);
                    String nombre = tabla.getAttributeNS(TABLE_NS, "name");
                    hojas.put(nombre, filasOds(tabla));
                }
                return hojas;
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("No se pudo leer el contenido del ODS", e);
        }
    }

    private List<List<String>> filasOds(Element tabla) {
        List<List<String>> filas = new ArrayList<>();
        NodeList hijos = tabla.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node nodo = hijos.item(i);
            if (!esNodo(nodo, TABLE_NS, "table-row")) {
                continue;
            }

            Element fila = (Element) nodo;
            int repeticiones = Math.min(repeticiones(fila, "number-rows-repeated"), 200);
            List<String> celdas = celdasOds(fila);
            if (filaVacia(celdas)) {
                continue;
            }
            for (int j = 0; j < repeticiones; j++) {
                filas.add(celdas);
            }
        }
        return filas;
    }

    private List<String> celdasOds(Element fila) {
        List<String> celdas = new ArrayList<>();
        NodeList hijos = fila.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node nodo = hijos.item(i);
            if (!esNodo(nodo, TABLE_NS, "table-cell") && !esNodo(nodo, TABLE_NS, "covered-table-cell")) {
                continue;
            }

            Element celda = (Element) nodo;
            int repeticiones = Math.min(repeticiones(celda, "number-columns-repeated"), 50);
            String valor = valorCeldaOds(celda);
            for (int j = 0; j < repeticiones; j++) {
                celdas.add(valor);
            }
        }
        return celdas;
    }

    private String valorCeldaOds(Element celda) {
        String fecha = celda.getAttributeNS(OFFICE_NS, "date-value");
        if (!fecha.isBlank()) {
            return fecha.trim();
        }

        String valor = celda.getAttributeNS(OFFICE_NS, "value");
        if (!valor.isBlank()) {
            return valor.trim();
        }

        NodeList textos = celda.getElementsByTagNameNS(TEXT_NS, "p");
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < textos.getLength(); i++) {
            String texto = textos.item(i).getTextContent().trim();
            if (!texto.isBlank()) {
                partes.add(texto);
            }
        }
        return String.join(" ", partes).trim();
    }

    private int repeticiones(Element elemento, String atributo) {
        String valor = elemento.getAttributeNS(TABLE_NS, atributo);
        if (valor == null || valor.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private boolean esNodo(Node nodo, String namespace, String localName) {
        return nodo instanceof Element
                && namespace.equals(nodo.getNamespaceURI())
                && localName.equals(nodo.getLocalName());
    }

    private boolean filaVacia(Row fila, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int i = 0; i < 4; i++) {
            Cell celda = fila.getCell(i);
            if (celda != null && celda.getCellType() != CellType.BLANK && !texto(celda, formatter, evaluator).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean filaVacia(List<String> fila) {
        return fila.stream().allMatch(String::isBlank);
    }

    private String texto(Cell celda, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (celda == null) {
            return "";
        }

        return formatter.formatCellValue(celda, evaluator).trim();
    }

    private BigDecimal decimal(String valorOriginal) {
        if (valorOriginal == null) {
            return null;
        }

        String valor = valorOriginal
                .replace("\u00a0", "")
                .replace(" ", "")
                .replace("€", "")
                .replace("â‚¬", "")
                .trim();
        if (valor.contains(",") && valor.contains(".")) {
            int ultimaComa = valor.lastIndexOf(',');
            int ultimoPunto = valor.lastIndexOf('.');
            if (ultimaComa > ultimoPunto) {
                valor = valor.replace(".", "").replace(",", ".");
            } else {
                valor = valor.replace(",", "");
            }
        } else if (valor.contains(",")) {
            valor = valor.replace(".", "").replace(",", ".");
        }
        try {
            return valor.isBlank() ? null : new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean positivo(BigDecimal cantidad) {
        return cantidad != null && cantidad.compareTo(BigDecimal.ZERO) > 0;
    }

    private String valor(List<String> fila, int indice) {
        if (fila == null || indice >= fila.size() || fila.get(indice) == null) {
            return "";
        }
        return fila.get(indice).trim();
    }

    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim();
    }

    private String normalizarNumeroFactura(String texto) {
        String numero = limpiarTexto(texto).replace("\u00a0", "").replace(" ", "");
        if (numero.matches("\\d+[,.]0+")) {
            return numero.replaceAll("[,.]0+$", "");
        }
        return numero;
    }

    private String normalizarNif(String nif) {
        return limpiarTexto(nif).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String email(String texto) {
        String email = limpiarTexto(texto);
        return email.replace("-", "").isBlank() ? "" : email;
    }

    private void aplicarPoblacion(Cliente cliente, String poblacion) {
        String valor = limpiarTexto(poblacion);
        if (valor.isBlank()) {
            return;
        }

        int coma = valor.indexOf(',');
        if (coma > 0) {
            cliente.setCodigoPostal(valor.substring(0, coma).trim());
            cliente.setLocalidad(valor.substring(coma + 1).trim());
            return;
        }

        String[] partes = valor.split("\\s+", 2);
        if (partes.length == 2 && partes[0].matches("\\d{5}")) {
            cliente.setCodigoPostal(partes[0]);
            cliente.setLocalidad(partes[1].trim());
        } else {
            cliente.setLocalidad(valor);
        }
    }

    private boolean esOds(File archivo) {
        return archivo != null && archivo.getName().toLowerCase(Locale.ROOT).endsWith(".ods");
    }

    private boolean esCsv(File archivo) {
        return archivo != null && archivo.getName().toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private String normalizarCabecera(String texto) {
        return texto == null
                ? ""
                : Normalizer.normalize(texto.toLowerCase(Locale.ROOT).replace("º", "o"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "");
    }
}
