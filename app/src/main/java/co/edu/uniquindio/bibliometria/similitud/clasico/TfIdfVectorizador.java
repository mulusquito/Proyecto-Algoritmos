package co.edu.uniquindio.bibliometria.similitud.clasico;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Construye representaciones vectoriales TF-IDF (Term Frequency - Inverse
 * Document Frequency) de un corpus de documentos, implementadas desde cero
 * (sin usar {@code CountVectorizer}, {@code TfidfVectorizer} ni ninguna
 * libreria equivalente).
 * <p>
 * Para un termino t y un documento d dentro de un corpus de N documentos:
 * <pre>
 * tf(t, d)  = (numero de ocurrencias de t en d) / (numero total de tokens de d)
 * idf(t)    = ln( (1 + N) / (1 + df(t)) ) + 1        (suavizado para evitar division por cero)
 * tfidf(t,d) = tf(t,d) * idf(t)
 * </pre>
 * donde df(t) es el numero de documentos del corpus que contienen al menos
 * una vez el termino t. El IDF se calcula una sola vez sobre todo el corpus
 * de 20 abstracts para que los pesos sean comparables entre cualquier par
 * de articulos que el usuario seleccione en la aplicacion.
 */
public class TfIdfVectorizador {

    private final Map<String, Double> idf = new HashMap<>();
    private final int numeroDocumentos;

    public TfIdfVectorizador(List<List<String>> corpusTokenizado) {
        this.numeroDocumentos = corpusTokenizado.size();
        Map<String, Integer> frecuenciaDocumental = new HashMap<>();

        for (List<String> documento : corpusTokenizado) {
            Set<String> terminosUnicos = new HashSet<>(documento);
            for (String termino : terminosUnicos) {
                frecuenciaDocumental.merge(termino, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entrada : frecuenciaDocumental.entrySet()) {
            double df = entrada.getValue();
            double valorIdf = Math.log((1.0 + numeroDocumentos) / (1.0 + df)) + 1.0;
            idf.put(entrada.getKey(), valorIdf);
        }
    }

    /** Calcula el vector TF-IDF (disperso, representado como mapa termino-&gt;peso) de un documento. */
    public Map<String, Double> vectorizar(List<String> tokensDocumento) {
        Map<String, Integer> conteos = new HashMap<>();
        for (String t : tokensDocumento) {
            conteos.merge(t, 1, Integer::sum);
        }
        int totalTokens = Math.max(1, tokensDocumento.size());

        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entrada : conteos.entrySet()) {
            String termino = entrada.getKey();
            double tf = entrada.getValue() / (double) totalTokens;
            // Si el termino no aparecio en el corpus de entrenamiento del IDF
            // (caso raro, corpus estatico y cerrado) se usa un IDF por defecto.
            double idfTermino = idf.getOrDefault(termino, Math.log(1.0 + numeroDocumentos) + 1.0);
            vector.put(termino, tf * idfTermino);
        }
        return vector;
    }

    public int getNumeroDocumentos() {
        return numeroDocumentos;
    }

    public Map<String, Double> getIdf() {
        return idf;
    }

    /**
     * Similitud del coseno entre dos vectores dispersos:
     * cos(theta) = (A . B) / (||A|| * ||B||)
     */
    public static double similitudCoseno(Map<String, Double> a, Map<String, Double> b) {
        double productoPunto = 0.0;
        for (Map.Entry<String, Double> entrada : a.entrySet()) {
            Double valorB = b.get(entrada.getKey());
            if (valorB != null) {
                productoPunto += entrada.getValue() * valorB;
            }
        }
        double normaA = norma(a);
        double normaB = norma(b);
        if (normaA == 0.0 || normaB == 0.0) {
            return 0.0;
        }
        return productoPunto / (normaA * normaB);
    }

    public static double norma(Map<String, Double> vector) {
        double sumaCuadrados = 0.0;
        for (double v : vector.values()) {
            sumaCuadrados += v * v;
        }
        return Math.sqrt(sumaCuadrados);
    }

    public static double productoPunto(Map<String, Double> a, Map<String, Double> b) {
        double producto = 0.0;
        for (Map.Entry<String, Double> entrada : a.entrySet()) {
            Double valorB = b.get(entrada.getKey());
            if (valorB != null) {
                producto += entrada.getValue() * valorB;
            }
        }
        return producto;
    }
}
