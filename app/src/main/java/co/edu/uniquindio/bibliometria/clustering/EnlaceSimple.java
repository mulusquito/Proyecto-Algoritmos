package co.edu.uniquindio.bibliometria.clustering;

/**
 * Enlace simple (Single Linkage): la distancia entre dos clusteres es la
 * distancia MINIMA entre cualquier par de elementos, uno de cada cluster.
 * Formula de actualizacion de Lance-Williams:
 * <pre> d(I∪J, M) = min( d(I,M), d(J,M) ) </pre>
 * Tiende a formar cadenas largas de clusteres (efecto "encadenamiento").
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class EnlaceSimple implements EstrategiaEnlace {

    /** Distancia al nuevo cluster = la menor de las dos distancias previas (los tamanos no influyen). */
    @Override
    public double distanciaFusion(double distanciaIM, double distanciaJM, int tamanoI, int tamanoJ, int tamanoM) {
        return Math.min(distanciaIM, distanciaJM);
    }

    /** @return nombre del criterio para mostrar en la interfaz */
    @Override
    public String getNombre() {
        return "Single Linkage (enlace simple)";
    }
}
