package co.edu.uniquindio.bibliometria.busqueda;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Algoritmo Knuth-Morris-Pratt (KMP), Knuth, Morris y Pratt (1977).
 * <p>
 * <b>Idea:</b> cuando el patron falla despues de haber coincidido q
 * caracteres, esos q caracteres del texto ya se conocen (son P[0..q-1]).
 * La <b>funcion de prefijo</b> (tabla de fallos) {@code pi[q-1]} dice cual
 * es el borde mas largo de P[0..q-1]: el prefijo propio mas largo que
 * tambien es sufijo. El patron se desplaza para alinear ese prefijo con el
 * texto ya leido, y el puntero del texto <b>nunca retrocede</b>.
 * <pre>
 * pi[0] = 0
 * pi[q] = max { k &lt; q+1 : P[0..k-1] == P[q-k+1..q] }
 * </pre>
 * <b>Complejidad:</b> preprocesamiento O(m) y busqueda O(n), ambos por
 * analisis amortizado: el indice q sube como mucho una unidad por
 * caracter leido, asi que en total no puede bajar mas veces de las que
 * subio. La busqueda hace como maximo 2n comparaciones.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public class KnuthMorrisPratt implements AlgoritmoBusqueda<int[]> {

    @Override
    public String getNombre() {
        return "Knuth-Morris-Pratt (KMP)";
    }

    @Override
    public String getComplejidadPreprocesamiento() {
        return "O(m)";
    }

    @Override
    public String getComplejidadBusqueda() {
        return "O(n)";
    }

    /**
     * Calcula la funcion de prefijo {@code pi}. Se compara el patron
     * contra si mismo: k es la longitud del borde actual; si el siguiente
     * caracter no extiende el borde, se prueba con el borde del borde
     * ({@code k = pi[k-1]}) hasta que lo extienda o k llegue a 0.
     *
     * @return pi, de longitud m
     */
    @Override
    public int[] preprocesar(char[] patron) {
        int m = patron.length;
        int[] pi = new int[m];
        int k = 0;
        for (int q = 1; q < m; q++) {
            while (k > 0 && patron[k] != patron[q]) {
                k = pi[k - 1];
            }
            if (patron[k] == patron[q]) {
                k++;
            }
            pi[q] = k;
        }
        return pi;
    }

    /**
     * Recorre el texto una sola vez. q es cuantos caracteres del patron
     * coinciden con los ultimos q caracteres leidos; el patron esta
     * alineado en {@code s = i - q}. Al fallar con q &gt; 0 el patron se
     * desplaza {@code q - pi[q-1]} posiciones y se vuelve a comparar el
     * mismo T[i]; al fallar con q = 0 avanza una posicion.
     */
    @Override
    public ResultadoBusqueda buscar(char[] texto, char[] patron, int[] pi, Traza traza) {
        ResultadoBusqueda resultado = new ResultadoBusqueda();
        int n = texto.length;
        int m = patron.length;
        if (m == 0 || m > n) return resultado;

        int q = 0;                 // caracteres del patron que coinciden
        int s = 0;                 // posicion actual del inicio del patron
        int comparacionesAlineamiento = 0;
        resultado.sumarAlineamiento();
        for (int i = 0; i < n; i++) {
            while (true) {
                resultado.sumarComparaciones(1);
                comparacionesAlineamiento++;
                if (patron[q] == texto[i]) {
                    q++;
                    if (q == m) {
                        // Ocurrencia completa: se reporta y se sigue desde el borde de P.
                        resultado.agregarPosicion(i - m + 1);
                        int nuevoQ = pi[m - 1];
                        if (traza != null) {
                            traza.registrar(s, comparacionesAlineamiento, "ocurrencia", m - nuevoQ);
                        }
                        q = nuevoQ;
                        s = i + 1 - q;
                        comparacionesAlineamiento = 0;
                        if (s <= n - m) resultado.sumarAlineamiento();
                    }
                    break;
                }
                if (q == 0) {
                    // Fallo en el primer caracter: el patron avanza una posicion.
                    if (traza != null) {
                        traza.registrar(s, comparacionesAlineamiento,
                                "fallo en j=0 ('" + texto[i] + "' vs '" + patron[0] + "')", 1);
                    }
                    s = i + 1;
                    comparacionesAlineamiento = 0;
                    if (s <= n - m) resultado.sumarAlineamiento();
                    break;
                }
                // Fallo con q > 0: se reutiliza el borde P[0..pi[q-1]-1] y se compara de nuevo T[i].
                int nuevoQ = pi[q - 1];
                if (traza != null) {
                    traza.registrar(s, comparacionesAlineamiento,
                            "fallo en j=" + q + " ('" + texto[i] + "' vs '" + patron[q] + "'), q = pi[" + (q - 1) + "] = " + nuevoQ,
                            q - nuevoQ);
                }
                s += q - nuevoQ;
                q = nuevoQ;
                comparacionesAlineamiento = 0;
                if (s > n - m) break;
                resultado.sumarAlineamiento();
            }
            if (s > n - m) break;  // el patron ya no cabe en lo que queda del texto
        }
        return resultado;
    }

    @Override
    public Map<String, Object> describirEstructura(char[] patron, int[] pi) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", "Funcion de prefijo pi (tabla de fallos)");
        mapa.put("patron", String.valueOf(patron));
        mapa.put("pi", pi);
        return mapa;
    }
}
