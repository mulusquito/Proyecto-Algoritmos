package co.edu.uniquindio.bibliometria.busqueda;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de buscar un patron en un texto con uno de los tres algoritmos:
 * las posiciones donde empieza cada ocurrencia y los contadores de
 * operaciones que permiten contrastar el analisis teorico (Big O) con lo
 * que realmente hizo el algoritmo.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public class ResultadoBusqueda {

    /** Posiciones (base 0) donde empieza cada ocurrencia, en orden creciente. */
    private final List<Integer> posiciones = new ArrayList<>();
    /** Comparaciones de caracteres patron-texto realizadas. */
    private long comparaciones = 0;
    /** Alineamientos del patron sobre el texto (ventanas evaluadas). */
    private long alineamientos = 0;
    /** Rabin-Karp: ventanas cuyo hash coincidio con el del patron. */
    private long coincidenciasHash = 0;
    /** Rabin-Karp: coincidencias de hash que no eran ocurrencias reales (colisiones espurias). */
    private long colisiones = 0;

    /** Agrega una ocurrencia que empieza en la posicion indicada. */
    void agregarPosicion(int posicion) {
        posiciones.add(posicion);
    }

    void sumarComparaciones(long cantidad) {
        comparaciones += cantidad;
    }

    void sumarAlineamiento() {
        alineamientos++;
    }

    void sumarCoincidenciaHash() {
        coincidenciasHash++;
    }

    void sumarColision() {
        colisiones++;
    }

    public List<Integer> getPosiciones() {
        return posiciones;
    }

    public long getComparaciones() {
        return comparaciones;
    }

    public long getAlineamientos() {
        return alineamientos;
    }

    public long getCoincidenciasHash() {
        return coincidenciasHash;
    }

    public long getColisiones() {
        return colisiones;
    }
}
