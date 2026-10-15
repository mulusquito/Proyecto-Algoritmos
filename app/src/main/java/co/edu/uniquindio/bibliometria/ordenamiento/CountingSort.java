package co.edu.uniquindio.bibliometria.ordenamiento;

/**
 * <b>Counting Sort</b> (ordenamiento por conteo), implementado desde cero.
 * <p>
 * En lugar de comparar elementos entre si, usa el <i>valor</i> de la clave
 * (la frecuencia) como indice de un arreglo auxiliar de conteo:
 * <ol>
 *     <li>Se cuenta cuantas veces aparece cada frecuencia v en [0, k]:
 *         {@code conteo[v]} &mdash; O(n).</li>
 *     <li>Se calcula, recorriendo las frecuencias de mayor a menor, la
 *         posicion de inicio de cada valor en la salida (suma acumulada):
 *         {@code inicio[v] = suma de conteo[w] para w &gt; v} &mdash; O(k).</li>
 *     <li>Se recorre la entrada en su orden original y cada elemento se
 *         coloca en {@code salida[inicio[v]++]} &mdash; O(n). Recorrer la
 *         entrada en orden directo es lo que hace al algoritmo estable.</li>
 * </ol>
 * Complejidad: O(n + k) en tiempo y O(n + k) en espacio, donde n es el
 * numero de terminos y k la frecuencia maxima.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class CountingSort implements AlgoritmoOrdenamiento {

    public static final String NOMBRE = "Counting Sort";

    @Override
    public String getNombre() {
        return NOMBRE;
    }

    @Override
    public String getComplejidad() {
        return "O(n + k)";
    }

    @Override
    public TerminoFrecuencia[] ordenarDescendente(TerminoFrecuencia[] entrada) {
        int n = entrada.length;
        TerminoFrecuencia[] salida = new TerminoFrecuencia[n];
        if (n == 0) return salida;

        int k = 0;
        for (TerminoFrecuencia t : entrada) {
            if (t.getFrecuencia() > k) k = t.getFrecuencia();
        }

        // 1. Conteo de ocurrencias de cada frecuencia.
        int[] conteo = new int[k + 1];
        for (TerminoFrecuencia t : entrada) {
            conteo[t.getFrecuencia()]++;
        }

        // 2. Posicion de inicio de cada frecuencia, de la mayor a la menor
        //    (por eso el resultado queda en orden descendente).
        int[] inicio = new int[k + 1];
        int acumulado = 0;
        for (int v = k; v >= 0; v--) {
            inicio[v] = acumulado;
            acumulado += conteo[v];
        }

        // 3. Ubicacion estable de cada elemento en la salida.
        for (TerminoFrecuencia t : entrada) {
            salida[inicio[t.getFrecuencia()]++] = t;
        }
        return salida;
    }
}
