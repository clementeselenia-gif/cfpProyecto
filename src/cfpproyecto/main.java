package cfpproyecto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Segunda clase principal del proyecto de ventas.
 *
 * <p>Lee los archivos de entrada generados por {@link GenerateInfoFiles}
 * y produce dos reportes CSV ordenados:</p>
 * <ul>
 *   <li>{@value #SALESMEN_REPORT} - vendedores de mayor a menor recaudacion.</li>
 *   <li>{@value #PRODUCTS_REPORT} - productos de mayor a menor cantidad vendida.</li>
 * </ul>
 *
 * <p>Incluye validacion basica de formato e incoherencias en los archivos
 * de entrada: IDs desconocidos, cantidades o precios negativos, lineas
 * con formato invalido.</p>
 *
 * <p>Soporta multiples archivos de ventas por vendedor: procesa el archivo
 * principal TipoDoc_NumDoc.txt y, si existe, un segundo archivo serializado
 * TipoDoc_NumDoc_2.ser con ventas adicionales del mismo vendedor.</p>
 *
 * <p>El programa no solicita informacion al usuario.</p>
 *
 * @author hp
 */
public class main {

    /** Nombre del archivo CSV de reporte de vendedores. */
    private static final String SALESMEN_REPORT = "vendedores_reporte.csv";

    /** Nombre del archivo CSV de reporte de productos. */
    private static final String PRODUCTS_REPORT = "productos_reporte.csv";

    // =========================================================================
    // Metodo main
    // =========================================================================

    /**
     * Punto de entrada del procesador de ventas.
     * Lee los archivos de entrada y genera los reportes CSV ordenados.
     * Muestra un mensaje de exito o de error segun corresponda.
     * No solicita informacion al usuario.
     *
     * @param args Argumentos de linea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        System.out.println("=== main: procesando archivos de ventas ===");
        System.out.println();

        try {
            // Mapas para almacenar el catalogo de productos
            Map<String, String>  productNames      = new HashMap<>();
            Map<String, Double>  productPrices     = new HashMap<>();
            Map<String, Integer> productQuantities = new HashMap<>();

            // 1. Leer catalogo de productos
            readProductsInfo(productNames, productPrices, productQuantities);

            // 2. Leer lista de vendedores
            List<String[]> salesmen = readSalesmenInfo();

            // 3. Procesar archivos de ventas de cada vendedor
            Map<String, Double> salesmenTotals = new HashMap<>();
            for (String[] salesman : salesmen) {
                String docType   = salesman[0];
                String docNumber = salesman[1];
                String fullName  = salesman[2] + " " + salesman[3];

                double total = 0.0;

                // Archivo principal: texto plano TipoDoc_NumDoc.txt
                String primaryFile = docType + "_" + docNumber + ".txt";
                total += processSalesFile(primaryFile, productPrices, productQuantities);

                // Extra [a+b]: archivo adicional serializado TipoDoc_NumDoc_2.ser
                String serializedFile = docType + "_" + docNumber + "_2.ser";
                if (new File(serializedFile).exists()) {
                    total += processSerializedSalesFile(
                            serializedFile, productPrices, productQuantities);
                }

                salesmenTotals.put(fullName, total);
            }

            // 4. Escribir reportes ordenados
            writeSalesmenReport(salesmenTotals);
            writeProductsReport(productNames, productPrices, productQuantities);

            System.out.println();
            System.out.println("Proceso completado exitosamente.");

        } catch (Exception e) {
            System.err.println("Error durante el procesamiento: " + e.getMessage());
        }
    }

    // =========================================================================
    // Metodos de lectura de archivos de entrada
    // =========================================================================

    /**
     * Lee el archivo de catalogo de productos y llena los mapas de nombres,
     * precios y cantidades (inicializadas en cero).
     *
     * <p>Valida que el precio no sea negativo y que el formato de cada
     * linea sea correcto. Las lineas invalidas se omiten con un aviso.</p>
     *
     * @param names      Se llenara con: productId -> nombre del producto.
     * @param prices     Se llenara con: productId -> precio unitario.
     * @param quantities Se llenara con: productId -> 0 (cantidad inicial).
     * @throws IOException Si el archivo no puede leerse.
     */
    private static void readProductsInfo(
            Map<String, String>  names,
            Map<String, Double>  prices,
            Map<String, Integer> quantities) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new FileReader(GenerateInfoFiles.PRODUCTS_INFO_FILE))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 3) {
                    System.err.println("  [AVISO] Formato invalido en linea " + lineNumber
                            + " de productos_info.csv - se omite.");
                    continue;
                }

                String id   = parts[0].trim();
                String name = parts[1].trim();
                double price;

                try {
                    price = Double.parseDouble(parts[2].trim());
                } catch (NumberFormatException e) {
                    System.err.println("  [AVISO] Precio no numerico en linea "
                            + lineNumber + " - se omite.");
                    continue;
                }

                if (price < 0) {
                    System.err.println("  [AVISO] Precio negativo para producto '"
                            + id + "' - se omite.");
                    continue;
                }

                names.put(id, name);
                prices.put(id, price);
                quantities.put(id, 0);
            }
        }

        System.out.println("Productos cargados: " + names.size());
    }

    /**
     * Lee el archivo de informacion de vendedores.
     *
     * <p>Las lineas con formato invalido (menos de 4 campos) se omiten
     * con un aviso en consola.</p>
     *
     * @return Lista de arreglos String[4]: [tipoDoc, numDoc, nombres, apellidos].
     * @throws IOException Si el archivo no puede leerse.
     */
    private static List<String[]> readSalesmenInfo() throws IOException {
        List<String[]> salesmen = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new FileReader(GenerateInfoFiles.SALESMEN_INFO_FILE))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 4) {
                    System.err.println("  [AVISO] Formato invalido en linea " + lineNumber
                            + " de vendedores_info.csv - se omite.");
                    continue;
                }

                salesmen.add(new String[]{
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim(),
                    parts[3].trim()
                });
            }
        }

        System.out.println("Vendedores cargados: " + salesmen.size());
        return salesmen;
    }

    // =========================================================================
    // Metodo de procesamiento de ventas
    // =========================================================================

    /**
     * Lee el archivo de ventas de un vendedor, calcula su total recaudado
     * y acumula las cantidades vendidas por producto en el mapa compartido.
     *
     * <p>Valida:</p>
     * <ul>
     *   <li>Formato invalido de linea.</li>
     *   <li>Cantidad no numerica o negativa.</li>
     *   <li>ID de producto desconocido (no presente en el catalogo).</li>
     * </ul>
     *
     * @param fileName          Nombre del archivo de ventas (ej: CC_12345678.txt).
     * @param productPrices     Mapa de precios: productId -> precio unitario.
     * @param productQuantities Mapa de cantidades acumuladas (se actualiza).
     * @return Total de dinero recaudado por el vendedor.
     */
    private static double processSalesFile(
            String fileName,
            Map<String, Double>  productPrices,
            Map<String, Integer> productQuantities) {

        double total = 0.0;

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            // Linea 1: TipoDoc;NumDoc — se consume pero no se procesa
            String header = reader.readLine();
            if (header == null) {
                System.err.println("  [AVISO] Archivo vacio: " + fileName);
                return 0.0;
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                // Formato esperado: IDProducto;Cantidad;
                String[] parts = line.split(";");
                if (parts.length < 2) {
                    System.err.println("  [AVISO] Formato invalido en linea "
                            + lineNumber + " de " + fileName + " - se omite.");
                    continue;
                }

                String productId = parts[0].trim();
                int quantity;

                try {
                    quantity = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("  [AVISO] Cantidad no numerica en linea "
                            + lineNumber + " de " + fileName + " - se omite.");
                    continue;
                }

                if (quantity < 0) {
                    System.err.println("  [AVISO] Cantidad negativa en linea "
                            + lineNumber + " de " + fileName + " - se omite.");
                    continue;
                }

                if (!productPrices.containsKey(productId)) {
                    System.err.println("  [AVISO] ID de producto desconocido '"
                            + productId + "' en " + fileName + " - se omite.");
                    continue;
                }

                double price = productPrices.get(productId);
                total += price * quantity;
                productQuantities.merge(productId, quantity, Integer::sum);
            }

        } catch (IOException e) {
            System.err.println("  [ERROR] No se pudo leer: " + fileName
                    + " - " + e.getMessage());
        }

        return total;
    }

    // =========================================================================
    // Metodos de escritura de reportes
    // =========================================================================

    /**
     * Escribe el reporte de vendedores ordenado de mayor a menor
     * por el total de dinero recaudado.
     *
     * <p>Formato de cada linea: {@code nombre;totalRecaudado}</p>
     *
     * @param salesmenTotals Mapa de nombre completo -> total recaudado.
     * @throws IOException Si no se puede escribir el archivo.
     */
    private static void writeSalesmenReport(
            Map<String, Double> salesmenTotals) throws IOException {

        // Convertir a lista y ordenar de mayor a menor por recaudacion
        List<Map.Entry<String, Double>> entries = new ArrayList<>(salesmenTotals.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SALESMEN_REPORT))) {
            for (Map.Entry<String, Double> entry : entries) {
                writer.write(entry.getKey() + ";" + String.format("%.2f", entry.getValue()));
                writer.newLine();
            }
        }

        System.out.println("Reporte generado: " + SALESMEN_REPORT);
    }

    /**
     * Escribe el reporte de productos ordenado de mayor a menor
     * por la cantidad total vendida.
     *
     * <p>Formato de cada linea: {@code nombre;precio}</p>
     *
     * @param productNames      Mapa de productId -> nombre.
     * @param productPrices     Mapa de productId -> precio.
     * @param productQuantities Mapa de productId -> cantidad total vendida.
     * @throws IOException Si no se puede escribir el archivo.
     */
    private static void writeProductsReport(
            Map<String, String>  productNames,
            Map<String, Double>  productPrices,
            Map<String, Integer> productQuantities) throws IOException {

        // Ordenar IDs de producto por cantidad vendida descendente
        List<String> productIds = new ArrayList<>(productNames.keySet());
        productIds.sort((a, b) -> Integer.compare(
                productQuantities.getOrDefault(b, 0),
                productQuantities.getOrDefault(a, 0)));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRODUCTS_REPORT))) {
            for (String id : productIds) {
                String name  = productNames.get(id);
                double price = productPrices.getOrDefault(id, 0.0);
                writer.write(name + ";" + price);
                writer.newLine();
            }
        }

        System.out.println("Reporte generado: " + PRODUCTS_REPORT);
    }

    // =========================================================================
    // Extra [b]: lectura de archivos de ventas serializados
    // =========================================================================

    /**
     * Lee un archivo de ventas serializado (.ser), deserializa la lista de
     * lineas de venta y acumula el total recaudado y las cantidades vendidas.
     *
     * <p>El archivo debe contener un {@code ArrayList<String>} serializado,
     * donde el primer elemento es la cabecera (TipoDoc;NumDoc) y los
     * siguientes tienen el formato {@code IDProducto;Cantidad;}.</p>
     *
     * <p>Aplica las mismas validaciones que {@link #processSalesFile}:
     * ID desconocido, cantidad negativa o no numerica.</p>
     *
     * @param fileName          Nombre del archivo .ser (ej: CC_12345_2.ser).
     * @param productPrices     Mapa de precios: productId -> precio unitario.
     * @param productQuantities Mapa de cantidades acumuladas (se actualiza).
     * @return Total de dinero recaudado segun este archivo.
     */
    @SuppressWarnings("unchecked")
    private static double processSerializedSalesFile(
            String fileName,
            Map<String, Double>  productPrices,
            Map<String, Integer> productQuantities) {

        double total = 0.0;

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(fileName))) {

            List<String> salesLines = (List<String>) ois.readObject();

            // La primera linea es la cabecera TipoDoc;NumDoc, se omite
            for (int i = 1; i < salesLines.size(); i++) {
                String line = salesLines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(";");
                if (parts.length < 2) {
                    System.err.println("  [AVISO] Formato invalido en linea "
                            + (i + 1) + " de " + fileName + " - se omite.");
                    continue;
                }

                String productId = parts[0].trim();
                int quantity;

                try {
                    quantity = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("  [AVISO] Cantidad no numerica en linea "
                            + (i + 1) + " de " + fileName + " - se omite.");
                    continue;
                }

                if (quantity < 0) {
                    System.err.println("  [AVISO] Cantidad negativa en linea "
                            + (i + 1) + " de " + fileName + " - se omite.");
                    continue;
                }

                if (!productPrices.containsKey(productId)) {
                    System.err.println("  [AVISO] ID de producto desconocido '"
                            + productId + "' en " + fileName + " - se omite.");
                    continue;
                }

                double price = productPrices.get(productId);
                total += price * quantity;
                productQuantities.merge(productId, quantity, Integer::sum);
            }

            System.out.println("  Archivo serializado procesado: " + fileName);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("  [ERROR] No se pudo deserializar: " + fileName
                    + " - " + e.getMessage());
        }

        return total;
    }
}
