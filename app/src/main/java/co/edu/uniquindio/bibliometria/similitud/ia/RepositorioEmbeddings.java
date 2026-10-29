package co.edu.uniquindio.bibliometria.similitud.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga en memoria un modelo de embeddings de palabras preentrenado. El
 * proyecto usa dos modelos distintos, uno por cada algoritmo de similitud
 * basado en Inteligencia Artificial:
 * <ul>
 *     <li><b>GloVe</b> 100 dimensiones (Pennington et al., 2014), entrenado
 *     sobre Wikipedia + Gigaword a partir de la matriz global de
 *     co-ocurrencias de palabras.</li>
 *     <li><b>FastText</b> 300 dimensiones (Mikolov et al., 2018),
 *     {@code wiki-news-300d-1M}, entrenado sobre Wikipedia 2017 + UMBC +
 *     noticias con un modelo predictivo (skip-gram) que incorpora
 *     informacion de subpalabras (n-gramas de caracteres).</li>
 * </ul>
 * Ambos archivos fueron filtrados previamente para conservar solo las
 * palabras que aparecen en los 20 abstracts (ver {@code extraction/} y
 * {@code FiltrarEmbeddingsFastText} en la carpeta de pruebas), de modo que
 * el recurso embebido en la aplicacion pesa pocos MB en lugar de cientos.
 * <p>
 * El uso de modelos preentrenados para generar "representaciones
 * vectoriales densas" (embeddings) es lo que el enunciado del proyecto
 * habilita para los dos algoritmos con IA. El calculo de la similitud
 * (coseno y distancia euclidiana) sobre esos vectores se implementa
 * manualmente, sin usar ninguna libreria de similitud.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class RepositorioEmbeddings {

    /** Nombre legible del modelo (para la interfaz y el documento). */
    private final String nombreModelo;
    /** Ruta, dentro de los recursos de la aplicacion, del archivo de vectores. */
    private final String recurso;
    /** Numero de componentes de cada vector de palabra. */
    private final int dimensiones;

    /** Vector de cada palabra del vocabulario. */
    private final Map<String, double[]> vectores = new HashMap<>();

    /** Modelo GloVe 100d filtrado al vocabulario del corpus. */
    public static RepositorioEmbeddings glove() {
        return new RepositorioEmbeddings("GloVe 100d (Wikipedia + Gigaword)",
                "/embeddings/glove100_filtrado.tsv", 100);
    }

    /** Modelo FastText wiki-news-300d-1M filtrado al vocabulario del corpus. */
    public static RepositorioEmbeddings fastText() {
        return new RepositorioEmbeddings("FastText 300d (wiki-news-300d-1M)",
                "/embeddings/fasttext300_filtrado.tsv", 300);
    }

    /**
     * Crea el repositorio y carga de inmediato todos los vectores en memoria.
     *
     * @param nombreModelo nombre legible del modelo
     * @param recurso      ruta del archivo de vectores dentro de los recursos
     * @param dimensiones  componentes que debe tener cada vector
     */
    public RepositorioEmbeddings(String nombreModelo, String recurso, int dimensiones) {
        this.nombreModelo = nombreModelo;
        this.recurso = recurso;
        this.dimensiones = dimensiones;
        cargar();
    }

    /**
     * Lee el archivo de vectores linea por linea. Cada linea tiene el formato
     * {@code palabra<TAB>v1 v2 ... vd}: se separa la palabra de los numeros
     * y estos se convierten a un arreglo de doubles.
     *
     * @throws IllegalStateException si el archivo no existe, no se puede leer
     *                               o algun vector no tiene las dimensiones esperadas
     */
    private void cargar() {
        try (InputStream is = RepositorioEmbeddings.class.getResourceAsStream(recurso)) {
            if (is == null) {
                throw new IllegalStateException("No se encontro el recurso en el classpath: " + recurso);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.isBlank()) continue;
                    int tab = linea.indexOf('\t');
                    String palabra = linea.substring(0, tab);
                    String[] partes = linea.substring(tab + 1).trim().split(" ");
                    if (partes.length != dimensiones) {
                        throw new IllegalStateException("El vector de '" + palabra + "' en " + recurso + " tiene "
                                + partes.length + " componentes; se esperaban " + dimensiones);
                    }
                    double[] vector = new double[dimensiones];
                    for (int i = 0; i < dimensiones; i++) {
                        vector[i] = Double.parseDouble(partes[i]);
                    }
                    vectores.put(palabra, vector);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el recurso de embeddings: " + recurso, e);
        }
    }

    /** @return el vector de la palabra, o null si no esta en el vocabulario */
    public double[] obtenerVector(String palabra) {
        return vectores.get(palabra);
    }

    /** @return true si la palabra tiene vector en el modelo */
    public boolean contiene(String palabra) {
        return vectores.containsKey(palabra);
    }

    /** @return numero de palabras cargadas */
    public int tamano() {
        return vectores.size();
    }

    /** @return numero de componentes de cada vector */
    public int getDimensiones() {
        return dimensiones;
    }

    /** @return nombre legible del modelo */
    public String getNombreModelo() {
        return nombreModelo;
    }
}
