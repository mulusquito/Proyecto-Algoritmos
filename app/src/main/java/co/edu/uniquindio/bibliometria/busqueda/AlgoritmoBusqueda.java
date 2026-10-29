package co.edu.uniquindio.bibliometria.busqueda;

import java.util.Map;

/**
 * Contrato comun de los tres algoritmos de coincidencia exacta de cadenas
 * del Requerimiento 3 (Knuth-Morris-Pratt, Boyer-Moore y Rabin-Karp).
 * <p>
 * Cada algoritmo trabaja en dos fases que se miden por separado, porque el
 * enunciado pide comparar el costo del <b>preprocesamiento</b> del patron
 * frente al de la <b>busqueda</b> sobre el texto:
 * <ol>
 *     <li>{@link #preprocesar(char[])}: construye la estructura auxiliar del
 *     patron (tabla de fallos en KMP, tablas de caracter malo y sufijo bueno
 *     en Boyer-Moore, hash del patron en Rabin-Karp). Depende solo de m.</li>
 *     <li>{@link #buscar(char[], char[], Object, Traza)}: recorre el texto
 *     usando esa estructura. Depende de n (y de m).</li>
 * </ol>
 * Las implementaciones solo usan arreglos y comparaciones de caracteres; no
 * se usa ninguna funcion de busqueda del lenguaje ({@code indexOf},
 * {@code contains}, expresiones regulares, etc.).
 *
 * @param <E> tipo de la estructura auxiliar que produce el preprocesamiento
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public interface AlgoritmoBusqueda<E> {

    /** Nombre legible del algoritmo. */
    String getNombre();

    /** Complejidad teorica del preprocesamiento (texto para la interfaz y el documento). */
    String getComplejidadPreprocesamiento();

    /** Complejidad teorica de la busqueda (texto para la interfaz y el documento). */
    String getComplejidadBusqueda();

    /**
     * Construye la estructura auxiliar del patron.
     *
     * @param patron patron de busqueda (longitud m &gt;= 1)
     * @return estructura auxiliar, reutilizable para buscar en cualquier texto
     */
    E preprocesar(char[] patron);

    /**
     * Busca todas las ocurrencias exactas del patron en el texto.
     *
     * @param texto      texto donde se busca (longitud n)
     * @param patron     el mismo patron que se preproceso
     * @param estructura resultado de {@link #preprocesar(char[])}
     * @param traza      donde registrar cada alineamiento, o {@code null} para no registrar nada
     * @return posiciones de inicio de cada ocurrencia y contadores de operaciones
     */
    ResultadoBusqueda buscar(char[] texto, char[] patron, E estructura, Traza traza);

    /**
     * Describe la estructura auxiliar en forma de mapa (para mostrarla en la
     * interfaz y en el caso de estudio del documento).
     */
    Map<String, Object> describirEstructura(char[] patron, E estructura);
}
