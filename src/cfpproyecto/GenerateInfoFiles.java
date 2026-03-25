package cfpproyecto;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase principal encargada de procesar las ventas y generar
 * los archivos CSV de salida: vendedores.csv y productos.csv.
 *
 * <p>Contiene las clases internas {@link Product}, {@link Sale} y
 * {@link Salesman} para representar los datos del dominio.</p>
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Generar datos de prueba simulados (ventas, productos).</li>
 *   <li>Acumular totales por vendedor y conteos por producto.</li>
 *   <li>Ordenar los resultados de forma descendente.</li>
 *   <li>Escribir los archivos CSV resultantes.</li>
 * </ul>
 *
 *
 * @author hp
 */
public class GenerateInfoFiles {

    // =========================================================================
    // Clases internas del dominio
    // =========================================================================

    /**
     * Representa un producto con su nombre, precio unitario
     * y la cantidad de veces que fue vendido.
     */
    static class Product {

        private String name;
        private double price;
        private int quantitySold;

        /**
         * @param name  Nombre del producto.
         * @param price Precio unitario.
         */
        public Product(String name, double price) {
            this.name = name;
            this.price = price;
            this.quantitySold = 0;
        }

        /** Incrementa en uno la cantidad vendida. */
        public void incrementQuantitySold() { this.quantitySold++; }

        public String getName()        { return name; }
        public double getPrice()       { return price; }
        public int getQuantitySold()   { return quantitySold; }
    }

    /**
     * Representa una venta: relaciona un vendedor con un producto
     * y la cantidad de unidades vendidas en esa transacción.
     */
    static class Sale {

        private String salesmanName;
        private String productName;
        private int quantity;

        /**
         * @param salesmanName Nombre del vendedor.
         * @param productName  Nombre del producto.
         * @param quantity     Unidades vendidas.
         */
        public Sale(String salesmanName, String productName, int quantity) {
            this.salesmanName = salesmanName;
            this.productName  = productName;
            this.quantity     = quantity;
        }

        public String getSalesmanName() { return salesmanName; }
        public String getProductName()  { return productName; }
        public int getQuantity()        { return quantity; }
    }

    /**
     * Representa un vendedor y acumula el total de dinero
     * recaudado por sus ventas.
     */
    static class Salesman {

        private String name;
        private double totalCollected;

        /**
         * @param name Nombre del vendedor.
         */
        public Salesman(String name) {
            this.name = name;
            this.totalCollected = 0.0;
        }

        /**
         * Añade un monto al total recaudado.
         *
         * @param amount Monto a sumar.
         */
        public void addToTotal(double amount) { this.totalCollected += amount; }

        public String getName()           { return name; }
        public double getTotalCollected() { return totalCollected; }
    }

    // =========================================================================
    // Constantes
    // =========================================================================

    /** Nombre del archivo CSV de vendedores. */
    private static final String FILE_SALESMEN = "vendedores.csv";

    /** Nombre del archivo CSV de productos. */
    private static final String FILE_PRODUCTS = "productos.csv";

    // -------------------------------------------------------------------------
    // Métodos de generación de datos de prueba
    // -------------------------------------------------------------------------

