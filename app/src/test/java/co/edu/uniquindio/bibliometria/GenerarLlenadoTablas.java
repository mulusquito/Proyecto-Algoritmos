package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.busqueda.AlgoritmoBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.BoyerMoore;
import co.edu.uniquindio.bibliometria.busqueda.KnuthMorrisPratt;
import co.edu.uniquindio.bibliometria.busqueda.MotorBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.RabinKarp;
import co.edu.uniquindio.bibliometria.busqueda.ResultadoBusqueda;

import java.util.Arrays;
import java.util.Map;

/**
 * Utilidad de una sola ejecucion que genera, para el documento tecnico
 * (seccion 6), el llenado paso a paso de las estructuras auxiliares de KMP y
 * Boyer-Moore y el experimento de peor caso. No forma parte de la aplicacion.
 * <p>
 * Las trazas repiten las mismas instrucciones de
 * {@link KnuthMorrisPratt#preprocesar} y {@link BoyerMoore#preprocesar},
 * agregando solo la impresion de cada paso; al final se verifica que las
 * tablas obtenidas sean identicas a las que calcula el codigo real.
 * <p>
 * Uso: {@code java -cp <clases> co.edu.uniquindio.bibliometria.GenerarLlenadoTablas}
 */
public class GenerarLlenadoTablas {

    public static void main(String[] args) {
        trazaPi("generative");
        trazaPi("ababaca");
        trazaCaracterMalo("generative");
        trazaSufijoBueno("generative");
        trazaSufijoBueno("ababaca");
        peorCaso();
    }

    // ------------------------------------------------------------------
    // KMP: funcion de prefijo
    // ------------------------------------------------------------------
    private static void trazaPi(String texto) {
        char[] p = texto.toCharArray();
        int m = p.length;
        int[] pi = new int[m];
        int k = 0;
        int comparaciones = 0;
        System.out.println("\n## Llenado de pi para \"" + texto + "\"\n");
        System.out.println("| q | P[q] | k al entrar | Comparaciones (P[k] vs P[q]) | Accion | pi[q] |");
        System.out.println("|---|---|---|---|---|---|");
        System.out.println("| 0 | " + p[0] + " | - | - | pi[0] = 0 por definicion | 0 |");
        for (int q = 1; q < m; q++) {
            int kEntrada = k;
            StringBuilder comps = new StringBuilder();
            StringBuilder accion = new StringBuilder();
            while (k > 0 && p[k] != p[q]) {
                comparaciones++;
                comps.append("P[").append(k).append("]=").append(p[k]).append(" ≠ ").append(p[q]).append("; ");
                accion.append("k = pi[").append(k - 1).append("] = ").append(pi[k - 1]).append("; ");
                k = pi[k - 1];
            }
            comparaciones++;
            if (p[k] == p[q]) {
                comps.append("P[").append(k).append("]=").append(p[k]).append(" = ").append(p[q]);
                accion.append("extiende el borde: k = ").append(k + 1);
                k++;
            } else {
                comps.append("P[").append(k).append("]=").append(p[k]).append(" ≠ ").append(p[q]);
                accion.append("k queda en 0");
            }
            pi[q] = k;
            System.out.println("| " + q + " | " + p[q] + " | " + kEntrada + " | " + comps + " | " + accion + " | " + k + " |");
        }
        int[] real = new KnuthMorrisPratt().preprocesar(p);
        System.out.println("\npi = " + Arrays.toString(pi) + "; comparaciones = " + comparaciones
                + " (cota 2m = " + 2 * m + "); igual al codigo real: " + Arrays.equals(pi, real));
    }

    // ------------------------------------------------------------------
    // Boyer-Moore: caracter malo
    // ------------------------------------------------------------------
    private static void trazaCaracterMalo(String texto) {
        char[] p = texto.toCharArray();
        System.out.println("\n## Llenado de la tabla de caracter malo para \"" + texto + "\"\n");
        System.out.println("Inicio: caracterMalo[c] = -1 para los 256 codigos.\n");
        System.out.println("| j | P[j] | Asignacion | Estado de la tabla (solo letras del patron) |");
        System.out.println("|---|---|---|---|");
        Map<Character, Integer> tabla = new java.util.LinkedHashMap<>();
        for (int j = 0; j < p.length; j++) {
            Integer anterior = tabla.get(p[j]);
            tabla.put(p[j], j);
            String asignacion = "caracterMalo['" + p[j] + "'] = " + j + (anterior != null ? " (sobrescribe " + anterior + ")" : "");
            StringBuilder estado = new StringBuilder();
            for (Map.Entry<Character, Integer> e : tabla.entrySet()) estado.append(e.getKey()).append("=").append(e.getValue()).append(" ");
            System.out.println("| " + j + " | " + p[j] + " | " + asignacion + " | " + estado.toString().trim() + " |");
        }
        Map<String, Object> real = new BoyerMoore().describirEstructura(p, new BoyerMoore().preprocesar(p));
        System.out.println("\nTabla real: " + real.get("caracterMalo"));
    }

