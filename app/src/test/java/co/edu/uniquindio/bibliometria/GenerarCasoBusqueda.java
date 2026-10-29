package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.busqueda.AlgoritmoBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.BoyerMoore;
import co.edu.uniquindio.bibliometria.busqueda.KnuthMorrisPratt;
import co.edu.uniquindio.bibliometria.busqueda.MotorBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.RabinKarp;
import co.edu.uniquindio.bibliometria.busqueda.ResultadoBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.Traza;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilidad de una sola ejecucion que genera, en Markdown, los datos reales
 * del caso de estudio del Requerimiento 3 que se incluyen en el documento
 * tecnico: estructuras auxiliares, trazas paso a paso sobre un parrafo de un
 * abstract y las mediciones de tiempo. No forma parte de la aplicacion.
 * <p>
 * Ejecutar con el classpath de las pruebas (ver README):
 * {@code java -cp <clases> co.edu.uniquindio.bibliometria.GenerarCasoBusqueda}
 */
public class GenerarCasoBusqueda {

    /** Patron del caso de estudio (dominio del proyecto: IA generativa). */
    private static final String PATRON = "generative";
    /** Articulo del que se toma el parrafo. */
    private static final int ARTICULO = 16;
    /** Inicio de la ultima oracion del abstract 16, que es el parrafo del caso de estudio. */
    private static final String INICIO_PARRAFO = "It argues that";

    private static final int CALENTAMIENTO = 300;
    private static final int REPETICIONES = 201;

    public static void main(String[] args) {
        List<Articulo> articulos = new RepositorioArticulos().listarTodos();
        String abstractCompleto = articulos.get(ARTICULO - 1).getAbstractOriginal();
        // Ubicar el parrafo con una comparacion directa (solo en esta utilidad de documentacion).
        int inicio = abstractCompleto.lastIndexOf(INICIO_PARRAFO);
        char[] texto = MotorBusqueda.aMinusculas(abstractCompleto.substring(inicio).toCharArray());
        char[] patron = PATRON.toCharArray();

        System.out.println("## Parrafo (abstract " + ARTICULO + ", en minusculas), n = " + texto.length + ", m = " + patron.length + "\n");
        System.out.println("`" + String.valueOf(texto) + "`\n");
        imprimirRegla(texto);

        // ---------------- KMP ----------------
        KnuthMorrisPratt kmp = new KnuthMorrisPratt();
        System.out.println("\n## KMP\n");
        System.out.println("pi(\"" + PATRON + "\") = " + java.util.Arrays.toString(kmp.preprocesar(patron)));
        System.out.println("pi(\"intelligence\") = " + java.util.Arrays.toString(kmp.preprocesar("intelligence".toCharArray())));
        System.out.println("pi(\"ababaca\") = " + java.util.Arrays.toString(kmp.preprocesar("ababaca".toCharArray())));
        imprimirTraza(kmp, texto, patron, true);

        // ---------------- Boyer-Moore ----------------
        BoyerMoore bm = new BoyerMoore();
        System.out.println("\n## Boyer-Moore\n");
        Map<String, Object> tablas = bm.describirEstructura(patron, bm.preprocesar(patron));
        System.out.println("caracterMalo = " + tablas.get("caracterMalo"));
        System.out.println("sufijoBueno = " + java.util.Arrays.toString((int[]) tablas.get("sufijoBueno")));
        System.out.println("borde = " + java.util.Arrays.toString((int[]) tablas.get("borde")));
        imprimirTraza(bm, texto, patron, false);

        // ---------------- Rabin-Karp ----------------
        for (long q : new long[]{RabinKarp.MODULO_DEMOSTRACION, RabinKarp.MODULO_POR_DEFECTO}) {
            RabinKarp rk = new RabinKarp(q);
            System.out.println("\n## Rabin-Karp q = " + q + "\n");
            System.out.println(rk.describirEstructura(patron, rk.preprocesar(patron)));
            imprimirTrazaRabinKarp(rk, texto, patron);
        }

        // ---------------- Mediciones ----------------
        List<char[]> corpus = new ArrayList<>();
        int n = 0;
        for (Articulo a : articulos) {
            char[] t = MotorBusqueda.aMinusculas(a.getAbstractOriginal().toCharArray());
            corpus.add(t);
            n += t.length;
        }
        System.out.println("\n## Experimento 1: corpus completo (n = " + n + "), distintas longitudes de patron\n");
        System.out.println("| Patron | m | Algoritmo | Preproc. (ms) | Busqueda (ms) | Comparaciones | Comp./caracter | Alineamientos | Ocurrencias |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        for (String p : new String[]{"ai", "data", "learning", "generative", "artificial intelligence",
                "the use of generative artificial intelligence"}) {
            for (AlgoritmoBusqueda<?> a : MotorBusqueda.crearAlgoritmos(RabinKarp.MODULO_POR_DEFECTO)) {
                imprimirMedicion(a, p, corpus, n);
            }
        }

