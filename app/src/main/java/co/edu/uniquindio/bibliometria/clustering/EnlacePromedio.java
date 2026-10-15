package co.edu.uniquindio.bibliometria.clustering;

/**
 * Enlace promedio (Average Linkage - UPGMA, Unweighted Pair Group Method
 * with Arithmetic Mean): la distancia entre dos clusteres es el promedio
 * de las distancias entre todos los pares de elementos, uno de cada
 * cluster, ponderado por el tamano de los clusteres que se acaban de
 * fusionar. Formula de actualizacion de Lance-Williams:
 * <pre> d(I∪J, M) = ( |I|*d(I,M) + |J|*d(J,M) ) / (|I| + |J|) </pre>
 * Ofrece un balance intermedio entre el encadenamiento del enlace simple y
 * la rigidez del enlace completo.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class EnlacePromedio implements EstrategiaEnlace {

    @Override
    public double distanciaFusion(double distanciaIM, double distanciaJM, int tamanoI, int tamanoJ, int tamanoM) {
        return (tamanoI * distanciaIM + tamanoJ * distanciaJM) / (double) (tamanoI + tamanoJ);
    }

    @Override
    public String getNombre() {
        return "Average Linkage (enlace promedio, UPGMA)";
    }
}
