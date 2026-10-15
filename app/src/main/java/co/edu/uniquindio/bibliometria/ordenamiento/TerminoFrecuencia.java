package co.edu.uniquindio.bibliometria.ordenamiento;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Par (termino, frecuencia) que se ordena en el ranking de terminos. La
 * <b>clave</b> de ordenamiento es la frecuencia (un entero no negativo); el
 * termino es el dato "satelite" que viaja junto a su clave. Los algoritmos
 * de ordenamiento mueven el par completo, nunca solo el numero.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public final class TerminoFrecuencia {

    /** La palabra (dato satelite). */
    private final String termino;
    /** Cuantas veces aparece la palabra en el corpus (clave de ordenamiento). */
    private final int frecuencia;

    /**
     * @param termino    la palabra
     * @param frecuencia cuantas veces aparece
     * @throws IllegalArgumentException si la frecuencia es negativa (los algoritmos la usan como indice)
     */
    public TerminoFrecuencia(String termino, int frecuencia) {
        if (frecuencia < 0) {
            throw new IllegalArgumentException("La frecuencia no puede ser negativa: " + frecuencia);
        }
        this.termino = termino;
        this.frecuencia = frecuencia;
    }

    /** @return la palabra */
    public String getTermino() {
        return termino;
    }

    /** @return cuantas veces aparece la palabra en el corpus */
    public int getFrecuencia() {
        return frecuencia;
    }

    /** @return el par como mapa {termino, frecuencia}, listo para convertir a JSON */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("termino", termino);
        mapa.put("frecuencia", frecuencia);
        return mapa;
    }

    /** Representacion "termino=frecuencia" (por ejemplo "ai=86"), usada en las pruebas. */
    @Override
    public String toString() {
        return termino + "=" + frecuencia;
    }
}
