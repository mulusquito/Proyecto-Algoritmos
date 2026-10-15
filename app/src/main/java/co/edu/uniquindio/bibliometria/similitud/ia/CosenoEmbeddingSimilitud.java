package co.edu.uniquindio.bibliometria.similitud.ia;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.VectorizadorEmbeddings.VectorDocumento;

import java.util.List;

/**
 * Primer algoritmo de similitud basado en Inteligencia Artificial: similitud
 * del coseno entre los vectores de <b>embeddings</b> (promedio de vectores
 * de palabra preentrenados, ver {@link VectorizadorEmbeddings}) de dos
 * abstracts.
 * <p>
 * A diferencia de la similitud del coseno sobre TF-IDF (que solo detecta
 * coincidencias literales de palabras), los embeddings capturan relaciones
 * semanticas aprendidas de un gran corpus de texto: por ejemplo, palabras
 * como "teacher" y "instructor" tendran vectores cercanos aunque nunca
 * coincidan textualmente.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class CosenoEmbeddingSimilitud {

    /** Nombre del algoritmo que se muestra en la interfaz. */
    public static final String NOMBRE = "Similitud del Coseno sobre Embeddings (IA)";

    /** Convierte cada abstract en su vector promedio de embeddings. */
    private final VectorizadorEmbeddings vectorizador;

    /**
     * @param vectorizador convertidor de tokens a vector promedio de embeddings
     */
    public CosenoEmbeddingSimilitud(VectorizadorEmbeddings vectorizador) {
        this.vectorizador = vectorizador;
    }

    /**
     * Calcula la similitud del coseno entre los vectores de embeddings de dos abstracts.
     *
     * @param tokensA tokens sin estemizar (ver {@link co.edu.uniquindio.bibliometria.texto.Preprocesador#procesarParaEmbeddings}) del primer abstract
     * @param tokensB tokens sin estemizar del segundo abstract
     * @return resultado con el coseno en [-1,1] y el detalle de cobertura de vocabulario
     */
    public ResultadoSimilitud calcular(List<String> tokensA, List<String> tokensB) {
        VectorDocumento va = vectorizador.vectorizar(tokensA);
        VectorDocumento vb = vectorizador.vectorizar(tokensB);

        // Un solo recorrido de las 100 componentes calcula el producto punto y las dos normas.
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;
        for (int i = 0; i < va.vector.length; i++) {
            productoPunto += va.vector[i] * vb.vector[i];
            normaA += va.vector[i] * va.vector[i];
            normaB += vb.vector[i] * vb.vector[i];
        }
        normaA = Math.sqrt(normaA);
        normaB = Math.sqrt(normaB);

        double coseno = (normaA == 0 || normaB == 0) ? 0.0 : productoPunto / (normaA * normaB);
        // Se recorta a [-1, 1] porque el redondeo puede dar 1.0000000001 y acos fallaria.
        coseno = Math.max(-1.0, Math.min(1.0, coseno));

        return new ResultadoSimilitud(NOMBRE, "ia", coseno)
                .conDetalle("modeloEmbeddings", "GloVe 100d (Wikipedia + Gigaword), filtrado al vocabulario del corpus")
                .conDetalle("palabrasEncontradasA", va.palabrasEncontradas + "/" + va.palabrasTotales)
                .conDetalle("palabrasEncontradasB", vb.palabrasEncontradas + "/" + vb.palabrasTotales)
                .conDetalle("normaVectorA", normaA)
                .conDetalle("normaVectorB", normaB)
                .conDetalle("anguloGrados", Math.toDegrees(Math.acos(coseno)))
                .conDetalle("formula", "cos(theta) = (A . B) / (||A|| * ||B||), A y B = promedio de embeddings");
    }
}
