package co.edu.uniquindio.bibliometria.clustering;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resultado completo de ejecutar un algoritmo de clustering jerarquico: el dendrograma, los pasos de fusion y su evaluacion.  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ResultadoClustering {

    private final String nombreEnlace;
    private final NodoDendrograma raiz;
    private final List<PasoFusion> pasos;
    private double correlacionCofenetica;

    public ResultadoClustering(String nombreEnlace, NodoDendrograma raiz, List<PasoFusion> pasos) {
        this.nombreEnlace = nombreEnlace;
        this.raiz = raiz;
        this.pasos = pasos;
    }

    public String getNombreEnlace() {
        return nombreEnlace;
    }

    public NodoDendrograma getRaiz() {
        return raiz;
    }

    public List<PasoFusion> getPasos() {
        return pasos;
    }

    public double getCorrelacionCofenetica() {
        return correlacionCofenetica;
    }

    public void setCorrelacionCofenetica(double correlacionCofenetica) {
        this.correlacionCofenetica = correlacionCofenetica;
    }

    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nombreEnlace", nombreEnlace);
        m.put("correlacionCofenetica", correlacionCofenetica);
        m.put("dendrograma", raiz.aMapa());
        m.put("pasos", pasos.stream().map(PasoFusion::aMapa).collect(Collectors.toList()));
        return m;
    }
}
