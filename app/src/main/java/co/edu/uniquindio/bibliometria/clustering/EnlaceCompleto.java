package co.edu.uniquindio.bibliometria.clustering;

/**
 * Enlace completo (Complete Linkage): la distancia entre dos clusteres es la
 * distancia MAXIMA entre cualquier par de elementos, uno de cada cluster.
 * Formula de actualizacion de Lance-Williams:
 * <pre> d(I∪J, M) = max( d(I,M), d(J,M) ) </pre>
 * Tiende a formar clusteres compactos y de tamano similar.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class EnlaceCompleto implements EstrategiaEnlace {

    @Override
    public double distanciaFusion(double distanciaIM, double distanciaJM, int tamanoI, int tamanoJ, int tamanoM) {
        return Math.max(distanciaIM, distanciaJM);
    }

    @Override
    public String getNombre() {
        return "Complete Linkage (enlace completo)";
    }
}