    // ------------------------------------------------------------------
    // Boyer-Moore: sufijo bueno (regla fuerte), con el arreglo borde
    // ------------------------------------------------------------------
    private static void trazaSufijoBueno(String texto) {
        char[] p = texto.toCharArray();
        int m = p.length;
        int[] s = new int[m + 1];
        int[] f = new int[m + 1];
        System.out.println("\n## Llenado de la tabla de sufijo bueno para \"" + texto + "\" (m = " + m + ")\n");
        System.out.println("### Caso 1 (recorrido de derecha a izquierda)\n");
        System.out.println("| i | j | Comparacion P[i-1] vs P[j-1] | Accion | borde[i] |");
        System.out.println("|---|---|---|---|---|");
        int i = m, j = m + 1;
        f[i] = j;
        System.out.println("| " + i + " | " + j + " | - | inicio: borde[" + m + "] = " + (m + 1) + " (el sufijo vacio no tiene borde) | " + f[i] + " |");
        while (i > 0) {
            StringBuilder comps = new StringBuilder();
            StringBuilder accion = new StringBuilder();
            int jEntrada = j;
            while (j <= m && p[i - 1] != p[j - 1]) {
                comps.append("P[").append(i - 1).append("]=").append(p[i - 1]).append(" ≠ P[").append(j - 1).append("]=").append(p[j - 1]).append("; ");
                if (s[j] == 0) {
                    s[j] = j - i;
                    accion.append("sufijoBueno[").append(j).append("] = ").append(j).append("−").append(i).append(" = ").append(j - i).append("; ");
                } else {
                    accion.append("sufijoBueno[").append(j).append("] ya tenia ").append(s[j]).append("; ");
                }
                accion.append("j = borde[").append(j).append("] = ").append(f[j]).append("; ");
                j = f[j];
            }
            if (j <= m) {
                comps.append("P[").append(i - 1).append("]=").append(p[i - 1]).append(" = P[").append(j - 1).append("]=").append(p[j - 1]);
            } else if (comps.length() == 0) {
                comps.append("(j = ").append(j).append(" > m: no hay borde que comparar)");
            }
            i--;
            j--;
            f[i] = j;
            accion.append("i = ").append(i).append(", j = ").append(j);
            System.out.println("| " + (i + 1) + "→" + i + " | " + jEntrada + "→" + j + " | " + comps + " | " + accion + " | borde[" + i + "] = " + j + " |");
        }
        System.out.println("\nTras el caso 1: sufijoBueno = " + Arrays.toString(s) + ", borde = " + Arrays.toString(f));

        System.out.println("\n### Caso 2 (posiciones que quedaron en 0 reciben el borde del patron)\n");
        j = f[0];
        System.out.println("j = borde[0] = " + j + "\n");
        System.out.println("| i | sufijoBueno[i] antes | Accion | j |");
        System.out.println("|---|---|---|---|");
        for (i = 0; i <= m; i++) {
            int antes = s[i];
            String accion;
            if (s[i] == 0) {
                s[i] = j;
                accion = "estaba en 0: sufijoBueno[" + i + "] = j = " + j;
            } else {
                accion = "ya tenia valor (caso 1), no cambia";
            }
            if (i == j) {
                accion += "; i = j: j = borde[" + j + "] = " + f[j];
                j = f[j];
            }
            System.out.println("| " + i + " | " + antes + " | " + accion + " | " + j + " |");
        }
        Map<String, Object> real = new BoyerMoore().describirEstructura(p, new BoyerMoore().preprocesar(p));
        System.out.println("\nsufijoBueno = " + Arrays.toString(s) + "; igual al codigo real: "
                + Arrays.equals(s, (int[]) real.get("sufijoBueno")) + "; borde igual: " + Arrays.equals(f, (int[]) real.get("borde")));
    }

    // ------------------------------------------------------------------
    // Peor caso: texto "aaa...a" y patron "aa...a"
    // ------------------------------------------------------------------
    private static void peorCaso() {
        int n = 100_000;
        char[] t = new char[n];
        Arrays.fill(t, 'a');
        System.out.println("\n## Peor caso: T = 'a' x " + n + ", P = 'a' x m\n");
        System.out.println("| m | Algoritmo | Comparaciones | Comp./caracter | (n-m+1)·m | Ocurrencias | Busqueda (ms) |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (int m : new int[]{5, 10, 20, 50, 100}) {
            char[] p = new char[m];
            Arrays.fill(p, 'a');
            for (AlgoritmoBusqueda<?> a : MotorBusqueda.crearAlgoritmos(RabinKarp.MODULO_POR_DEFECTO)) {
                imprimir(a, t, p, n, m);
            }
        }
        // Contraste: patron con un caracter distinto al inicio ("baa...a"): no hay ninguna ocurrencia.
        System.out.println("\n## Peor caso con P = 'b' + 'a' x (m-1) (sin ocurrencias)\n");
        System.out.println("| m | Algoritmo | Comparaciones | Comp./caracter | (n-m+1)·m | Ocurrencias | Busqueda (ms) |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (int m : new int[]{10, 100}) {
            char[] p = new char[m];
            Arrays.fill(p, 'a');
            p[0] = 'b';
            for (AlgoritmoBusqueda<?> a : MotorBusqueda.crearAlgoritmos(RabinKarp.MODULO_POR_DEFECTO)) {
                imprimir(a, t, p, n, m);
            }
        }
    }

    private static <E> void imprimir(AlgoritmoBusqueda<E> a, char[] t, char[] p, int n, int m) {
        E e = a.preprocesar(p);
        for (int r = 0; r < 30; r++) a.buscar(t, p, e, null);
        double[] tiempos = new double[51];
        ResultadoBusqueda res = null;
        for (int r = 0; r < tiempos.length; r++) {
            long t0 = System.nanoTime();
            res = a.buscar(t, p, e, null);
            tiempos[r] = (System.nanoTime() - t0) / 1e6;
        }
        Arrays.sort(tiempos);
        System.out.printf("| %d | %s | %d | %.1f | %d | %d | %.3f |%n", m, a.getNombre(), res.getComparaciones(),
                res.getComparaciones() / (double) n, (long) (n - m + 1) * m, res.getPosiciones().size(), tiempos[tiempos.length / 2]);
    }
}
