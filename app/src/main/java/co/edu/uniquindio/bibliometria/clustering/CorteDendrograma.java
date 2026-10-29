package co.edu.uniquindio.bibliometria.clustering;

import java.util.ArrayList;
import java.util.List;

/**
 * Corta un dendrograma en k grupos ("clusters planos").
 * <p>
 * El dendrograma guarda todas las fusiones, desde 20 grupos de un articulo
 * hasta un solo grupo con los 20. Para obtener k grupos se deshacen las
 * k - 1 fusiones mas altas: se empieza con la raiz y, mientras haya menos de
 * k grupos, se reemplaza el grupo cuya fusion ocurrio a mayor altura por sus
 * dos hijos. Equivale a trazar una linea horizontal sobre el dendrograma justo
 * por debajo de esas k - 1 fusiones.
 * <p>
 * Esto es correcto porque los tres criterios de enlace (simple, completo y
 * promedio) producen dendrogramas <b>monotonos</b>: cada fusion ocurre a una
 * altura mayor o igual que las fusiones de sus hijos, asi que las k - 1
 * fusiones mas altas son siempre las ultimas que hizo el algoritmo.
 * <p>
 * Complejidad: O(k·n), con k cortes sobre como mucho n grupos.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public final class CorteDendrograma {

    private CorteDendrograma() {
    }

    /**
     * @param raiz raiz del dendrograma
     * @param k    numero de grupos deseado (1 a numero de hojas)
     * @return los k grupos, cada uno como la lista de ids de articulos que contiene
     */
    public static List<List<Integer>> cortar(NodoDendrograma raiz, int k) {
        List<NodoDendrograma> grupos = new ArrayList<>();
        grupos.add(raiz);
        while (grupos.size() < k) {
            // El grupo (no hoja) que se formo con la fusion mas alta.
            int indiceMasAlto = -1;
            for (int i = 0; i < grupos.size(); i++) {
                NodoDendrograma g = grupos.get(i);
                if (!g.isEsHoja() && (indiceMasAlto < 0 || g.getAltura() > grupos.get(indiceMasAlto).getAltura())) {
                    indiceMasAlto = i;
                }
            }
            if (indiceMasAlto < 0) break;  // todos son hojas: no se puede cortar mas
            NodoDendrograma separado = grupos.remove(indiceMasAlto);
            grupos.add(separado.getIzquierdo());
            grupos.add(separado.getDerecho());
        }
        List<List<Integer>> resultado = new ArrayList<>();
        for (NodoDendrograma g : grupos) {
            resultado.add(g.getMiembros());
        }
        return resultado;
    }
}
