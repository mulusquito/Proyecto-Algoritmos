package co.edu.uniquindio.bibliometria.similitud.ia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga en memoria un modelo de embeddings preentrenado (GloVe 100
 * dimensiones, entrenado sobre Wikipedia + Gigaword) que fue previamente
 * filtrado para conservar unicamente las palabras que aparecen en el
 * corpus de 20 abstracts (ver script de preparacion de datos en
 * {@code /extraction}), de modo que el recurso embebido en la aplicacion
 * pese menos de 1 MB en lugar de los cientos de MB del modelo completo.
 * <p>
 * El uso de un modelo de lenguaje preentrenado para generar
 * "representaciones vectoriales densas" (embeddings) corresponde
 * exactamente a lo que el enunciado del proyecto habilita para los dos
 * algoritmos de similitud basados en Inteligencia Artificial. El calculo
 * de la similitud (coseno y distancia euclidiana) sobre esos vectores se
 * implementa manualmente, sin usar ninguna libreria de similitud.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class RepositorioEmbeddings {

    /** Ruta, dentro de los recursos de la aplicacion, del archivo de vectores. */
    private static final String RECURSO = "/embeddings/glove100_filtrado.tsv";
    /** Numero de componentes de cada vector de palabra (GloVe 100d). */
    public static final int DIMENSIONES = 100;

    /** Vector de cada palabra del vocabulario. */
    private final Map<String, double[]> vectores = new HashMap<>();

    /** Crea el repositorio y carga de inmediato todos los vectores en memoria. */
    public RepositorioEmbeddings() {
        cargar();
    }

    /**
     * Lee el archivo de vectores linea por linea. Cada linea tiene el formato
     * {@code palabra<TAB>v1 v2 ... v100}: se separa la palabra de los numeros
     * y estos se convierten a un arreglo de doubles.
     *
     * @throws IllegalStateException si el archivo no existe o no se puede leer
     */
    private void cargar() {
        try (InputStream is = RepositorioEmbeddings.class.getResourceAsStream(RECURSO)) {
            if (is == null) {
                throw new IllegalStateException("No se encontro el recurso en el classpath: " + RECURSO);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.isBlank()) continue;
                    int tab = linea.indexOf('\t');
                    String palabra = linea.substring(0, tab);
                    String[] partes = linea.substring(tab + 1).split(" ");
                    double[] vector = new double[partes.length];
                    for (int i = 0; i < partes.length; i++) {
                        vector[i] = Double.parseDouble(partes[i]);
                    }
                    vectores.put(palabra, vector);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el recurso de embeddings: " + RECURSO, e);
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
}
