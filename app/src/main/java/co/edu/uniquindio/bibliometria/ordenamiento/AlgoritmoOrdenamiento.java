package co.edu.uniquindio.bibliometria.ordenamiento;

/**
 * Contrato comun de los tres algoritmos de ordenamiento <b>no basados en
 * comparaciones</b> del seguimiento (Counting Sort, Radix Sort y Bucket
 * Sort). Todos ordenan por frecuencia de mayor a menor y son
 * <b>estables</b>: los terminos con la misma frecuencia conservan el orden
 * relativo que tenian en la entrada. Gracias a esa estabilidad los tres
 * algoritmos producen exactamente el mismo ranking, lo que permite validar
 * unos contra otros.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public interface AlgoritmoOrdenamiento {

    /** Nombre legible del algoritmo. */
    String getNombre();

    /** Complejidad asintotica teorica (texto para la interfaz y el documento). */
    String getComplejidad();

    /**
     * Ordena los terminos por frecuencia descendente sin modificar el
     * arreglo de entrada.
     *
     * @param entrada terminos con su frecuencia (frecuencias &gt;= 0)
     * @return nuevo arreglo ordenado de mayor a menor frecuencia
     */
    TerminoFrecuencia[] ordenarDescendente(TerminoFrecuencia[] entrada);
}
