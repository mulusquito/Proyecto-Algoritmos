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
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class VectorizadorEmbeddings {

    /** Modelo de donde se obtiene el vector de cada palabra. */
    private final RepositorioEmbeddings repositorio;

    /** @param repositorio modelo de embeddings ya cargado */
    public VectorizadorEmbeddings(RepositorioEmbeddings repositorio) {
        this.repositorio = repositorio;
    }

    /** @return el modelo de embeddings que usa este vectorizador */
    public RepositorioEmbeddings getRepositorio() {
        return repositorio;
    }

    /** Vector de un documento completo junto con su cobertura de vocabulario. */
    public static class VectorDocumento {
        /** Promedio de los vectores de las palabras encontradas (tantas componentes como el modelo). */
        public final double[] vector;
        /** Cuantas palabras del documento tenian vector en el modelo. */
        public final int palabrasEncontradas;
        /** Cuantas palabras tenia el documento en total. */
        public final int palabrasTotales;

        /** Agrupa el vector y las dos cifras de cobertura. */
        VectorDocumento(double[] vector, int encontradas, int totales) {
            this.vector = vector;
            this.palabrasEncontradas = encontradas;
            this.palabrasTotales = totales;
        }
    }

    /**
     * Promedia los vectores de embedding de las palabras de un documento.
     *
     * @param tokens tokens del documento (sin estemizar, para hacer match con el vocabulario del modelo)
     * @return vector promedio junto con estadisticas de cobertura (palabras encontradas / totales)
     */
    public VectorDocumento vectorizar(List<String> tokens) {
        // 1. Sumar componente a componente los vectores de las palabras que existen en el modelo.
        double[] suma = new double[repositorio.getDimensiones()];
        int encontradas = 0;
        for (String token : tokens) {
            double[] v = repositorio.obtenerVector(token);
            // Las palabras que no estan en el modelo se ignoran.
            if (v != null) {
                for (int i = 0; i < suma.length; i++) {
                    suma[i] += v[i];
                }
                encontradas++;
            }
        }
        // 2. Dividir entre el numero de palabras encontradas para obtener el promedio
        //    (si no se encontro ninguna, queda el vector cero).
        if (encontradas > 0) {
            for (int i = 0; i < suma.length; i++) {
                suma[i] /= encontradas;
            }
        }
        return new VectorDocumento(suma, encontradas, tokens.size());
    }
}
