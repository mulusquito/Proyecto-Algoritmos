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
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class TfIdfVectorizador {

    /** Valor IDF de cada termino del corpus. */
    private final Map<String, Double> idf = new HashMap<>();
    /** N: numero de documentos con que se entreno el vectorizador. */
    private final int numeroDocumentos;

    /**
     * Entrena el vectorizador: calcula el IDF de cada termino a partir de
     * todos los documentos del corpus.
     *
     * @param corpusTokenizado lista de documentos, cada uno como lista de tokens
     */
    public TfIdfVectorizador(List<List<String>> corpusTokenizado) {
        this.numeroDocumentos = corpusTokenizado.size();
        Map<String, Integer> frecuenciaDocumental = new HashMap<>();

        // df(t): en cuantos documentos aparece t. El Set evita contar dos veces
        // un termino repetido dentro del mismo documento.
        for (List<String> documento : corpusTokenizado) {
            Set<String> terminosUnicos = new HashSet<>(documento);
            for (String termino : terminosUnicos) {
                frecuenciaDocumental.merge(termino, 1, Integer::sum);
            }
        }

        // idf(t) = ln((1 + N) / (1 + df)) + 1: los terminos raros pesan mas que los comunes.
        for (Map.Entry<String, Integer> entrada : frecuenciaDocumental.entrySet()) {
            double df = entrada.getValue();
            double valorIdf = Math.log((1.0 + numeroDocumentos) / (1.0 + df)) + 1.0;
            idf.put(entrada.getKey(), valorIdf);
        }
    }

    /** Calcula el vector TF-IDF (disperso, representado como mapa termino-&gt;peso) de un documento. */
    public Map<String, Double> vectorizar(List<String> tokensDocumento) {
        // Cuantas veces aparece cada termino en este documento.
        Map<String, Integer> conteos = new HashMap<>();
        for (String t : tokensDocumento) {
            conteos.merge(t, 1, Integer::sum);
        }
        // Max(1, ...) evita dividir por cero si el documento quedo vacio.
        int totalTokens = Math.max(1, tokensDocumento.size());

        // Peso de cada termino = tf * idf.
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

    /** @return N, el numero de documentos con que se entreno */
    public int getNumeroDocumentos() {
        return numeroDocumentos;
    }

    /** @return el IDF de cada termino del corpus */
    public Map<String, Double> getIdf() {
        return idf;
    }

    /**
     * Similitud del coseno entre dos vectores dispersos:
     * cos(theta) = (A . B) / (||A|| * ||B||)
     *
     * @return valor en [0, 1] para vectores TF-IDF (pesos no negativos); 0 si algun vector esta vacio
     */
    public static double similitudCoseno(Map<String, Double> a, Map<String, Double> b) {
        // Producto punto: solo suman los terminos que estan en ambos vectores.
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

    /**
     * Norma euclidiana (longitud) de un vector: ||v|| = raiz(suma de v_i^2).
     *
     * @return longitud del vector
     */
    public static double norma(Map<String, Double> vector) {
        double sumaCuadrados = 0.0;
        for (double v : vector.values()) {
            sumaCuadrados += v * v;
        }
        return Math.sqrt(sumaCuadrados);
    }

    /**
     * Producto punto A . B = suma de a_t * b_t sobre los terminos t comunes a
     * ambos vectores (los terminos ausentes valen 0 y no aportan).
     *
     * @return producto punto de los dos vectores
     */
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
