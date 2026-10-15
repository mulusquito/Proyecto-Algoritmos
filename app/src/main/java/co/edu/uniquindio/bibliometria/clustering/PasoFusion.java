package co.edu.uniquindio.bibliometria.clustering;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registra una fusion de dos clusteres durante el algoritmo aglomerativo (una fila del "linkage matrix" clasico).  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class PasoFusion {
    public final int clusterA;
    public final int clusterB;
    public final double altura;
    public final int nuevoClusterId;
    public final int tamanoResultante;

    public PasoFusion(int clusterA, int clusterB, double altura, int nuevoClusterId, int tamanoResultante) {
        this.clusterA = clusterA;
        this.clusterB = clusterB;
        this.altura = altura;
        this.nuevoClusterId = nuevoClusterId;
        this.tamanoResultante = tamanoResultante;
    }

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
