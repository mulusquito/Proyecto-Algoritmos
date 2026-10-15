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
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ResultadoSimilitud {

    /** Nombre del algoritmo que produjo el resultado. */
    private final String algoritmo;
    private final String categoria; // "clasico" o "ia"
    private final double similitud; // normalizada en [0,1]; 1 = identicos
    /** Valores intermedios del calculo, en el orden en que se agregaron. */
    private final Map<String, Object> detalle = new LinkedHashMap<>();

    /**
     * @param algoritmo nombre del algoritmo
     * @param categoria "clasico" o "ia"
     * @param similitud puntaje final de similitud
     */
    public ResultadoSimilitud(String algoritmo, String categoria, double similitud) {
        this.algoritmo = algoritmo;
        this.categoria = categoria;
        this.similitud = similitud;
    }

    /**
     * Agrega un dato intermedio del calculo (por ejemplo la distancia o la
     * matriz). Devuelve el mismo objeto para poder encadenar llamadas:
     * {@code new ResultadoSimilitud(...).conDetalle(...).conDetalle(...)}.
     */
    public ResultadoSimilitud conDetalle(String clave, Object valor) {
        this.detalle.put(clave, valor);
        return this;
    }

    /** @return nombre del algoritmo */
    public String getAlgoritmo() {
        return algoritmo;
    }

    /** @return "clasico" o "ia" */
    public String getCategoria() {
        return categoria;
    }

    /** @return puntaje de similitud */
    public double getSimilitud() {
        return similitud;
    }

    /** @return datos intermedios del calculo */
    public Map<String, Object> getDetalle() {
        return detalle;
    }

    /** @return el resultado como mapa clave -> valor, listo para convertir a JSON */
    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("algoritmo", algoritmo);
        m.put("categoria", categoria);
        m.put("similitud", similitud);
        m.put("detalle", detalle);
        return m;
    }
}
