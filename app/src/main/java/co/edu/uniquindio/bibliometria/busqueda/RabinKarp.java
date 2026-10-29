package co.edu.uniquindio.bibliometria.busqueda;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Algoritmo Rabin-Karp (Karp y Rabin, 1987), basado en aritmetica modular y
 * <b>rolling hash</b>.
 * <p>
 * <b>Idea:</b> cada cadena de longitud m se interpreta como un numero en
 * base d (d = 256, un "digito" por caracter) y se reduce modulo un primo q:
 * <pre>
 * hash(P) = (P[0]·d^(m-1) + P[1]·d^(m-2) + ... + P[m-1]) mod q
 * </pre>
 * Solo se comparan caracteres en las ventanas del texto cuyo hash es igual
 * al del patron. El hash de la ventana siguiente se obtiene del anterior en
 * O(1), quitando el caracter que sale y agregando el que entra:
 * <pre>
 * h = d^(m-1) mod q
 * t(s+1) = ( d·(t(s) - T[s]·h) + T[s+m] ) mod q
 * </pre>
 * Dos cadenas distintas pueden tener el mismo hash (<b>colision</b>); por
 * eso cada coincidencia de hash se verifica caracter por caracter. Con un
 * modulo pequeno (por ejemplo q = 101) las colisiones son frecuentes, lo
 * que sirve para el caso de estudio; con un primo grande casi no ocurren.
 * <p>
 * <b>Complejidad:</b> preprocesamiento O(m). Busqueda O(n + m) esperada
 * (n - m + 1 ventanas de costo O(1) mas O(m) por cada coincidencia de
 * hash) y O(n·m) en el peor caso, si casi todas las ventanas colisionan.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public class RabinKarp implements AlgoritmoBusqueda<RabinKarp.Hash> {

    /** Base: cada caracter es un "digito" en base 256. */
    public static final long BASE = 256;
    /** Primo grande por defecto: practicamente sin colisiones. */
    public static final long MODULO_POR_DEFECTO = 1_000_000_007L;
    /** Primo pequeno para el caso de estudio: produce colisiones visibles. */
    public static final long MODULO_DEMOSTRACION = 101L;

    /** Primo q usado para reducir los hashes. */
    private final long modulo;

    /** Estructura auxiliar: hash del patron y potencia h = d^(m-1) mod q. */
    public static class Hash {
        final long hashPatron;
        final long potencia;

        Hash(long hashPatron, long potencia) {
            this.hashPatron = hashPatron;
            this.potencia = potencia;
        }
    }

    /** Rabin-Karp con el primo grande por defecto. */
    public RabinKarp() {
        this(MODULO_POR_DEFECTO);
    }

    /** @param modulo primo q para reducir los hashes */
    public RabinKarp(long modulo) {
        this.modulo = modulo;
    }

    public long getModulo() {
        return modulo;
    }

    @Override
    public String getNombre() {
        return "Rabin-Karp";
    }

    @Override
    public String getComplejidadPreprocesamiento() {
        return "O(m)";
    }

    @Override
    public String getComplejidadBusqueda() {
        return "O(n + m) esperado, O(n·m) peor caso";
    }

    /**
     * Calcula hash(P) con la regla de Horner (un caracter por paso) y la
     * potencia h = d^(m-1) mod q por multiplicaciones sucesivas.
     */
    @Override
    public Hash preprocesar(char[] patron) {
        int m = patron.length;
        long hashPatron = 0;
        for (int i = 0; i < m; i++) {
            hashPatron = (hashPatron * BASE + patron[i]) % modulo;
        }
        long potencia = 1;
        for (int i = 1; i < m; i++) {
            potencia = (potencia * BASE) % modulo;
        }
        return new Hash(hashPatron, potencia);
    }

    /** Hash de T[inicio..inicio+m-1] con la regla de Horner (solo para la primera ventana). */
    private long hashInicial(char[] texto, int m) {
        long hash = 0;
        for (int i = 0; i < m; i++) {
            hash = (hash * BASE + texto[i]) % modulo;
        }
        return hash;
    }

    /**
     * Recorre las n - m + 1 ventanas actualizando el hash en O(1). Cuando el
     * hash coincide con el del patron, verifica caracter por caracter: si
     * todos coinciden es una ocurrencia; si no, es una colision espuria.
     */
    @Override
    public ResultadoBusqueda buscar(char[] texto, char[] patron, Hash estructura, Traza traza) {
        ResultadoBusqueda resultado = new ResultadoBusqueda();
        int n = texto.length;
        int m = patron.length;
        if (m == 0 || m > n) return resultado;

        long hashVentana = hashInicial(texto, m);
        for (int s = 0; s <= n - m; s++) {
            resultado.sumarAlineamiento();
            int comparaciones = 0;
            int j = -1;  // -1: el hash no coincidio y no hubo verificacion
            if (hashVentana == estructura.hashPatron) {
                resultado.sumarCoincidenciaHash();
                // Verificacion caracter por caracter (puede ser una colision).
                j = 0;
                while (j < m) {
                    comparaciones++;
                    if (texto[s + j] != patron[j]) break;
                    j++;
                }
                resultado.sumarComparaciones(comparaciones);
                if (j == m) {
                    resultado.agregarPosicion(s);
                } else {
                    resultado.sumarColision();
                }
            }
            // El texto del evento solo se arma si se pidio traza, para no afectar los tiempos medidos.
            if (traza != null) {
                String evento;
                if (j < 0) {
                    evento = "hash = " + hashVentana + " != " + estructura.hashPatron;
                } else if (j == m) {
                    evento = "hash = " + hashVentana + " = hash(P); verificado: ocurrencia";
                } else {
                    evento = "hash = " + hashVentana + " = hash(P); verificado: COLISION espuria (falla en j=" + j + ")";
                }
                traza.registrar(s, comparaciones, evento, 1);
            }

            // Rolling hash: quitar T[s] (que pesa h) y agregar T[s+m].
            if (s < n - m) {
                long sinPrimero = (hashVentana - (texto[s] * estructura.potencia) % modulo + modulo) % modulo;
                hashVentana = (sinPrimero * BASE + texto[s + m]) % modulo;
            }
        }
        return resultado;
    }

    @Override
    public Map<String, Object> describirEstructura(char[] patron, Hash estructura) {
        // Paso a paso de la regla de Horner sobre el patron, para el caso de estudio.
        Map<String, Object> horner = new LinkedHashMap<>();
        long hash = 0;
        for (int i = 0; i < patron.length; i++) {
            long anterior = hash;
            hash = (hash * BASE + patron[i]) % modulo;
            horner.put("i=" + i + " '" + patron[i] + "' (" + (int) patron[i] + ")",
                    "(" + anterior + "·" + BASE + " + " + (int) patron[i] + ") mod " + modulo + " = " + hash);
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("nombre", "Hash del patron (aritmetica modular)");
        mapa.put("patron", String.valueOf(patron));
        mapa.put("base", BASE);
        mapa.put("modulo", modulo);
        mapa.put("potencia", "h = " + BASE + "^" + (patron.length - 1) + " mod " + modulo + " = " + estructura.potencia);
        mapa.put("hashPatron", estructura.hashPatron);
        mapa.put("horner", horner);
        return mapa;
    }
}