    /**
     * Genera una lista de productos de prueba con nombre y precio.
     *
     * @return Lista de objetos {@link Product} con datos simulados.
     */
    public List<Product> generateSampleProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Laptop",      2500000.0));
        products.add(new Product("Mouse",          45000.0));
        products.add(new Product("Teclado",        85000.0));
        products.add(new Product("Monitor",       650000.0));
        products.add(new Product("Audífonos",     120000.0));
        products.add(new Product("Webcam",        200000.0));
        return products;
    }

    /**
     * Genera una lista de ventas de prueba que relacionan vendedores
     * con productos mediante la cantidad vendida.
     *
     * @return Lista de objetos {@link Sale} con datos simulados.
     */
    public List<Sale> generateSampleSales() {
        List<Sale> sales = new ArrayList<>();

        // Vendedor: Carlos Pérez
        sales.add(new Sale("Carlos Pérez",  "Laptop",    2));
        sales.add(new Sale("Carlos Pérez",  "Mouse",     5));
        sales.add(new Sale("Carlos Pérez",  "Monitor",   1));

        // Vendedor: Ana Gómez
        sales.add(new Sale("Ana Gómez",     "Teclado",   3));
        sales.add(new Sale("Ana Gómez",     "Audífonos", 4));
        sales.add(new Sale("Ana Gómez",     "Laptop",    1));

        // Vendedor: Luis Torres
        sales.add(new Sale("Luis Torres",   "Webcam",    6));
        sales.add(new Sale("Luis Torres",   "Mouse",     8));
        sales.add(new Sale("Luis Torres",   "Teclado",   2));

        // Vendedor: Sofía Ruiz
        sales.add(new Sale("Sofía Ruiz",    "Monitor",   3));
        sales.add(new Sale("Sofía Ruiz",    "Audífonos", 2));
        sales.add(new Sale("Sofía Ruiz",    "Webcam",    1));

        return sales;
    }

    // -------------------------------------------------------------------------
    // Métodos de procesamiento
    // -------------------------------------------------------------------------

    /**
     * Procesa las ventas y acumula el total recaudado por cada vendedor.
     * Utiliza el precio de cada producto para calcular el subtotal.
     *
     * @param sales    Lista de ventas realizadas.
     * @param products Lista de productos disponibles (fuente de precios).
     * @return Lista de objetos {@link Salesman} con sus totales, ordenada
     *         de mayor a menor según el dinero recaudado.
     */
    public List<Salesman> processSalesmen(List<Sale> sales, List<Product> products) {

        // Construir un mapa nombre -> precio para acceso rápido
        Map<String, Double> priceMap = new HashMap<>();
        for (Product product : products) {
            priceMap.put(product.getName(), product.getPrice());
        }

        // Acumular totales por vendedor
        Map<String, Salesman> salesmanMap = new HashMap<>();
        for (Sale sale : sales) {
            String salesmanName = sale.getSalesmanName();
            String productName  = sale.getProductName();
            double price        = priceMap.getOrDefault(productName, 0.0);
            double subtotal     = price * sale.getQuantity();

            if (!salesmanMap.containsKey(salesmanName)) {
                salesmanMap.put(salesmanName, new Salesman(salesmanName));
            }
            salesmanMap.get(salesmanName).addToTotal(subtotal);
        }

        // Convertir a lista y ordenar de mayor a menor por total recaudado
        List<Salesman> salesmanList = new ArrayList<>(salesmanMap.values());
        salesmanList.sort((a, b) -> Double.compare(b.getTotalCollected(), a.getTotalCollected()));

        return salesmanList;
    }

    /**
     * Procesa las ventas y acumula la cantidad vendida de cada producto.
     *
     * @param sales    Lista de ventas realizadas.
     * @param products Lista de productos disponibles.
     * @return Lista de objetos {@link Product} con cantidades actualizadas,
     *         ordenada de mayor a menor según la cantidad vendida.
     */
    public List<Product> processProducts(List<Sale> sales, List<Product> products) {

        // Construir un mapa nombre -> producto para acceso rápido
        Map<String, Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getName(), product);
        }

        // Acumular cantidad vendida por producto
        for (Sale sale : sales) {
            String productName = sale.getProductName();
            if (productMap.containsKey(productName)) {
                Product product = productMap.get(productName);
                for (int i = 0; i < sale.getQuantity(); i++) {
                    product.incrementQuantitySold();
                }
            }
        }

        // Ordenar de mayor a menor por cantidad vendida
        List<Product> productList = new ArrayList<>(productMap.values());
        productList.sort((a, b) -> Integer.compare(b.getQuantitySold(), a.getQuantitySold()));

        return productList;
    }

    // -------------------------------------------------------------------------
    // Métodos de escritura de archivos CSV
    // -------------------------------------------------------------------------

    /**
     * Genera el archivo {@code vendedores.csv} con el formato:
     * {@code nombre;totalRecaudado}, ordenado de mayor a menor recaudación.
     *
     * @param salesmenList Lista de vendedores ya ordenada.
     */
    public void writeSalesmenFile(List<Salesman> salesmenList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_SALESMEN))) {
            // Cabecera del archivo
            writer.write("nombre;totalRecaudado");
            writer.newLine();

            for (Salesman salesman : salesmenList) {
                writer.write(salesman.getName() + ";" + salesman.getTotalCollected());
                writer.newLine();
            }

            System.out.println("Archivo generado: " + FILE_SALESMEN);

        } catch (IOException e) {
            System.err.println("Error al escribir " + FILE_SALESMEN + ": " + e.getMessage());
        }
    }

    /**
     * Genera el archivo {@code productos.csv} con el formato:
     * {@code nombre;precio}, ordenado de mayor a menor cantidad vendida.
     *
     * @param productList Lista de productos ya ordenada.
     */
    public void writeProductsFile(List<Product> productList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PRODUCTS))) {
            // Cabecera del archivo
            writer.write("nombre;precio");
            writer.newLine();

            for (Product product : productList) {
                writer.write(product.getName() + ";" + product.getPrice());
                writer.newLine();
            }

            System.out.println("Archivo generado: " + FILE_PRODUCTS);

        } catch (IOException e) {
            System.err.println("Error al escribir " + FILE_PRODUCTS + ": " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Método principal de ejecución
    // -------------------------------------------------------------------------

    /**
     * Ejecuta el flujo completo: genera datos de prueba, los procesa
     * y escribe los archivos CSV de salida.
     */
    public void run() {
        System.out.println("=== Generador de archivos de información de ventas ===");
        System.out.println();

        // 1. Generar datos de prueba
        List<Product> products = generateSampleProducts();
        List<Sale>    sales    = generateSampleSales();

        System.out.println("Ventas cargadas: " + sales.size());
        System.out.println("Productos cargados: " + products.size());
        System.out.println();

        // 2. Procesar vendedores y productos
        List<Salesman> salesmenSorted = processSalesmen(sales, products);
        List<Product>  productsSorted = processProducts(sales, products);

        // 3. Mostrar resumen en consola
        System.out.println("--- Vendedores (mayor a menor recaudación) ---");
        for (Salesman s : salesmenSorted) {
            System.out.println("  " + s.getName() + " -> $" + s.getTotalCollected());
        }
        System.out.println();

        System.out.println("--- Productos (mayor a menor cantidad vendida) ---");
        for (Product p : productsSorted) {
            System.out.println("  " + p.getName() + " -> vendidos: " + p.getQuantitySold());
        }
        System.out.println();

        // 4. Escribir archivos CSV
        writeSalesmenFile(salesmenSorted);
        writeProductsFile(productsSorted);

        System.out.println();
        System.out.println("Proceso completado exitosamente.");
    }
}
