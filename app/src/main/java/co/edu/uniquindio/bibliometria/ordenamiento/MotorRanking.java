package co.edu.uniquindio.bibliometria.ordenamiento;

import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el seguimiento "Ranking de terminos mediante algoritmos de
 * ordenamiento": construye una sola vez la lista de frecuencias del corpus,
 * ejecuta sobre ella Counting Sort, Radix Sort y Bucket Sort, mide sus
 * tiempos reales en milisegundos y valida que los tres produzcan el mismo
 * ranking.
 * <p>
 * Medicion: la JVM compila el codigo "caliente" en tiempo de ejecucion
 * (JIT), asi que las primeras ejecuciones son mas lentas y no representan
 * el costo del algoritmo. Por eso se hace un calentamiento sin medir y luego
 * varias repeticiones medidas con {@link System#nanoTime()}. Las
 * repeticiones se intercalan (Counting, Radix, Bucket, Counting, ...) para
 * que ningun algoritmo se beneficie o se perjudique por el momento en que
 * corre (recolector de basura, otros procesos).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class MotorRanking {

    public static final int TOP_POR_DEFECTO = 15;
    public static final int REPETICIONES_POR_DEFECTO = 50;
    private static final int CALENTAMIENTO = 200;

    private final TerminoFrecuencia[] frecuencias;
    private final int totalTokens;
    private final List<AlgoritmoOrdenamiento> algoritmos =
            List.of(new CountingSort(), new RadixSort(), new BucketSort());

    public MotorRanking(RepositorioArticulos repositorioArticulos) {
        this.frecuencias = ContadorFrecuencias.contar(repositorioArticulos.listarTodos());
        int suma = 0;
        for (TerminoFrecuencia t : frecuencias) suma += t.getFrecuencia();
        this.totalTokens = suma;
    }

    public TerminoFrecuencia[] getFrecuencias() {
        return frecuencias;
    }

    public List<AlgoritmoOrdenamiento> getAlgoritmos() {
        return algoritmos;
    }

    /**
     * Ejecuta y mide los tres algoritmos. Es {@code synchronized} para que
     * dos peticiones simultaneas no compitan por la CPU y distorsionen los
     * tiempos medidos.
     */
    public synchronized List<ResultadoOrdenamiento> ejecutar(int top, int repeticiones) {
        for (int i = 0; i < CALENTAMIENTO; i++) {
            for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
                algoritmo.ordenarDescendente(frecuencias);
            }
        }

        double[][] tiempos = new double[algoritmos.size()][repeticiones];
        TerminoFrecuencia[][] salidas = new TerminoFrecuencia[algoritmos.size()][];
        for (int r = 0; r < repeticiones; r++) {
            for (int a = 0; a < algoritmos.size(); a++) {
                long inicio = System.nanoTime();
                TerminoFrecuencia[] ordenado = algoritmos.get(a).ordenarDescendente(frecuencias);
                long fin = System.nanoTime();
                tiempos[a][r] = (fin - inicio) / 1_000_000.0;
                salidas[a] = ordenado;
            }
        }

        List<ResultadoOrdenamiento> resultados = new ArrayList<>();
        for (int a = 0; a < algoritmos.size(); a++) {
            AlgoritmoOrdenamiento algoritmo = algoritmos.get(a);
            resultados.add(new ResultadoOrdenamiento(algoritmo.getNombre(), algoritmo.getComplejidad(),
                    tiempos[a], salidas[a], top, estaOrdenadoDescendente(salidas[a])));
        }
        return resultados;
    }

    /** Respuesta completa para la API: datos del corpus, parametros del analisis y resultados. */
    public Map<String, Object> ejecutarComoMapa(int top, int repeticiones) {
        List<ResultadoOrdenamiento> resultados = ejecutar(top, repeticiones);

        boolean coinciden = true;
        TerminoFrecuencia[] referencia = resultados.get(0).getOrdenado();
        for (ResultadoOrdenamiento r : resultados) {
            coinciden &= mismoOrden(referencia, r.getOrdenado());
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("corpus", estadisticasCorpus());
        List<Map<String, Object>> lista = new ArrayList<>();
        for (ResultadoOrdenamiento r : resultados) lista.add(r.aMapa());
        respuesta.put("resultados", lista);
        respuesta.put("losTresCoinciden", coinciden);
        return respuesta;
    }

    /** Valores de n, k, d y m que intervienen en la complejidad de cada algoritmo. */
    public Map<String, Object> estadisticasCorpus() {
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (TerminoFrecuencia t : frecuencias) {
            min = Math.min(min, t.getFrecuencia());
            max = Math.max(max, t.getFrecuencia());
        }
        boolean[] presente = new boolean[max + 1];
        int valoresDistintos = 0;
        for (TerminoFrecuencia t : frecuencias) {
            if (!presente[t.getFrecuencia()]) {
                presente[t.getFrecuencia()] = true;
                valoresDistintos++;
            }
        }
        int cubetasOcupadas = 0;
        int cubetaMasGrande = 0;
        List<List<TerminoFrecuencia>> cubetas = new BucketSort().distribuir(frecuencias);
        for (List<TerminoFrecuencia> c : cubetas) {
            if (!c.isEmpty()) cubetasOcupadas++;
            cubetaMasGrande = Math.max(cubetaMasGrande, c.size());
        }

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("terminosDistintos", frecuencias.length);
        mapa.put("totalTokens", totalTokens);
        mapa.put("frecuenciaMinima", min);
        mapa.put("frecuenciaMaxima", max);
        mapa.put("valoresDeFrecuenciaDistintos", valoresDistintos);
        mapa.put("digitosRadix", RadixSort.numeroDeDigitos(max));
        mapa.put("cubetas", cubetas.size());
        mapa.put("cubetasOcupadas", cubetasOcupadas);
        mapa.put("cubetaMasGrande", cubetaMasGrande);
        return mapa;
    }

    public static boolean estaOrdenadoDescendente(TerminoFrecuencia[] arreglo) {
        for (int i = 1; i < arreglo.length; i++) {
            if (arreglo[i - 1].getFrecuencia() < arreglo[i].getFrecuencia()) return false;
        }
        return true;
    }

    public static boolean mismoOrden(TerminoFrecuencia[] a, TerminoFrecuencia[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
