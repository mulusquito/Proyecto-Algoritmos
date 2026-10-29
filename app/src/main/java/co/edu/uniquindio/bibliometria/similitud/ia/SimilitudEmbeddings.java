package co.edu.uniquindio.bibliometria.similitud.ia;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.VectorizadorEmbeddings.VectorDocumento;

import java.util.List;

/**
 * Algoritmo de similitud basado en Inteligencia Artificial: compara dos
 * abstracts a traves de sus <b>embeddings</b> (representaciones vectoriales
 * densas) generados con un modelo preentrenado.
 * <p>
 * El proyecto crea dos instancias de esta clase, una por cada <b>enfoque de
 * generacion de embeddings</b> que pide el enunciado:
 * <ol>
 *     <li><b>GloVe</b> 100d: vectores obtenidos factorizando la matriz global
 *     de co-ocurrencias de palabras.</li>
 *     <li><b>FastText</b> 300d: vectores obtenidos con un modelo predictivo
 *     (skip-gram) que ademas aprende vectores de n-gramas de caracteres.</li>
 * </ol>
 * En ambos casos el vector del abstract es el promedio de los vectores de
 * sus palabras ({@link VectorizadorEmbeddings}) y la similitud metrica entre
 * los dos vectores se calcula con:
 * <pre>
 * cos(theta) = (A . B) / (||A|| * ||B||)            (similitud principal, en [-1, 1])
 * dist(A,B)  = sqrt( sum (A_i - B_i)^2 )             (distancia euclidiana, en el detalle)
 * </pre>
 * Todo el calculo (producto punto, normas, suma de cuadrados) se hace con
 * un solo recorrido de las d componentes de los vectores, sin usar ninguna
 * libreria de algebra lineal.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class SimilitudEmbeddings {

    /** Nombre del algoritmo que se muestra en la interfaz. */
    private final String nombre;
    /** Convierte cada abstract en su vector promedio de embeddings del modelo. */
    private final VectorizadorEmbeddings vectorizador;

    /**
     * @param nombre       nombre del algoritmo (incluye el modelo)
     * @param vectorizador convertidor de tokens a vector promedio con el modelo elegido
     */
    public SimilitudEmbeddings(String nombre, VectorizadorEmbeddings vectorizador) {
        this.nombre = nombre;
        this.vectorizador = vectorizador;
    }

    /** Algoritmo de IA basado en GloVe. */
    public static SimilitudEmbeddings conGlove(RepositorioEmbeddings glove) {
        return new SimilitudEmbeddings("Embeddings GloVe + Coseno (IA)", new VectorizadorEmbeddings(glove));
    }

    /** Algoritmo de IA basado en FastText. */
    public static SimilitudEmbeddings conFastText(RepositorioEmbeddings fastText) {
        return new SimilitudEmbeddings("Embeddings FastText + Coseno (IA)", new VectorizadorEmbeddings(fastText));
    }

    public String getNombre() {
        return nombre;
    }

    public VectorizadorEmbeddings getVectorizador() {
        return vectorizador;
    }

    /**
     * Calcula la similitud del coseno (y la distancia euclidiana) entre los
     * vectores de embeddings de dos abstracts.
     *
     * @param tokensA tokens sin estemizar (ver {@link co.edu.uniquindio.bibliometria.texto.Preprocesador#procesarParaEmbeddings}) del primer abstract
     * @param tokensB tokens sin estemizar del segundo abstract
     * @return resultado con el coseno en [-1,1] y el detalle del calculo
     */
    public ResultadoSimilitud calcular(List<String> tokensA, List<String> tokensB) {
        VectorDocumento va = vectorizador.vectorizar(tokensA);
        VectorDocumento vb = vectorizador.vectorizar(tokensB);

        // Un solo recorrido de las d componentes calcula el producto punto, las dos normas
        // y la suma de cuadrados de las diferencias.
        double productoPunto = 0.0;
        double normaA = 0.0;
        double normaB = 0.0;
        double sumaCuadrados = 0.0;
        for (int i = 0; i < va.vector.length; i++) {
            productoPunto += va.vector[i] * vb.vector[i];
            normaA += va.vector[i] * va.vector[i];
            normaB += vb.vector[i] * vb.vector[i];
            double diferencia = va.vector[i] - vb.vector[i];
            sumaCuadrados += diferencia * diferencia;
        }
        normaA = Math.sqrt(normaA);
        normaB = Math.sqrt(normaB);
        double distancia = Math.sqrt(sumaCuadrados);

        double coseno = (normaA == 0 || normaB == 0) ? 0.0 : productoPunto / (normaA * normaB);
        // Se recorta a [-1, 1] porque el redondeo puede dar 1.0000000001 y acos fallaria.
        coseno = Math.max(-1.0, Math.min(1.0, coseno));

        RepositorioEmbeddings modelo = vectorizador.getRepositorio();
        return new ResultadoSimilitud(nombre, "ia", coseno)
                .conDetalle("modeloEmbeddings", modelo.getNombreModelo() + ", filtrado al vocabulario del corpus")
                .conDetalle("dimensiones", modelo.getDimensiones())
                .conDetalle("palabrasEncontradasA", va.palabrasEncontradas + "/" + va.palabrasTotales)
                .conDetalle("palabrasEncontradasB", vb.palabrasEncontradas + "/" + vb.palabrasTotales)
                .conDetalle("productoPunto", productoPunto)
                .conDetalle("normaVectorA", normaA)
                .conDetalle("normaVectorB", normaB)
                .conDetalle("anguloGrados", Math.toDegrees(Math.acos(coseno)))
                .conDetalle("distanciaEuclidiana", distancia)
                .conDetalle("formula", "cos(theta) = (A . B) / (||A|| * ||B||);  dist(A,B) = sqrt(sum (Ai-Bi)^2);"
                        + "  A y B = promedio de los embeddings de las palabras");
    }
}
