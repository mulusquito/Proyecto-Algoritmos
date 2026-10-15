package co.edu.uniquindio.bibliometria.similitud.ia;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.VectorizadorEmbeddings.VectorDocumento;

import java.util.List;

/**
 * Segundo algoritmo de similitud basado en Inteligencia Artificial:
 * distancia euclidiana entre los vectores de embeddings de dos abstracts.
 * <p>
 * La distancia euclidiana entre dos vectores A y B de d dimensiones es:
 * <pre>
 * dist(A,B) = sqrt( sum_{i=1}^{d} (A_i - B_i)^2 )
 * </pre>
 * A menor distancia, mayor similitud semantica. Para poder comparar este
 * algoritmo con los otros 5 (todos expresados como una similitud en
 * [0,1], donde 1 = identico), la distancia se transforma con
 * {@code similitud = 1 / (1 + distancia)}, conservando la distancia bruta
 * en el detalle del resultado para el analisis critico solicitado por el
 * proyecto.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class EuclidianaEmbeddingSimilitud {

    public static final String NOMBRE = "Distancia Euclidiana sobre Embeddings (IA)";

    private final VectorizadorEmbeddings vectorizador;

    /**
     * @param vectorizador convertidor de tokens a vector promedio de embeddings
     */
    public EuclidianaEmbeddingSimilitud(VectorizadorEmbeddings vectorizador) {
        this.vectorizador = vectorizador;
    }

    /**
     * Calcula la distancia euclidiana (transformada a similitud) entre los embeddings de dos abstracts.
     *
     * @param tokensA tokens sin estemizar del primer abstract
     * @param tokensB tokens sin estemizar del segundo abstract
     * @return resultado con la similitud derivada en [0,1] y la distancia euclidiana bruta
     */
    public ResultadoSimilitud calcular(List<String> tokensA, List<String> tokensB) {
        VectorDocumento va = vectorizador.vectorizar(tokensA);
        VectorDocumento vb = vectorizador.vectorizar(tokensB);

        double sumaCuadrados = 0.0;
        for (int i = 0; i < va.vector.length; i++) {
            double diferencia = va.vector[i] - vb.vector[i];
            sumaCuadrados += diferencia * diferencia;
        }
        double distancia = Math.sqrt(sumaCuadrados);
        double similitud = 1.0 / (1.0 + distancia);

        return new ResultadoSimilitud(NOMBRE, "ia", similitud)
                .conDetalle("modeloEmbeddings", "GloVe 100d (Wikipedia + Gigaword), filtrado al vocabulario del corpus")
                .conDetalle("palabrasEncontradasA", va.palabrasEncontradas + "/" + va.palabrasTotales)
                .conDetalle("palabrasEncontradasB", vb.palabrasEncontradas + "/" + vb.palabrasTotales)
                .conDetalle("distanciaEuclidiana", distancia)
                .conDetalle("formula", "dist(A,B) = sqrt(sum (Ai-Bi)^2);  similitud = 1 / (1 + dist)");
    }
}
