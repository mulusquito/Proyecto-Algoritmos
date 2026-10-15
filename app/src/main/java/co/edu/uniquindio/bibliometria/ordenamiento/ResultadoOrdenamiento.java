package co.edu.uniquindio.bibliometria.ordenamiento;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de ejecutar (y medir) uno de los tres algoritmos de
 * ordenamiento sobre la lista de frecuencias del corpus: estadisticas de
 * tiempo en milisegundos, el top N de terminos obtenido y si el arreglo
 * completo quedo correctamente ordenado.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ResultadoOrdenamiento {

    private final String algoritmo;
    private final String complejidad;
    private final double[] tiemposMs;
    private final TerminoFrecuencia[] ordenado;
    private final int top;
    private final boolean ordenCorrecto;

    public ResultadoOrdenamiento(String algoritmo, String complejidad, double[] tiemposMs,
                                 TerminoFrecuencia[] ordenado, int top, boolean ordenCorrecto) {
        this.algoritmo = algoritmo;
        this.complejidad = complejidad;
        this.tiemposMs = tiemposMs;
        this.ordenado = ordenado;
        this.top = Math.min(top, ordenado.length);
        this.ordenCorrecto = ordenCorrecto;
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public TerminoFrecuencia[] getOrdenado() {
        return ordenado;
    }

    public double getPromedioMs() {
        double suma = 0;
        for (double t : tiemposMs) suma += t;
        return suma / tiemposMs.length;
    }

    public double getMedianaMs() {
        double[] copia = tiemposMs.clone();
        // Insercion: el numero de repeticiones es pequeno y asi no se usa Arrays.sort.
        for (int i = 1; i < copia.length; i++) {
            double actual = copia[i];
            int j = i - 1;
            while (j >= 0 && copia[j] > actual) {
                copia[j + 1] = copia[j];
                j--;
            }
            copia[j + 1] = actual;
        }
        int mitad = copia.length / 2;
        return copia.length % 2 == 1 ? copia[mitad] : (copia[mitad - 1] + copia[mitad]) / 2;
    }

    public double getMinimoMs() {
        double min = Double.MAX_VALUE;
        for (double t : tiemposMs) min = Math.min(min, t);
        return min;
    }

    public double getMaximoMs() {
        double max = 0;
        for (double t : tiemposMs) max = Math.max(max, t);
        return max;
    }

    public double getDesviacionMs() {
        double promedio = getPromedioMs();
        double suma = 0;
        for (double t : tiemposMs) suma += (t - promedio) * (t - promedio);
        return Math.sqrt(suma / tiemposMs.length);
    }

    public List<TerminoFrecuencia> getTop() {
        List<TerminoFrecuencia> lista = new ArrayList<>(top);
        for (int i = 0; i < top; i++) lista.add(ordenado[i]);
        return lista;
    }

    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("algoritmo", algoritmo);
        mapa.put("complejidad", complejidad);
        mapa.put("repeticiones", tiemposMs.length);
        mapa.put("promedioMs", getPromedioMs());
        mapa.put("medianaMs", getMedianaMs());
        mapa.put("minimoMs", getMinimoMs());
        mapa.put("maximoMs", getMaximoMs());
        mapa.put("desviacionMs", getDesviacionMs());
        mapa.put("ordenCorrecto", ordenCorrecto);
        List<Map<String, Object>> lista = new ArrayList<>();
        for (TerminoFrecuencia t : getTop()) lista.add(t.aMapa());
        mapa.put("top", lista);
        return mapa;
    }
}
