package cfpproyecto;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Primera clase principal del proyecto de ventas.
 *
 * <p>Al ejecutarse genera los archivos de entrada pseudoaleatorios que
 * servirán como datos de prueba para la clase {@link main}:</p>
 * <ul>
 *   <li>{@value #PRODUCTS_INFO_FILE} - catalogo de productos.</li>
 *   <li>{@value #SALESMEN_INFO_FILE} - informacion de vendedores.</li>
 *   <li>Un archivo TipoDoc_NumDoc.txt por cada vendedor generado.</li>
 * </ul>
 *
 * <p>El programa no solicita informacion al usuario.</p>
 *
 *
 * @author hp
 */
public class GenerateInfoFiles {

    // =========================================================================
    // Constantes de nombres de archivos (compartidas con la clase main)
    // =========================================================================

    /** Archivo CSV con el catalogo de productos de entrada. */
    static final String PRODUCTS_INFO_FILE = "productos_info.csv";

    /** Archivo CSV con la informacion de vendedores de entrada. */
    static final String SALESMEN_INFO_FILE = "vendedores_info.csv";

    // =========================================================================
    // Datos para generacion pseudoaleatoria
    // =========================================================================

    /** Nombres de pila reales para generar vendedores ficticios. */
    private static final String[] FIRST_NAMES = {
        "Carlos", "Ana", "Luis", "Sofia", "Miguel",
        "Laura", "Andres", "Maria", "Juan", "Claudia"
    };

    /** Apellidos reales para generar vendedores ficticios. */
    private static final String[] LAST_NAMES = {
        "Perez", "Gomez", "Torres", "Ruiz", "Lopez",
        "Martinez", "Garcia", "Rodriguez", "Vargas", "Castro"
    };

    /** Nombres de productos para el catalogo de prueba. */
    private static final String[] PRODUCT_NAMES = {
        "Laptop", "Mouse", "Teclado", "Monitor", "Audifonos",
        "Webcam", "SSD 1TB", "Memoria RAM", "Impresora", "Tablet",
        "Smartphone", "Cargador USB", "Cable HDMI", "Hub USB", "Soporte Monitor"
    };

    /** Tipos de documento validos. */
    private static final String[] DOC_TYPES = {"CC", "CE", "PA"};

    // =========================================================================
    // Metodo main
    // =========================================================================

    /**
     * Punto de entrada del generador de archivos de prueba.
     * Llama a los metodos de creacion de archivos y muestra
     * un mensaje de exito o de error segun corresponda.
     *
     * @param args Argumentos de linea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        System.out.println("=== GenerateInfoFiles: generando archivos de entrada ===");
        System.out.println();

        try {
            createProductsFile(10);
            createSalesManInfoFile(5);
            System.out.println();
            System.out.println("Proceso completado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error durante la generacion de archivos: " + e.getMessage());
        }
    }

    // =========================================================================
    // Metodos de generacion requeridos por el enunciado
    // =========================================================================

    /**
     * Crea un archivo de ventas pseudoaleatorio para un vendedor.
     *
     * <p>Formato del archivo generado:</p>
     * <pre>
     * TipoDocumento;NumeroDocumento
     * IDProducto1;Cantidad1;
     * IDProducto2;Cantidad2;
     * </pre>
     *
     * <p>El archivo se nombra CC_{id}.txt.
     * Los IDs de producto generados pertenecen al rango P001-P010.</p>
     *
     * @param randomSalesCount Numero de lineas de venta a generar.
     * @param name             Nombre del vendedor (se muestra en consola).
     * @param id               Numero de documento del vendedor.
     */
    public static void createSalesMenFile(int randomSalesCount, String name, long id) {
        createSalesMenFile(randomSalesCount, name, id, "CC");
    }

    /**
     * Sobrecarga interna de createSalesMenFile(int, String, long)
     * que acepta el tipo de documento para nombrar el archivo correctamente.
     *
     * @param randomSalesCount Numero de lineas de venta a generar.
     * @param name             Nombre del vendedor.
     * @param id               Numero de documento del vendedor.
     * @param docType          Tipo de documento (CC, CE, PA).
     */
    static void createSalesMenFile(int randomSalesCount, String name, long id, String docType) {
        String fileName = docType + "_" + id + ".txt";
        Random random   = new Random();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Primera linea: identificacion del vendedor
            writer.write(docType + ";" + id);
            writer.newLine();

            // Lineas de venta: IDProducto;Cantidad;
            for (int i = 0; i < randomSalesCount; i++) {
                String productId = String.format("P%03d", random.nextInt(10) + 1);
                int    quantity  = random.nextInt(10) + 1;
                writer.write(productId + ";" + quantity + ";");
                writer.newLine();
            }

            System.out.println("  Ventas creadas: " + fileName + " - " + name);

        } catch (IOException e) {
            System.err.println("  Error al crear " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Crea un archivo CSV con informacion pseudoaleatoria de productos.
     *
     * <p>Formato: IDProducto;NombreProducto;PrecioPorUnidad</p>
     * <p>Los IDs se generan como P001, P002, etc.
     * Los precios son valores entre $10.000 y $500.000.</p>
     *
     * @param productsCount Numero de productos a generar.
     */
    public static void createProductsFile(int productsCount) {
        Random random = new Random();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRODUCTS_INFO_FILE))) {
            for (int i = 0; i < productsCount; i++) {
                String id    = String.format("P%03d", i + 1);
                String name  = PRODUCT_NAMES[i % PRODUCT_NAMES.length];
                double price = 10000 + (random.nextInt(490) * 1000.0);
                writer.write(id + ";" + name + ";" + price);
                writer.newLine();
            }
            System.out.println("Productos creados: " + PRODUCTS_INFO_FILE
                    + " (" + productsCount + " productos)");

        } catch (IOException e) {
            System.err.println("Error al crear " + PRODUCTS_INFO_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Crea un archivo CSV con informacion pseudoaleatoria de vendedores
     * y genera el archivo de ventas individual de cada uno.
     *
     * <p>Formato del archivo de informacion: TipoDoc;NumDoc;Nombres;Apellidos</p>
     * <p>Los nombres y apellidos se extraen de listas de nombres reales.</p>
     *
     * @param salesmanCount Numero de vendedores a generar.
     */
    public static void createSalesManInfoFile(int salesmanCount) {
        Random random = new Random();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SALESMEN_INFO_FILE))) {
            for (int i = 0; i < salesmanCount; i++) {
                String docType   = DOC_TYPES[random.nextInt(DOC_TYPES.length)];
                long   docNumber = 10000000L + (long)(random.nextInt(90000000));
                String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String lastName  = LAST_NAMES[random.nextInt(LAST_NAMES.length)];

                // Escribir linea en el archivo de informacion
                writer.write(docType + ";" + docNumber + ";" + firstName + ";" + lastName);
                writer.newLine();

                // Crear tambien el archivo de ventas de este vendedor
                createSalesMenFile(15, firstName + " " + lastName, docNumber, docType);
            }
            System.out.println("Vendedores creados: " + SALESMEN_INFO_FILE
                    + " (" + salesmanCount + " vendedores)");

        } catch (IOException e) {
            System.err.println("Error al crear " + SALESMEN_INFO_FILE + ": " + e.getMessage());
        }
    }
}