package co.edu.uniquindio.bibliometria.busqueda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro paso a paso de los alineamientos que hace un algoritmo de
 * busqueda: en que posicion del texto se coloco el patron, cuantos
 * caracteres comparo, que paso y cuanto se desplazo despues. Sirve para
 * "graficar las iteraciones y desplazamientos sobre el texto" que pide el
 * enunciado, tanto en la interfaz como en el documento.
 * <p>
 * Tiene un limite de pasos para que un texto largo no produzca una
 * respuesta enorme; los pasos que lo superan se cuentan pero no se guardan.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public class Traza {

    /** Maximo de pasos que se guardan. */
    private final int limite;
    /** Pasos guardados, en el orden en que ocurrieron. */
    private final List<Map<String, Object>> pasos = new ArrayList<>();
    /** Pasos totales, incluidos los que no se guardaron por el limite. */
    private int totalPasos = 0;

    /** @param limite maximo de pasos que se guardan */
    public Traza(int limite) {
        this.limite = limite;
    }

    /**
     * Registra un alineamiento.
     *
     * @param posicion       posicion s del texto donde estaba alineado el inicio del patron
     * @param comparaciones  caracteres comparados en este alineamiento
     * @param evento         que ocurrio (coincidencia, fallo en j, hash distinto, colision...)
     * @param desplazamiento cuantas posiciones avanza el patron despues de este paso
     */
    public void registrar(int posicion, int comparaciones, String evento, int desplazamiento) {
        totalPasos++;
        if (pasos.size() >= limite) return;
        Map<String, Object> paso = new LinkedHashMap<>();
        paso.put("paso", totalPasos);
        paso.put("posicion", posicion);
        paso.put("comparaciones", comparaciones);
        paso.put("evento", evento);
        paso.put("desplazamiento", desplazamiento);
        pasos.add(paso);
    }

    /** @return pasos guardados */
    public List<Map<String, Object>> getPasos() {
        return pasos;
    }

    /** @return pasos totales, incluidos los que no se guardaron */
    public int getTotalPasos() {
        return totalPasos;
    }
}
