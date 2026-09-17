package co.edu.uniquindio.bibliometria.similitud;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resultado producido por cualquiera de los 6 algoritmos de similitud
 * (4 clasicos + 2 basados en IA). Ademas del puntaje final, expone un mapa
 * de "detalle" con la informacion intermedia del calculo (por ejemplo la
 * matriz de programacion dinamica, o el numero de terminos en comun) para
 * poder demostrar el funcionamiento algoritmico paso a paso, tal como lo
 * exige el requerimiento 1 del proyecto.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class ResultadoSimilitud {

    private final String algoritmo;
    private final String categoria; // "clasico" o "ia"
    private final double similitud; // normalizada en [0,1]; 1 = identicos
    private final Map<String, Object> detalle = new LinkedHashMap<>();

    public ResultadoSimilitud(String algoritmo, String categoria, double similitud) {
        this.algoritmo = algoritmo;
        this.categoria = categoria;
        this.similitud = similitud;
    }

    public ResultadoSimilitud conDetalle(String clave, Object valor) {
        this.detalle.put(clave, valor);
        return this;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public String getCategoria() {
        return categoria;
    }

    public double getSimilitud() {
        return similitud;
    }

    public Map<String, Object> getDetalle() {
        return detalle;
    }

    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("algoritmo", algoritmo);
        m.put("categoria", categoria);
        m.put("similitud", similitud);
        m.put("detalle", detalle);
        return m;
    }
}
