package co.edu.uniquindio.bibliometria.web;

import co.edu.uniquindio.bibliometria.busqueda.MotorBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.RabinKarp;
import co.edu.uniquindio.bibliometria.clustering.MotorClustering;
import co.edu.uniquindio.bibliometria.clustering.ResultadoClustering;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.ordenamiento.MotorRanking;
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.util.JsonEscritor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Servidor HTTP y capa de API REST de la aplicacion.
 * <p>
 * Se implemento con {@link com.sun.net.httpserver.HttpServer} (incluido en
 * el JDK estandar, modulo {@code jdk.httpserver}) en lugar de un framework
 * web como Spring Boot, porque el entorno donde se desarrollo y verifico
 * este proyecto no tenia acceso de red a un repositorio de dependencias
 * (Maven Central) para descargar dicho framework. Esta decision se
 * documenta en el README y en el documento tecnico. El resultado es una
 * aplicacion sin dependencias externas: compila y corre unicamente con el
 * JDK (17+), lo que tambien simplifica el despliegue.
 * <p>
 * Expone:
 * <ul>
 *     <li>{@code GET /api/articulos} y {@code GET /api/articulos/{id}}</li>
 *     <li>{@code GET /api/similitud?idA=..&idB=..} (Requerimiento 1)</li>
 *     <li>{@code GET /api/clustering} (Requerimiento 2)</li>
 *     <li>{@code GET /api/busqueda?patron=..} (Requerimiento 3)</li>
 *     <li>{@code GET /api/ranking?top=15&repeticiones=50} (Seguimiento: ranking de terminos)</li>
 *     <li>Archivos estaticos del frontend (carpeta {@code static/})</li>
 * </ul>
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ServidorHttp {

    /** Servidor HTTP del JDK que atiende las peticiones. */
    private final HttpServer servidor;
    // Servicios de cada requerimiento; se reciben ya construidos desde BibliometriaApplication.
    private final RepositorioArticulos repositorioArticulos;
    private final MotorSimilitud motorSimilitud;
    private final MotorClustering motorClustering;
    private final MotorRanking motorRanking;
    private final MotorBusqueda motorBusqueda;

    /**
     * Crea el servidor y registra cada ruta con su manejador. No empieza a
     * escuchar hasta llamar a {@link #iniciar()}.
     *
     * @param puerto puerto donde escuchara (8080 por defecto, o el de la variable PORT)
     * @throws IOException si el puerto no se puede abrir (por ejemplo, ya esta en uso)
     */
    public ServidorHttp(int puerto, RepositorioArticulos repositorioArticulos,
                         MotorSimilitud motorSimilitud, MotorClustering motorClustering,
                         MotorRanking motorRanking, MotorBusqueda motorBusqueda) throws IOException {
        this.repositorioArticulos = repositorioArticulos;
        this.motorSimilitud = motorSimilitud;
        this.motorClustering = motorClustering;
        this.motorRanking = motorRanking;
        this.motorBusqueda = motorBusqueda;

        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        // Rutas de la API. La ruta "/" va al final y atiende todo lo demas (archivos del frontend).
        servidor.createContext("/api/articulos", conManejoDeErrores(this::manejarArticulos));
        servidor.createContext("/api/similitud", conManejoDeErrores(this::manejarSimilitud));
        servidor.createContext("/api/clustering", conManejoDeErrores(this::manejarClustering));
        servidor.createContext("/api/ranking", conManejoDeErrores(this::manejarRanking));
        servidor.createContext("/api/busqueda", conManejoDeErrores(this::manejarBusqueda));
        servidor.createContext("/", conManejoDeErrores(this::manejarEstatico));
        // Hasta 8 peticiones atendidas al mismo tiempo.
        servidor.setExecutor(Executors.newFixedThreadPool(8));
    }

    /**
     * Envuelve un manejador de rutas para gestionar de forma centralizada
     * cualquier excepcion no controlada que pueda ocurrir al procesar una
     * peticion (por ejemplo, un error inesperado al leer o serializar
     * datos). Sin este envoltorio, el {@link HttpServer} del JDK simplemente
     * cierra la conexion sin enviar respuesta cuando el handler lanza una
     * excepcion, dejando al cliente sin ninguna informacion del error.
     *
     * @param manejador logica especifica de la ruta (puede lanzar {@link IOException})
     * @return manejador equivalente que garantiza el envio de una respuesta HTTP,
     *         incluso ante un fallo inesperado (500 con un mensaje de error en JSON)
     */
    private com.sun.net.httpserver.HttpHandler conManejoDeErrores(ManejadorConExcepcion manejador) {
        return exchange -> {
            try {
                manejador.manejar(exchange);
            } catch (IOException e) {
                // Error de E/S al leer la peticion o escribir la respuesta: se
                // registra pero no se puede garantizar el envio de una respuesta.
                System.err.println("Error de E/S atendiendo " + exchange.getRequestURI() + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error inesperado atendiendo " + exchange.getRequestURI() + ": " + e);
                try {
                    responderJson(exchange, 500, Map.of("error", "Error interno del servidor: " + e.getMessage()));
                } catch (IOException ioException) {
                    System.err.println("No se pudo enviar la respuesta de error: " + ioException.getMessage());
                }
            }
        };
    }

    /** Variante de {@link com.sun.net.httpserver.HttpHandler} que permite propagar {@link IOException}. */
    @FunctionalInterface
    private interface ManejadorConExcepcion {
        void manejar(HttpExchange exchange) throws IOException;
    }

    /** Empieza a escuchar peticiones en segundo plano (el metodo retorna de inmediato). */
    public void iniciar() {
        servidor.start();
    }

    // -----------------------------------------------------------------
    // Handlers de la API
    // -----------------------------------------------------------------

    /**
     * {@code GET /api/articulos}: lista los 20 articulos.
     * {@code GET /api/articulos/{id}}: devuelve un articulo (404 si no existe,
     * 400 si el id no es un numero).
     */
    private void manejarArticulos(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        // Lo que viene despues de "/api/articulos": vacio para la lista, "/5" para un articulo.
        String resto = path.substring("/api/articulos".length());
        if (resto.isEmpty() || resto.equals("/")) {
            List<Map<String, Object>> lista = repositorioArticulos.listarTodos().stream()
                    .map(a -> new ArticuloDTOSalida(a).aMapa())
                    .collect(Collectors.toList());
            responderJson(exchange, 200, lista);
        } else {
            try {
                int id = Integer.parseInt(resto.replace("/", ""));
                Optional<Articulo> articulo = repositorioArticulos.buscarPorId(id);
                if (articulo.isPresent()) {
                    responderJson(exchange, 200, new ArticuloDTOSalida(articulo.get()).aMapa());
                } else {
                    responderJson(exchange, 404, Map.of("error", "Articulo no encontrado: " + id));
                }
            } catch (NumberFormatException e) {
                responderJson(exchange, 400, Map.of("error", "Id invalido: " + resto));
            }
        }
    }

    /**
     * {@code GET /api/similitud?ids=1,2,5} (Requerimiento 1): compara dos o
     * mas articulos. Valida que los ids existan y no se repitan, ejecuta los 6
     * algoritmos sobre cada par y devuelve los articulos, los resultados de
     * cada par y una matriz de similitud por algoritmo.
     * <p>
     * Tambien acepta la forma {@code ?idA=1&idB=2} para un solo par, que
     * devuelve los dos articulos con los 6 resultados. Responde 400 si faltan
     * parametros o no son validos.
     */
    private void manejarSimilitud(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        Map<String, String> query = parsearQuery(exchange.getRequestURI().getRawQuery());
        if (query.containsKey("ids")) {
            manejarSimilitudVarios(exchange, query.get("ids"));
            return;
        }
        try {
            // Si falta un parametro, parseInt(null) lanza NumberFormatException y se responde 400.
            int idA = Integer.parseInt(query.get("idA"));
            int idB = Integer.parseInt(query.get("idB"));

            Optional<Articulo> a = repositorioArticulos.buscarPorId(idA);
            Optional<Articulo> b = repositorioArticulos.buscarPorId(idB);

            if (a.isEmpty() || b.isEmpty()) {
                responderJson(exchange, 400, Map.of("error",
                        "Alguno de los articulos indicados no existe (ids validos: 1-" + repositorioArticulos.total() + ")"));
                return;
            }
            if (idA == idB) {
                responderJson(exchange, 400, Map.of("error", "Selecciona dos articulos distintos"));
                return;
            }

            List<ResultadoSimilitud> resultados = motorSimilitud.compararArticulos(a.get(), b.get());
            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("articuloA", new ArticuloDTOSalida(a.get()).aMapa());
            respuesta.put("articuloB", new ArticuloDTOSalida(b.get()).aMapa());
            respuesta.put("resultados", resultados.stream().map(ResultadoSimilitud::aMapa).collect(Collectors.toList()));
            responderJson(exchange, 200, respuesta);
        } catch (NumberFormatException | NullPointerException e) {
            responderJson(exchange, 400, Map.of("error", "Parametros invalidos. Uso: /api/similitud?ids=1,2,5"));
        }
    }

    /** Comparacion de dos o mas articulos ({@code ids} separados por comas). */
    private void manejarSimilitudVarios(HttpExchange exchange, String idsTexto) throws IOException {
        List<Articulo> seleccionados = new java.util.ArrayList<>();
        try {
            for (String parte : idsTexto.split(",")) {
                if (parte.isBlank()) continue;
                int id = Integer.parseInt(parte.trim());
                Optional<Articulo> articulo = repositorioArticulos.buscarPorId(id);
                if (articulo.isEmpty()) {
                    responderJson(exchange, 400, Map.of("error",
                            "El articulo " + id + " no existe (ids validos: 1-" + repositorioArticulos.total() + ")"));
                    return;
                }
                if (seleccionados.contains(articulo.get())) {
                    responderJson(exchange, 400, Map.of("error", "El articulo " + id + " esta repetido"));
                    return;
                }
                seleccionados.add(articulo.get());
            }
        } catch (NumberFormatException e) {
            responderJson(exchange, 400, Map.of("error", "Parametros invalidos. Uso: /api/similitud?ids=1,2,5"));
            return;
        }
        if (seleccionados.size() < 2) {
            responderJson(exchange, 400, Map.of("error", "Selecciona al menos dos articulos"));
            return;
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("articulos", seleccionados.stream().map(a -> new ArticuloDTOSalida(a).aMapa())
                .collect(Collectors.toList()));
        respuesta.putAll(motorSimilitud.compararVarios(seleccionados));
        responderJson(exchange, 200, respuesta);
    }

    /**
     * {@code GET /api/clustering} (Requerimiento 2): devuelve los 3
     * dendrogramas con su correlacion cofenetica e indica cual es el mas
     * coherente. Los resultados ya estan calculados desde el arranque.
     */
    private void manejarClustering(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        List<ResultadoClustering> resultados = motorClustering.getResultados();
        ResultadoClustering mejor = motorClustering.getMejorResultado();

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("resultados", resultados.stream().map(ResultadoClustering::aMapa).collect(Collectors.toList()));
        respuesta.put("algoritmoMasCoherente", mejor.getNombreEnlace());
        respuesta.put("correlacionCofeneticaMejor", mejor.getCorrelacionCofenetica());
        responderJson(exchange, 200, respuesta);
    }

    /**
     * {@code GET /api/ranking?top=15&repeticiones=50} (Seguimiento): ordena
     * la lista de frecuencias con Counting, Radix y Bucket Sort, mide sus
     * tiempos y devuelve el top N de cada uno. Ambos parametros son
     * opcionales; si faltan se usan 15 y 50.
     */
    private void manejarRanking(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        Map<String, String> query = parsearQuery(exchange.getRequestURI().getRawQuery());
        try {
            // Valores por defecto cuando el parametro no viene en la URL.
            int top = Integer.parseInt(query.getOrDefault("top", String.valueOf(MotorRanking.TOP_POR_DEFECTO)));
            int repeticiones = Integer.parseInt(query.getOrDefault("repeticiones",
                    String.valueOf(MotorRanking.REPETICIONES_POR_DEFECTO)));
            // Limites para que una peticion no bloquee el servidor con millones de repeticiones.
            if (top < 1 || top > 100 || repeticiones < 1 || repeticiones > 500) {
                responderJson(exchange, 400, Map.of("error", "Rangos validos: top 1-100, repeticiones 1-500"));
                return;
            }
            responderJson(exchange, 200, motorRanking.ejecutarComoMapa(top, repeticiones));
        } catch (NumberFormatException e) {
            responderJson(exchange, 400, Map.of("error", "Parametros invalidos. Uso: /api/ranking?top=15&repeticiones=50"));
        }
    }

    /**
     * {@code GET /api/busqueda?patron=algorithm&mayusculas=false&modulo=1000000007&traza=1&repeticiones=50}
     * (Requerimiento 3): busca el patron en los 20 abstracts con KMP,
     * Boyer-Moore y Rabin-Karp. Solo {@code patron} es obligatorio;
     * {@code traza} es el id del articulo cuya traza paso a paso se devuelve.
     */
    private void manejarBusqueda(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        Map<String, String> query = parsearQuery(exchange.getRequestURI().getRawQuery());
        String patron = query.get("patron");
        if (patron == null || patron.isEmpty() || patron.length() > MotorBusqueda.LONGITUD_MAXIMA_PATRON) {
            responderJson(exchange, 400, Map.of("error",
                    "Indica un patron de 1 a " + MotorBusqueda.LONGITUD_MAXIMA_PATRON + " caracteres. Uso: /api/busqueda?patron=learning"));
            return;
        }
        try {
            boolean distinguirMayusculas = Boolean.parseBoolean(query.getOrDefault("mayusculas", "false"));
            long modulo = Long.parseLong(query.getOrDefault("modulo", String.valueOf(RabinKarp.MODULO_POR_DEFECTO)));
            int repeticiones = Integer.parseInt(query.getOrDefault("repeticiones",
                    String.valueOf(MotorBusqueda.REPETICIONES_POR_DEFECTO)));
            Integer traza = query.containsKey("traza") ? Integer.valueOf(query.get("traza")) : null;
            if (modulo < 2 || modulo > 2_000_000_000L || repeticiones < 1 || repeticiones > 500) {
                responderJson(exchange, 400, Map.of("error", "Rangos validos: modulo 2-2000000000, repeticiones 1-500"));
                return;
            }
            responderJson(exchange, 200, motorBusqueda.buscar(patron, distinguirMayusculas, modulo, traza, repeticiones));
        } catch (NumberFormatException e) {
            responderJson(exchange, 400, Map.of("error", "Parametros invalidos. Uso: /api/busqueda?patron=learning&traza=1"));
        }
    }

    // -----------------------------------------------------------------
    // Archivos estaticos (frontend)
    // -----------------------------------------------------------------

    /**
     * Sirve los archivos del frontend (HTML, CSS, JS) guardados en la carpeta
     * {@code static/} de los recursos. La raiz "/" muestra {@code index.html};
     * si el archivo no existe responde 404.
     */
    private void manejarEstatico(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        // Por ejemplo, "/app.js" se busca como "/static/app.js" dentro de la aplicacion.
        String recurso = "/static" + path;

        try (InputStream is = ServidorHttp.class.getResourceAsStream(recurso)) {
            if (is == null) {
                responderTexto(exchange, 404, "No encontrado: " + path);
                return;
            }
            byte[] contenido = leerTodo(is);
            String tipoContenido = detectarTipoContenido(path);
            exchange.getResponseHeaders().set("Content-Type", tipoContenido);
            exchange.sendResponseHeaders(200, contenido.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(contenido);
            }
        }
    }

    /**
     * Elige el tipo de contenido (Content-Type) segun la extension del
     * archivo, para que el navegador sepa como interpretarlo.
     */
    private static String detectarTipoContenido(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    // -----------------------------------------------------------------
    // Utilidades HTTP
    // -----------------------------------------------------------------

    /**
     * Convierte el cuerpo a JSON y lo envia con el codigo HTTP indicado.
     * Permite peticiones desde cualquier origen (CORS) para poder probar la
     * API desde otras paginas.
     *
     * @param codigo codigo HTTP (200 exito, 400 peticion invalida, 404 no encontrado, 500 error interno)
     * @param cuerpo estructura a enviar (Map, List, etc.)
     */
    private void responderJson(HttpExchange exchange, int codigo, Object cuerpo) throws IOException {
        String json = JsonEscritor.escribir(cuerpo);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Envia una respuesta de texto plano con el codigo HTTP indicado. */
    private void responderTexto(HttpExchange exchange, int codigo, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Lee completo un flujo de datos en bloques de 8 KB y lo devuelve como arreglo de bytes. */
    private static byte[] leerTodo(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] datos = new byte[8192];
        int leidos;
        while ((leidos = is.read(datos)) != -1) {
            buffer.write(datos, 0, leidos);
        }
        return buffer.toByteArray();
    }

    /**
     * Separa la parte de la URL despues de "?" en pares clave -&gt; valor.
     * Por ejemplo {@code idA=1&idB=2} da {idA=1, idB=2}. Decodifica
     * caracteres especiales (%20, +) y descarta pares sin "=".
     */
    private static Map<String, String> parsearQuery(String query) {
        Map<String, String> resultado = new HashMap<>();
        if (query == null || query.isBlank()) return resultado;
        for (String par : query.split("&")) {
            int igual = par.indexOf('=');
            if (igual == -1) continue;
            String clave = URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8);
            resultado.put(clave, valor);
        }
        return resultado;
    }
}
