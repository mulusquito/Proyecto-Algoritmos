package co.edu.uniquindio.bibliometria.ordenamiento;

/**
 * <b>Radix Sort LSD</b> (digito menos significativo primero) en base 10,
 * implementado desde cero.
 * <p>
 * Ordena las frecuencias digito por digito, empezando por las unidades,
 * luego las decenas, las centenas, etc. Cada pasada es un Counting Sort
 * estable sobre un solo digito (valores 0..9, es decir, b = 10 cubetas).
 * Como cada pasada es estable, el orden logrado por los digitos menos
 * significativos se conserva cuando dos claves empatan en un digito mas
 * significativo; al terminar la ultima pasada el arreglo queda ordenado por
 * la clave completa. Para obtener orden descendente, en cada pasada el
 * digito 9 se ubica primero y el 0 al final.
 * <p>
 * Complejidad: O(d &middot; (n + b)), con d = numero de digitos de la
 * frecuencia maxima k (d = floor(log10 k) + 1) y b = 10. Como d y b son
 * constantes pequenas para un corpus dado, el costo es O(n).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class RadixSort implements AlgoritmoOrdenamiento {

    /** Nombre del algoritmo que se muestra en la interfaz. */
    public static final String NOMBRE = "Radix Sort (LSD, base 10)";
    /** Base numerica: cada pasada ordena por un digito decimal (0 a 9). */
    private static final int BASE = 10;

    /** @return nombre del algoritmo */
    @Override
    public String getNombre() {
        return NOMBRE;
    }

    /** @return complejidad teorica, para mostrarla en la tabla de tiempos */
    @Override
    public String getComplejidad() {
        return "O(d · (n + b))";
    }

    /** Ordena de mayor a menor frecuencia con d pasadas de Counting Sort, una por digito. */
    @Override
    public TerminoFrecuencia[] ordenarDescendente(TerminoFrecuencia[] entrada) {
        // Copia de la entrada para no modificar el arreglo original.
        TerminoFrecuencia[] actual = new TerminoFrecuencia[entrada.length];
        for (int i = 0; i < entrada.length; i++) actual[i] = entrada[i];
        if (actual.length <= 1) return actual;

        // La frecuencia maxima define cuantos digitos (pasadas) hacen falta.
        int maximo = 0;
        for (TerminoFrecuencia t : actual) {
            if (t.getFrecuencia() > maximo) maximo = t.getFrecuencia();
        }

        // Una pasada por cada digito de la frecuencia maxima.
        for (long exp = 1; maximo / exp > 0; exp *= BASE) {
            actual = ordenarPorDigito(actual, exp);
        }
        return actual;
    }

    /** Numero de pasadas (digitos) que necesita una frecuencia maxima dada. */
    public static int numeroDeDigitos(int maximo) {
        int d = 0;
        for (long exp = 1; maximo / exp > 0; exp *= BASE) d++;
        return d;
    }

    /** Counting Sort estable y descendente sobre el digito de peso {@code exp}. */
    private TerminoFrecuencia[] ordenarPorDigito(TerminoFrecuencia[] entrada, long exp) {
        int[] conteo = new int[BASE];
        for (TerminoFrecuencia t : entrada) {
            conteo[digito(t.getFrecuencia(), exp)]++;
        }

        int[] inicio = new int[BASE];
        int acumulado = 0;
        for (int d = BASE - 1; d >= 0; d--) {
            inicio[d] = acumulado;
            acumulado += conteo[d];
        }

        TerminoFrecuencia[] salida = new TerminoFrecuencia[entrada.length];
        for (TerminoFrecuencia t : entrada) {
            salida[inicio[digito(t.getFrecuencia(), exp)]++] = t;
        }
        return salida;
    }

    /**
     * Extrae un digito del valor: con exp = 1 las unidades, con exp = 10 las
     * decenas, etc. Por ejemplo digito(86, 10) = 8.
     */
    private static int digito(int valor, long exp) {
        return (int) ((valor / exp) % BASE);
    }
}
