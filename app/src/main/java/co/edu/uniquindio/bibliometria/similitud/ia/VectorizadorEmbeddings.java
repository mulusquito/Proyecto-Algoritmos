package co.edu.uniquindio.bibliometria.similitud.ia;

import java.util.List;

/**
 * Convierte una lista de tokens (abstract preprocesado) en un unico vector
 * denso, promediando los embeddings preentrenados de cada palabra que se
 * encuentre en el {@link RepositorioEmbeddings}. Esta tecnica se conoce
 * como "average word embeddings" y es una forma sencilla y ampliamente
 * usada de obtener una representacion vectorial de un documento completo a
 * partir de vectores de palabras individuales.
 * <p>
 * El promedio (suma componente a componente dividida entre el numero de
 * palabras encontradas) se implementa manualmente con operaciones
 * aritmeticas basicas sobre arreglos, sin usar ninguna libreria de
 * algebra lineal.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class VectorizadorEmbeddings {

    private final RepositorioEmbeddings repositorio;

    public VectorizadorEmbeddings(RepositorioEmbeddings repositorio) {
        this.repositorio = repositorio;
    }

    public static class VectorDocumento {
        public final double[] vector;
        public final int palabrasEncontradas;
        public final int palabrasTotales;

        VectorDocumento(double[] vector, int encontradas, int totales) {
            this.vector = vector;
            this.palabrasEncontradas = encontradas;
            this.palabrasTotales = totales;
        }
    }

    /**
     * Promedia los vectores de embedding de las palabras de un documento.
     *
     * @param tokens tokens del documento (sin estemizar, para hacer match con el vocabulario de GloVe)
     * @return vector promedio junto con estadisticas de cobertura (palabras encontradas / totales)
     */
    public VectorDocumento vectorizar(List<String> tokens) {
        double[] suma = new double[RepositorioEmbeddings.DIMENSIONES];
        int encontradas = 0;
        for (String token : tokens) {
            double[] v = repositorio.obtenerVector(token);
            if (v != null) {
                for (int i = 0; i < suma.length; i++) {
                    suma[i] += v[i];
                }
                encontradas++;
            }
        }
        if (encontradas > 0) {
            for (int i = 0; i < suma.length; i++) {
                suma[i] /= encontradas;
            }
        }
        return new VectorDocumento(suma, encontradas, tokens.size());
    }
}
