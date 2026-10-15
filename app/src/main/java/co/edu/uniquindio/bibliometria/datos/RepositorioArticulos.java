package co.edu.uniquindio.bibliometria.datos;

import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;
import co.edu.uniquindio.bibliometria.util.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fuente de datos del corpus: carga el archivo curado
 * {@code data/articulos.json} (generado a partir de los 20 PDFs originales,
 * ver carpeta {@code /extraction} del repositorio) y precalcula, para cada
 * articulo, los tokens preprocesados de su abstract, de modo que los
 * algoritmos de similitud y clustering no repitan ese trabajo en cada
 * peticion.
 * <p>
 * La lectura del JSON usa el parser propio {@link JsonParser} (ver esa
 * clase para la justificacion de por que no se usa Jackson/Gson).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class RepositorioArticulos {

    /** Ruta, dentro de los recursos de la aplicacion, del JSON con los 20 articulos. */
    private static final String RECURSO = "/data/articulos.json";

    /** Lista inmutable de los articulos, ordenada por id. */
    private List<Articulo> articulos;

    /** Crea el repositorio y carga de inmediato los 20 articulos desde el JSON. */
    public RepositorioArticulos() {
        cargar();
    }

    /**
     * Lee el JSON del corpus, construye un {@link Articulo} por cada entrada,
     * le precalcula sus dos listas de tokens (con y sin stemming) y deja la
     * lista ordenada por id y protegida contra modificaciones.
     */
    @SuppressWarnings("unchecked")
    private void cargar() {
        String contenido = leerRecursoClasspath(RECURSO);
        Object raiz = JsonParser.parsear(contenido);
        // El JSON es un arreglo de objetos: cada objeto es un articulo.
        List<Object> crudos = (List<Object>) raiz;

        List<Articulo> resultado = new ArrayList<>();
        for (Object o : crudos) {
            // Cada articulo llega como un mapa clave -> valor con sus campos.
            Map<String, Object> dto = (Map<String, Object>) o;
            int id = ((Number) dto.get("id")).intValue();
            String archivo = (String) dto.get("archivo");
            String titulo = (String) dto.get("titulo");
            String autores = (String) dto.get("autores");
            String resumen = (String) dto.get("abstract");

            Articulo articulo = new Articulo(id, archivo, titulo, autores, resumen);
            // Se preprocesa una sola vez aqui para que los algoritmos no lo repitan en cada peticion.
            articulo.setTokens(Preprocesador.procesarParaVectorial(resumen));
            articulo.setTokensEmbeddings(Preprocesador.procesarParaEmbeddings(resumen));
            resultado.add(articulo);
        }
        resultado.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        this.articulos = Collections.unmodifiableList(resultado);
    }

    /**
     * Lee completo un archivo empaquetado dentro de la aplicacion (classpath)
     * y lo devuelve como texto UTF-8. Lee en bloques de 8 KB hasta llegar al
     * final del archivo.
     *
     * @param ruta ruta del recurso, por ejemplo {@code /data/articulos.json}
     * @return contenido del archivo como texto
     * @throws IllegalStateException si el recurso no existe o no se puede leer
     */
    private static String leerRecursoClasspath(String ruta) {
        try (InputStream is = RepositorioArticulos.class.getResourceAsStream(ruta)) {
            if (is == null) {
                throw new IllegalStateException("No se encontro el recurso en el classpath: " + ruta);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] datos = new byte[8192];
            int leidos;
            while ((leidos = is.read(datos)) != -1) {
                buffer.write(datos, 0, leidos);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el recurso: " + ruta, e);
        }
    }

    /** @return los 20 articulos del corpus, ordenados por id (lista de solo lectura) */
    public List<Articulo> listarTodos() {
        return articulos;
    }

    /**
     * Busca un articulo por su numero.
     *
     * @param id numero del articulo (1 a 20)
     * @return el articulo, o un {@link Optional} vacio si no existe
     */
    public Optional<Articulo> buscarPorId(int id) {
        return articulos.stream().filter(a -> a.getId() == id).findFirst();
    }

    /** @return cantidad de articulos cargados */
    public int total() {
        return articulos.size();
    }
}
