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

    /** Nombre del algoritmo medido. */
    private final String algoritmo;
    /** Complejidad teorica del algoritmo. */
    private final String complejidad;
    /** Tiempo de cada repeticion, en milisegundos. */
    private final double[] tiemposMs;
    /** Lista completa ordenada que produjo el algoritmo. */
    private final TerminoFrecuencia[] ordenado;
    /** Cuantos terminos se incluyen en el top. */
    private final int top;
    /** true si la lista quedo en orden descendente. */
    private final boolean ordenCorrecto;

    /**
     * Guarda los datos de la medicion. Si se pide un top mayor que la
     * cantidad de terminos, se ajusta a esa cantidad.
     */
    public ResultadoOrdenamiento(String algoritmo, String complejidad, double[] tiemposMs,
                                 TerminoFrecuencia[] ordenado, int top, boolean ordenCorrecto) {
        this.algoritmo = algoritmo;
        this.complejidad = complejidad;
        this.tiemposMs = tiemposMs;
        this.ordenado = ordenado;
        this.top = Math.min(top, ordenado.length);
        this.ordenCorrecto = ordenCorrecto;
    }

    /** @return nombre del algoritmo */
    public String getAlgoritmo() {
        return algoritmo;
    }

    /** @return la lista completa ordenada */
    public TerminoFrecuencia[] getOrdenado() {
        return ordenado;
    }

    /** @return promedio de los tiempos (ms) */
    public double getPromedioMs() {
        double suma = 0;
        for (double t : tiemposMs) suma += t;
        return suma / tiemposMs.length;
    }

    /**
     * Mediana de los tiempos: se ordena una copia y se toma el valor del
     * medio (o el promedio de los dos del medio si la cantidad es par).
     *
     * @return mediana de los tiempos (ms)
     */
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

    /** @return el tiempo mas rapido (ms) */
    public double getMinimoMs() {
        double min = Double.MAX_VALUE;
        for (double t : tiemposMs) min = Math.min(min, t);
        return min;
    }

    /** @return el tiempo mas lento (ms) */
    public double getMaximoMs() {
        double max = 0;
        for (double t : tiemposMs) max = Math.max(max, t);
        return max;
    }

    /**
     * Desviacion estandar poblacional: raiz del promedio de las diferencias
     * al cuadrado entre cada tiempo y el promedio. Mide cuanto varian los tiempos.
     *
     * @return desviacion estandar (ms)
     */
    public double getDesviacionMs() {
        double promedio = getPromedioMs();
        double suma = 0;
        for (double t : tiemposMs) suma += (t - promedio) * (t - promedio);
        return Math.sqrt(suma / tiemposMs.length);
    }

    /** @return los primeros {@code top} terminos de la lista ordenada */
    public List<TerminoFrecuencia> getTop() {
        List<TerminoFrecuencia> lista = new ArrayList<>(top);
        for (int i = 0; i < top; i++) lista.add(ordenado[i]);
        return lista;
    }

    /** @return estadisticas de tiempo y top como mapa, listo para convertir a JSON */
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
