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

    private static final String RECURSO = "/embeddings/glove100_filtrado.tsv";
    public static final int DIMENSIONES = 100;

    private final Map<String, double[]> vectores = new HashMap<>();

    public RepositorioEmbeddings() {
        cargar();
    }

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

    public double[] obtenerVector(String palabra) {
        return vectores.get(palabra);
    }

    public boolean contiene(String palabra) {
        return vectores.containsKey(palabra);
    }

    public int tamano() {
        return vectores.size();
    }
}
