package co.edu.uniquindio.bibliometria.clustering;

/**
 * Estrategia de enlace ("linkage") usada por el algoritmo de clustering
 * jerarquico aglomerativo para decidir la distancia entre un cluster recien
 * formado (fusion de los clusteres I y J) y cualquier otro cluster M
 * existente, a partir de las distancias ya conocidas d(I,M) y d(J,M).
 * <p>
 * Cada implementacion corresponde a uno de los 3 criterios de agrupamiento
 * jerarquico exigidos por el Requerimiento 2 del proyecto.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public interface EstrategiaEnlace {

    /**
     * @param distanciaIM distancia entre el cluster I (antes de fusionarse) y M
     * @param distanciaJM distancia entre el cluster J (antes de fusionarse) y M
     * @param tamanoI     numero de elementos originales en el cluster I
     * @param tamanoJ     numero de elementos originales en el cluster J
     * @param tamanoM     numero de elementos originales en el cluster M
     * @return distancia entre el nuevo cluster (I unido con J) y M
     */
    double distanciaFusion(double distanciaIM, double distanciaJM, int tamanoI, int tamanoJ, int tamanoM);

    /** @return nombre legible del criterio de enlace (se muestra en la interfaz y en el JSON) */
    String getNombre();
}
