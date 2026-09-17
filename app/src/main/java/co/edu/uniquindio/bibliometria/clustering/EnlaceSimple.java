package co.edu.uniquindio.bibliometria.clustering;

/**
 * Enlace simple (Single Linkage): la distancia entre dos clusteres es la
 * distancia MINIMA entre cualquier par de elementos, uno de cada cluster.
 * Formula de actualizacion de Lance-Williams:
 * <pre> d(I∪J, M) = min( d(I,M), d(J,M) ) </pre>
 * Tiende a formar cadenas largas de clusteres (efecto "encadenamiento").
 */
public class EnlaceSimple implements EstrategiaEnlace {

    @Override
    public double distanciaFusion(double distanciaIM, double distanciaJM, int tamanoI, int tamanoJ, int tamanoM) {
        return Math.min(distanciaIM, distanciaJM);
    }

    @Override
    public String getNombre() {
        return "Single Linkage (enlace simple)";
    }
}
