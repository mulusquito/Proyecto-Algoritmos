package co.edu.uniquindio.bibliometria.ordenamiento;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Bucket Sort</b> (ordenamiento por cubetas) adaptado a claves enteras,
 * implementado desde cero.
 * <p>
 * La version clasica (Cormen) distribuye numeros reales en [0, 1) en n
 * cubetas. Aqui las claves son frecuencias enteras en [min, max], asi que
 * cada elemento se envia a la cubeta
 * <pre>
 * indice = (frecuencia - min) &middot; m / (max - min + 1)
 * </pre>
 * con m = n cubetas. La funcion es monotona: una cubeta de indice mayor
 * solo contiene frecuencias mayores o iguales a las de una cubeta de indice
 * menor, por lo que <b>no se compara</b> entre cubetas. Luego:
 * <ol>
 *     <li>Cada cubeta se ordena con Insercion (estable, descendente).</li>
 *     <li>Las cubetas se concatenan de la de mayor indice a la de menor.</li>
 * </ol>
 * Complejidad: O(n + m) esperada cuando las claves se reparten de forma
 * aproximadamente uniforme entre las cubetas; O(n<sup>2</sup>) en el peor
 * caso si muchas claves <i>distintas</i> caen en la misma cubeta. Cuando el
 * rango de frecuencias (max - min + 1) es menor o igual que n, cada cubeta
 * cubre un unico valor de frecuencia, la Insercion no realiza
 * desplazamientos y el costo es O(n + m) = O(n) (ver documento tecnico).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class BucketSort implements AlgoritmoOrdenamiento {

    /** Nombre del algoritmo que se muestra en la interfaz. */
    public static final String NOMBRE = "Bucket Sort";

    /** @return nombre del algoritmo */
    @Override
    public String getNombre() {
        return NOMBRE;
    }

    /** @return complejidad teorica, para mostrarla en la tabla de tiempos */
    @Override
    public String getComplejidad() {
        return "O(n + m) esperado";
    }

    /** Ordena de mayor a menor frecuencia: distribuir, ordenar cada cubeta y concatenar. */
    @Override
    public TerminoFrecuencia[] ordenarDescendente(TerminoFrecuencia[] entrada) {
        int n = entrada.length;
        TerminoFrecuencia[] salida = new TerminoFrecuencia[n];
        if (n == 0) return salida;

        // 1. Repartir los elementos en cubetas segun su frecuencia.
        List<List<TerminoFrecuencia>> cubetas = distribuir(entrada);

        // 2 y 3. Recorrer las cubetas de la mayor a la menor, ordenar cada una
        //        y copiar sus elementos a la salida.
        int pos = 0;
        for (int i = cubetas.size() - 1; i >= 0; i--) {
            List<TerminoFrecuencia> cubeta = cubetas.get(i);
            ordenarPorInsercionDescendente(cubeta);
            for (TerminoFrecuencia t : cubeta) {
                salida[pos++] = t;
            }
        }
        return salida;
    }

    /**
     * Fase de distribucion: reparte los elementos (en su orden original) en
     * m = n cubetas segun su frecuencia. Es publica para poder reportar en la
     * interfaz cuantas cubetas quedan ocupadas y su tamano maximo.
     */
    public List<List<TerminoFrecuencia>> distribuir(TerminoFrecuencia[] entrada) {
        int n = entrada.length;
        // Rango de frecuencias [min, max] que hay que repartir entre las cubetas.
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (TerminoFrecuencia t : entrada) {
            if (t.getFrecuencia() < min) min = t.getFrecuencia();
            if (t.getFrecuencia() > max) max = t.getFrecuencia();
        }

        // Se crean m = n cubetas vacias, como en la version clasica del algoritmo.
        int m = n;
        long rango = (long) max - min + 1;
        List<List<TerminoFrecuencia>> cubetas = new ArrayList<>(m);
        for (int i = 0; i < m; i++) cubetas.add(new ArrayList<>());

        // Cada elemento va a la cubeta que le corresponde por su frecuencia (se usa long
        // en la multiplicacion para evitar desbordamiento con valores grandes).
        for (TerminoFrecuencia t : entrada) {
            int indice = (int) ((t.getFrecuencia() - min) * (long) m / rango);
            cubetas.get(indice).add(t);
        }
        return cubetas;
    }

    /**
     * Insercion estable de mayor a menor: solo se desplaza un elemento cuando
     * el anterior tiene una frecuencia estrictamente menor, por lo que los
     * empates conservan su orden.
     */
    private static void ordenarPorInsercionDescendente(List<TerminoFrecuencia> cubeta) {
        for (int i = 1; i < cubeta.size(); i++) {
            TerminoFrecuencia actual = cubeta.get(i);
            int j = i - 1;
            while (j >= 0 && cubeta.get(j).getFrecuencia() < actual.getFrecuencia()) {
                cubeta.set(j + 1, cubeta.get(j));
                j--;
            }
            cubeta.set(j + 1, actual);
        }
    }
}
