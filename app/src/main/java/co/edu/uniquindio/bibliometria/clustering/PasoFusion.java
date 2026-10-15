package co.edu.uniquindio.bibliometria.clustering;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registra una fusion de dos clusteres durante el algoritmo aglomerativo (una fila del "linkage matrix" clasico).  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class PasoFusion {
    /** Id del primer cluster que se fusiona. */
    public final int clusterA;
    /** Id del segundo cluster que se fusiona. */
    public final int clusterB;
    /** Distancia entre los dos clusteres en el momento de fusionarlos. */
    public final double altura;
    /** Id que recibe el cluster resultante de la fusion. */
    public final int nuevoClusterId;
    /** Numero de articulos que contiene el cluster resultante. */
    public final int tamanoResultante;

    /** Registra una fusion con todos sus datos. */
    public PasoFusion(int clusterA, int clusterB, double altura, int nuevoClusterId, int tamanoResultante) {
        this.clusterA = clusterA;
        this.clusterB = clusterB;
        this.altura = altura;
        this.nuevoClusterId = nuevoClusterId;
        this.tamanoResultante = tamanoResultante;
    }

    /** @return la fusion como mapa clave -> valor, listo para convertir a JSON */
    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("clusterA", clusterA);
        m.put("clusterB", clusterB);
        m.put("altura", altura);
        m.put("nuevoClusterId", nuevoClusterId);
        m.put("tamanoResultante", tamanoResultante);
        return m;
    }
}
