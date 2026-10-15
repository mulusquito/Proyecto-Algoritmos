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

    private final String termino;
    private final int frecuencia;

    public TerminoFrecuencia(String termino, int frecuencia) {
        if (frecuencia < 0) {
            throw new IllegalArgumentException("La frecuencia no puede ser negativa: " + frecuencia);
        }
        this.termino = termino;
        this.frecuencia = frecuencia;
    }

    public String getTermino() {
        return termino;
    }

    public int getFrecuencia() {
        return frecuencia;
    }

    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("termino", termino);
        mapa.put("frecuencia", frecuencia);
        return mapa;
    }

    @Override
    public String toString() {
        return termino + "=" + frecuencia;
    }
}