        System.out.println("\n## Experimento 2: crecimiento de n (corpus concatenado k veces), patron \"" + PATRON + "\"\n");
        System.out.println("| k | n | Algoritmo | Busqueda (ms) | Comparaciones | Comp./caracter |");
        System.out.println("|---|---|---|---|---|---|");
        for (int k : new int[]{1, 2, 4, 8, 16}) {
            char[] grande = concatenar(corpus, k);
            List<char[]> uno = new ArrayList<>();
            uno.add(grande);
            for (AlgoritmoBusqueda<?> a : MotorBusqueda.crearAlgoritmos(RabinKarp.MODULO_POR_DEFECTO)) {
                imprimirCrecimiento(a, patron, uno, k, grande.length);
            }
        }

        System.out.println("\n## Experimento 3: Rabin-Karp, colisiones segun el modulo (corpus completo)\n");
        System.out.println("| q | Patron | Coincidencias de hash | Colisiones | Ocurrencias | Busqueda (ms) |");
        System.out.println("|---|---|---|---|---|---|");
        for (long q : new long[]{101, 1009, 1_000_000_007L}) {
            for (String p : new String[]{"ai", "generative"}) {
                RabinKarp rk = new RabinKarp(q);
                char[] pc = p.toCharArray();
                RabinKarp.Hash h = rk.preprocesar(pc);
                long hashes = 0, colisiones = 0, ocurrencias = 0;
                for (char[] t : corpus) {
                    ResultadoBusqueda r = rk.buscar(t, pc, h, null);
                    hashes += r.getCoincidenciasHash();
                    colisiones += r.getColisiones();
                    ocurrencias += r.getPosiciones().size();
                }
                double[] tiempos = medir(rk, pc, corpus)[1];
                System.out.printf("| %d | %s | %d | %d | %d | %.4f |%n", q, p, hashes, colisiones, ocurrencias, mediana(tiempos));
            }
        }
    }

    /** Regla de posiciones (decenas y unidades) para ubicar indices en el parrafo. */
    private static void imprimirRegla(char[] texto) {
        StringBuilder decenas = new StringBuilder();
        for (int i = 0; i < texto.length; i += 10) {
            String etiqueta = String.valueOf(i);
            decenas.append(etiqueta);
            for (int k = etiqueta.length(); k < 10 && i + k < texto.length; k++) decenas.append(' ');
        }
        System.out.println("```\n" + decenas + "\n" + String.valueOf(texto) + "\n```");
    }

    /** Traza completa en tabla Markdown; en KMP solo se listan los pasos con mas de una comparacion. */
    private static <E> void imprimirTraza(AlgoritmoBusqueda<E> algoritmo, char[] texto, char[] patron, boolean resumir) {
        Traza traza = new Traza(5000);
        ResultadoBusqueda r = algoritmo.buscar(texto, patron, algoritmo.preprocesar(patron), traza);
        System.out.println("\nPosiciones: " + r.getPosiciones() + " | comparaciones: " + r.getComparaciones()
                + " | alineamientos: " + r.getAlineamientos() + " | pasos: " + traza.getTotalPasos() + "\n");
        System.out.println("| Paso | s | Texto T[s..s+m-1] | Comp. | Evento | Desplaz. |");
        System.out.println("|---|---|---|---|---|---|");
        int omitidos = 0;
        for (Map<String, Object> paso : traza.getPasos()) {
            int comparaciones = (Integer) paso.get("comparaciones");
            String evento = (String) paso.get("evento");
            if (resumir && comparaciones <= 1 && !evento.startsWith("ocurrencia")) {
                omitidos++;
                continue;
            }
            int s = (Integer) paso.get("posicion");
            System.out.println("| " + paso.get("paso") + " | " + s + " | `" + ventana(texto, s, patron.length) + "` | "
                    + comparaciones + " | " + evento + " | +" + paso.get("desplazamiento") + " |");
        }
        if (resumir) System.out.println("\n(" + omitidos + " pasos con una sola comparacion fallida en j=0 omitidos)");
    }

    /** Traza de Rabin-Karp: las primeras ventanas y todas las coincidencias de hash. */
    private static void imprimirTrazaRabinKarp(RabinKarp rk, char[] texto, char[] patron) {
        Traza traza = new Traza(5000);
        ResultadoBusqueda r = rk.buscar(texto, patron, rk.preprocesar(patron), traza);
        System.out.println("\nPosiciones: " + r.getPosiciones() + " | ventanas: " + r.getAlineamientos()
                + " | coincidencias de hash: " + r.getCoincidenciasHash() + " | colisiones: " + r.getColisiones()
                + " | comparaciones: " + r.getComparaciones() + "\n");
        System.out.println("| s | Ventana | Evento |");
        System.out.println("|---|---|---|");
        for (Map<String, Object> paso : traza.getPasos()) {
            int s = (Integer) paso.get("posicion");
            String evento = (String) paso.get("evento");
            if (s < 6 || evento.contains("= hash(P)")) {
                System.out.println("| " + s + " | `" + ventana(texto, s, patron.length) + "` | " + evento + " |");
            }
        }
    }

    private static String ventana(char[] texto, int s, int m) {
        StringBuilder sb = new StringBuilder();
        for (int i = s; i < s + m && i < texto.length; i++) sb.append(texto[i] == ' ' ? '_' : texto[i]);
        return sb.toString();
    }

    private static <E> void imprimirMedicion(AlgoritmoBusqueda<E> a, String p, List<char[]> corpus, int n) {
        char[] pc = p.toCharArray();
        double[][] tiempos = medir(a, pc, corpus);
        E e = a.preprocesar(pc);
        long comp = 0, alin = 0, oc = 0;
        for (char[] t : corpus) {
            ResultadoBusqueda r = a.buscar(t, pc, e, null);
            comp += r.getComparaciones();
            alin += r.getAlineamientos();
            oc += r.getPosiciones().size();
        }
        System.out.printf("| %s | %d | %s | %.5f | %.4f | %d | %.3f | %d | %d |%n", p, pc.length, a.getNombre(),
                mediana(tiempos[0]), mediana(tiempos[1]), comp, comp / (double) n, alin, oc);
    }

    private static <E> void imprimirCrecimiento(AlgoritmoBusqueda<E> a, char[] pc, List<char[]> textos, int k, int n) {
        double[][] tiempos = medir(a, pc, textos);
        ResultadoBusqueda r = a.buscar(textos.get(0), pc, a.preprocesar(pc), null);
        System.out.printf("| %d | %d | %s | %.4f | %d | %.3f |%n", k, n, a.getNombre(), mediana(tiempos[1]),
                r.getComparaciones(), r.getComparaciones() / (double) n);
    }

    /** Calentamiento y R repeticiones; devuelve {tiemposPreprocesamiento, tiemposBusqueda} en ms. */
    private static <E> double[][] medir(AlgoritmoBusqueda<E> a, char[] pc, List<char[]> textos) {
        for (int r = 0; r < CALENTAMIENTO; r++) {
            E e = a.preprocesar(pc);
            for (char[] t : textos) a.buscar(t, pc, e, null);
        }
        double[] pre = new double[REPETICIONES];
        double[] bus = new double[REPETICIONES];
        for (int r = 0; r < REPETICIONES; r++) {
            long t0 = System.nanoTime();
            E e = a.preprocesar(pc);
            long t1 = System.nanoTime();
            for (char[] t : textos) a.buscar(t, pc, e, null);
            long t2 = System.nanoTime();
            pre[r] = (t1 - t0) / 1e6;
            bus[r] = (t2 - t1) / 1e6;
        }
        return new double[][]{pre, bus};
    }

    private static double mediana(double[] v) {
        double[] c = v.clone();
        java.util.Arrays.sort(c);
        return c[c.length / 2];
    }

    private static char[] concatenar(List<char[]> corpus, int veces) {
        int total = 0;
        for (char[] t : corpus) total += t.length + 1;
        char[] r = new char[total * veces];
        int i = 0;
        for (int v = 0; v < veces; v++) {
            for (char[] t : corpus) {
                for (char c : t) r[i++] = c;
                r[i++] = ' ';
            }
        }
        return r;
    }
}
