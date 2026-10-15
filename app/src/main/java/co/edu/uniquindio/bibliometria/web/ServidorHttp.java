package co.edu.uniquindio.bibliometria.web;

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
 *     <li>{@code GET /api/ranking?top=15&repeticiones=50} (Seguimiento: ranking de terminos)</li>
 *     <li>Archivos estaticos del frontend (carpeta {@code static/})</li>
 * </ul>
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ServidorHttp {

    private final HttpServer servidor;
    private final RepositorioArticulos repositorioArticulos;
    private final MotorSimilitud motorSimilitud;
    private final MotorClustering motorClustering;
    private final MotorRanking motorRanking;

    public ServidorHttp(int puerto, RepositorioArticulos repositorioArticulos,
                         MotorSimilitud motorSimilitud, MotorClustering motorClustering,
                         MotorRanking motorRanking) throws IOException {
        this.repositorioArticulos = repositorioArticulos;
        this.motorSimilitud = motorSimilitud;
        this.motorClustering = motorClustering;
        this.motorRanking = motorRanking;

        this.servidor = HttpServer.create(new InetSocketAddress(puerto), 0);
        servidor.createContext("/api/articulos", conManejoDeErrores(this::manejarArticulos));
        servidor.createContext("/api/similitud", conManejoDeErrores(this::manejarSimilitud));
        servidor.createContext("/api/clustering", conManejoDeErrores(this::manejarClustering));
        servidor.createContext("/api/ranking", conManejoDeErrores(this::manejarRanking));
        servidor.createContext("/",conManejoDeErrores(this::manejarEstatico));
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

    public void iniciar() {
        servidor.start();
    }

    // -----------------------------------------------------------------
    // Handlers de la API
    // -----------------------------------------------------------------

    private void manejarArticulos(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
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

    private void manejarSimilitud(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        Map<String, String> query = parsearQuery(exchange.getRequestURI().getRawQuery());
        try {
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
            responderJson(exchange, 400, Map.of("error", "Parametros invalidos. Uso: /api/similitud?idA=1&idB=2"));
        }
    }

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

    private void manejarRanking(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        Map<String, String> query = parsearQuery(exchange.getRequestURI().getRawQuery());
        try {
            int top = Integer.parseInt(query.getOrDefault("top", String.valueOf(MotorRanking.TOP_POR_DEFECTO)));
            int repeticiones = Integer.parseInt(query.getOrDefault("repeticiones",
                    String.valueOf(MotorRanking.REPETICIONES_POR_DEFECTO)));
            if (top < 1 || top > 100 || repeticiones < 1 || repeticiones > 500) {
                responderJson(exchange, 400, Map.of("error", "Rangos validos: top 1-100, repeticiones 1-500"));
                return;
            }
            responderJson(exchange, 200, motorRanking.ejecutarComoMapa(top, repeticiones));
        } catch (NumberFormatException e) {
            responderJson(exchange, 400, Map.of("error", "Parametros invalidos. Uso: /api/ranking?top=15&repeticiones=50"));
        }
    }

    // -----------------------------------------------------------------
    // Archivos estaticos (frontend)
    // -----------------------------------------------------------------

    private void manejarEstatico(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, Map.of("error", "Metodo no soportado"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
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

    private void responderTexto(HttpExchange exchange, int codigo, String texto) throws IOException {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static byte[] leerTodo(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] datos = new byte[8192];
        int leidos;
        while ((leidos = is.read(datos)) != -1) {
            buffer.write(datos, 0, leidos);
        }
        return buffer.toByteArray();
    }

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
