package co.edu.uniquindio.bibliometria.busqueda;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Algoritmo Boyer-Moore (Boyer y Moore, 1977), con las dos heuristicas de
 * desplazamiento: <b>caracter malo</b> y <b>sufijo bueno</b> (version
 * fuerte).
 * <p>
 * <b>Idea:</b> el patron se compara de <b>derecha a izquierda</b>. Cuando
 * falla en la posicion j con el caracter c del texto, se calculan dos
 * desplazamientos seguros y se usa el mayor:
 * <ul>
 *     <li><b>Caracter malo:</b> alinear c con su ultima aparicion en el
 *     patron: {@code j - ultima(c)}, o saltar el patron completo si c no
 *     esta en el patron ({@code ultima(c) = -1}).</li>
 *     <li><b>Sufijo bueno:</b> el sufijo P[j+1..m-1] ya coincidio; se
 *     desplaza hasta la siguiente aparicion de ese sufijo en el patron
 *     (precedida por un caracter distinto de P[j]) o, si no hay, hasta el
 *     prefijo mas largo de P que sea sufijo de ese tramo.</li>
 * </ul>
 * <b>Complejidad:</b> preprocesamiento O(m + sigma), con sigma = 256 el
 * tamano del alfabeto de la tabla de caracter malo. Busqueda O(n·m) en el
 * peor caso teorico de esta version (texto y patron muy repetitivos), pero
 * en texto natural es <b>sublineal</b>: cerca de O(n/m), porque la mayoria
 * de los fallos ocurre en el primer caracter comparado y el patron salta m
 * posiciones.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public class BoyerMoore implements AlgoritmoBusqueda<BoyerMoore.Tablas> {

    /** Tamano del alfabeto de la tabla de caracter malo (codigos 0-255). */
    public static final int ALFABETO = 256;

    /** Las dos tablas heuristicas del patron. */
    public static class Tablas {
        /** caracterMalo[c] = ultima posicion de c en el patron, o -1 si no aparece. */
        final int[] caracterMalo;
        /** sufijoBueno[j] = desplazamiento si falla en j-1 (sufijoBueno[0]: tras una ocurrencia completa). */
        final int[] sufijoBueno;
        /** borde[i] = inicio del borde mas ancho del sufijo P[i..m-1] (auxiliar del sufijo bueno). */
        final int[] borde;

        Tablas(int[] caracterMalo, int[] sufijoBueno, int[] borde) {
            this.caracterMalo = caracterMalo;
            this.sufijoBueno = sufijoBueno;
            this.borde = borde;
        }
    }

    @Override
    public String getNombre() {
        return "Boyer-Moore";
    }

    @Override
    public String getComplejidadPreprocesamiento() {
        return "O(m + sigma)";
    }

    @Override
    public String getComplejidadBusqueda() {
        return "O(n·m) peor caso, ~O(n/m) en texto natural";
    }

    @Override
    public Tablas preprocesar(char[] patron) {
        int m = patron.length;

        // 1. Tabla de caracter malo: se recorre el patron de izquierda a derecha,
        //    asi que al final queda la ULTIMA aparicion de cada caracter.
        int[] caracterMalo = new int[ALFABETO];
        for (int c = 0; c < ALFABETO; c++) {
            caracterMalo[c] = -1;
        }
        for (int j = 0; j < m; j++) {
            if (patron[j] < ALFABETO) {
                caracterMalo[patron[j]] = j;
            }
        }

        // 2. Tabla de sufijo bueno (regla fuerte).
        int[] sufijoBueno = new int[m + 1];
        int[] borde = new int[m + 1];

        // Caso 1: el sufijo coincidente aparece otra vez dentro del patron.
        // borde[i] guarda donde empieza el borde mas ancho de P[i..m-1]; cuando ese borde
        // no se puede extender hacia la izquierda, el desplazamiento j - i lleva esa
        // otra aparicion bajo el texto ya comparado.
        int i = m;
        int j = m + 1;
        borde[i] = j;
        while (i > 0) {
            while (j <= m && patron[i - 1] != patron[j - 1]) {
                if (sufijoBueno[j] == 0) {
                    sufijoBueno[j] = j - i;
                }
                j = borde[j];
            }
            i--;
            j--;
            borde[i] = j;
        }

        // Caso 2: solo una parte del sufijo coincidente es prefijo del patron.
        // Se usa el borde mas ancho de todo el patron (borde[0]) mientras quepa.
        j = borde[0];
        for (i = 0; i <= m; i++) {
            if (sufijoBueno[i] == 0) {
                sufijoBueno[i] = j;
            }
            if (i == j) {
                j = borde[j];
            }
        }
        return new Tablas(caracterMalo, sufijoBueno, borde);
    }

    /**
     * Ultima posicion de c en el patron. Los caracteres fuera del alfabeto
     * de la tabla (codigo &gt;= 256, raros en los abstracts) se buscan
     * recorriendo el patron de derecha a izquierda.
     */
    private static int ultimaAparicion(char c, char[] patron, int[] caracterMalo) {
        if (c < ALFABETO) {
            return caracterMalo[c];
        }
        for (int j = patron.length - 1; j >= 0; j--) {
            if (patron[j] == c) return j;
        }
        return -1;
    }

    /**
     * Coloca el patron en s y compara de derecha a izquierda. Si coincide
     * completo, reporta s y desplaza {@code sufijoBueno[0]}; si falla en j,
     * desplaza {@code max(sufijoBueno[j+1], j - ultima(T[s+j]))}.
     */
    @Override
    public ResultadoBusqueda buscar(char[] texto, char[] patron, Tablas tablas, Traza traza) {
        ResultadoBusqueda resultado = new ResultadoBusqueda();
        int n = texto.length;
        int m = patron.length;
        if (m == 0 || m > n) return resultado;

        int s = 0;
        while (s <= n - m) {
            resultado.sumarAlineamiento();
            int j = m - 1;
            int comparaciones = 1;
            while (j >= 0 && patron[j] == texto[s + j]) {
                j--;
                if (j >= 0) comparaciones++;
            }
            resultado.sumarComparaciones(comparaciones);

            if (j < 0) {
                resultado.agregarPosicion(s);
                int desplazamiento = tablas.sufijoBueno[0];
                if (traza != null) {
                    traza.registrar(s, comparaciones, "ocurrencia; sufijo bueno = " + desplazamiento, desplazamiento);
                }
                s += desplazamiento;
            } else {
                char c = texto[s + j];
                int porCaracterMalo = j - ultimaAparicion(c, patron, tablas.caracterMalo);
                int porSufijoBueno = tablas.sufijoBueno[j + 1];
                int desplazamiento = Math.max(porCaracterMalo, porSufijoBueno);
                if (traza != null) {
                    traza.registrar(s, comparaciones,
                            "fallo en j=" + j + " ('" + c + "' vs '" + patron[j] + "'); caracter malo = "
                                    + porCaracterMalo + ", sufijo bueno = " + porSufijoBueno,
                            desplazamiento);
                }
                s += desplazamiento;
            }
        }
        return resultado;
    }

    @Override
    public Map<String, Object> describirEstructura(char[] patron, Tablas tablas) {
        // Solo se muestran las entradas de los caracteres que aparecen en el patron;
        // el resto del alfabeto vale -1.
        Map<String, Object> caracterMalo = new LinkedHashMap<>();
        for (char c : patron) {
            caracterMalo.put("'" + c + "'", ultimaAparicion(c, patron, tablas.caracterMalo));
        }
        caracterMalo.put("cualquier otro", -1);

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", "Tablas de caracter malo y sufijo bueno");
        mapa.put("patron", String.valueOf(patron));
        mapa.put("caracterMalo", caracterMalo);
        mapa.put("sufijoBueno", tablas.sufijoBueno);
        mapa.put("borde", tablas.borde);
        return mapa;
    }
}
