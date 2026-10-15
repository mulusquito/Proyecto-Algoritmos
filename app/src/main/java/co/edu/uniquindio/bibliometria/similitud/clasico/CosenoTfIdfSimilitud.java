package co.edu.uniquindio.bibliometria.similitud.clasico;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Algoritmo clasico de similitud basado en modelo de espacio vectorial:
 * <b>Similitud del Coseno sobre vectores TF-IDF</b>.
 * <p>
 * Cada abstract se representa como un vector en un espacio de tantas
 * dimensiones como terminos distintos existan en el corpus (ver
 * {@link TfIdfVectorizador}), y se mide el coseno del angulo que forman
 * los dos vectores. Dos documentos identicos en composicion de vocabulario
 * (ponderado por relevancia) producen un angulo de 0 grados
 * (similitud = 1); dos documentos sin ningun termino en comun producen un
 * angulo de 90 grados (similitud = 0).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class CosenoTfIdfSimilitud {

    /** Nombre del algoritmo que se muestra en la interfaz. */
    public static final String NOMBRE = "Similitud del Coseno (TF-IDF)";

    /** Vectorizador ya entrenado con los 20 abstracts. */
    private final TfIdfVectorizador vectorizador;

    /**
     * @param vectorizador vectorizador TF-IDF ya entrenado sobre el corpus completo de 20 abstracts
     */
    public CosenoTfIdfSimilitud(TfIdfVectorizador vectorizador) {
        this.vectorizador = vectorizador;
    }

    /**
     * Calcula la similitud del coseno TF-IDF entre dos abstracts.
     *
     * @param tokensA tokens estemizados (sin stopwords) del primer abstract
     * @param tokensB tokens estemizados (sin stopwords) del segundo abstract
     * @return resultado con el coseno en [-1,1] y el detalle de normas, angulo y terminos mas relevantes
     */
    public ResultadoSimilitud calcular(List<String> tokensA, List<String> tokensB) {
        Map<String, Double> vectorA = vectorizador.vectorizar(tokensA);
        Map<String, Double> vectorB = vectorizador.vectorizar(tokensB);

        double productoPunto = TfIdfVectorizador.productoPunto(vectorA, vectorB);
        double normaA = TfIdfVectorizador.norma(vectorA);
        double normaB = TfIdfVectorizador.norma(vectorB);
        double coseno = (normaA == 0 || normaB == 0) ? 0.0 : productoPunto / (normaA * normaB);
        // Se recorta a [-1, 1] porque el redondeo puede dar 1.0000000001 y acos fallaria.
        coseno = Math.max(-1.0, Math.min(1.0, coseno));
        double anguloGrados = Math.toDegrees(Math.acos(coseno));

        // Top-10 terminos con mayor peso TF-IDF de cada documento, utiles
        // para mostrar el "calculo de los pesos" exigido en la documentacion.
        Map<String, Double> topA = topTerminos(vectorA, 10);
        Map<String, Double> topB = topTerminos(vectorB, 10);

        return new ResultadoSimilitud(NOMBRE, "clasico", coseno)
                .conDetalle("productoPunto", productoPunto)
                .conDetalle("normaVectorA", normaA)
                .conDetalle("normaVectorB", normaB)
                .conDetalle("anguloGrados", anguloGrados)
                .conDetalle("dimensionesVectorA", vectorA.size())
                .conDetalle("dimensionesVectorB", vectorB.size())
                .conDetalle("topTerminosA", topA)
                .conDetalle("topTerminosB", topB)
                .conDetalle("formula", "cos(theta) = (A . B) / (||A|| * ||B||)");
    }

    /**
     * Devuelve los n terminos de mayor peso del vector, de mayor a menor (a
     * igual peso, en orden alfabetico). Solo se usa para mostrar el detalle
     * en la interfaz, no para calcular la similitud.
     */
    private static Map<String, Double> topTerminos(Map<String, Double> vector, int n) {
        List<Map.Entry<String, Double>> entradas = new java.util.ArrayList<>(vector.entrySet());
        entradas.sort((e1, e2) -> {
            int cmp = Double.compare(e2.getValue(), e1.getValue());
            return cmp != 0 ? cmp : e1.getKey().compareTo(e2.getKey());
        });
        Map<String, Double> resultado = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(n, entradas.size()); i++) {
            resultado.put(entradas.get(i).getKey(), entradas.get(i).getValue());
        }
        return resultado;
    }
}
