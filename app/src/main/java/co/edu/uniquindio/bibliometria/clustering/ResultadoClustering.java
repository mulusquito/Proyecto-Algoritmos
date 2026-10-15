package co.edu.uniquindio.bibliometria.clustering;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resultado completo de ejecutar un algoritmo de clustering jerarquico: el dendrograma, los pasos de fusion y su evaluacion.  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ResultadoClustering {

    /** Nombre del criterio de enlace usado (Single, Complete o Average Linkage). */
    private final String nombreEnlace;
    /** Raiz del dendrograma: el cluster final que contiene los 20 articulos. */
    private final NodoDendrograma raiz;
    /** Las n-1 fusiones en el orden en que ocurrieron. */
    private final List<PasoFusion> pasos;
    /** Correlacion cofenetica del dendrograma (se asigna despues de evaluarlo). */
    private double correlacionCofenetica;

    /** Crea el resultado con el dendrograma y sus pasos; la correlacion se asigna luego. */
    public ResultadoClustering(String nombreEnlace, NodoDendrograma raiz, List<PasoFusion> pasos) {
        this.nombreEnlace = nombreEnlace;
        this.raiz = raiz;
        this.pasos = pasos;
    }

    /** @return nombre del criterio de enlace usado */
    public String getNombreEnlace() {
        return nombreEnlace;
    }

    /** @return raiz del dendrograma */
    public NodoDendrograma getRaiz() {
        return raiz;
    }

    /** @return las fusiones en orden de ejecucion */
    public List<PasoFusion> getPasos() {
        return pasos;
    }

    /** @return correlacion cofenetica (1 = el dendrograma conserva perfectamente las distancias) */
    public double getCorrelacionCofenetica() {
        return correlacionCofenetica;
    }

    /** Asigna la correlacion cofenetica calculada por {@code CorrelacionCofenetica}. */
    public void setCorrelacionCofenetica(double correlacionCofenetica) {
        this.correlacionCofenetica = correlacionCofenetica;
    }

    /** @return el resultado completo (dendrograma, pasos y evaluacion) como mapa para JSON */
    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombreEnlace", nombreEnlace);
        m.put("correlacionCofenetica", correlacionCofenetica);
        m.put("dendrograma", raiz.aMapa());
        m.put("pasos", pasos.stream().map(PasoFusion::aMapa).collect(Collectors.toList()));
        return m;
    }
}
