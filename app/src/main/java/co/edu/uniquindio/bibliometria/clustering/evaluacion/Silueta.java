package co.edu.uniquindio.bibliometria.clustering.evaluacion;

import java.util.List;
import java.util.Map;

/**
 * Coeficiente de silueta (Rousseeuw, 1987): metrica de evaluacion interna
 * que mide que tan bien separados estan los grupos de una particion.
 * <p>
 * Para cada articulo i de un grupo C:
 * <pre>
 * a(i) = distancia promedio de i a los demas articulos de su propio grupo C
 * b(i) = menor distancia promedio de i a los articulos de otro grupo
 * s(i) = (b(i) - a(i)) / max(a(i), b(i))          (s(i) = 0 si C tiene un solo articulo)
 * S    = promedio de s(i) sobre los n articulos
 * </pre>
 * s(i) cercano a 1 indica que i esta mucho mas cerca de su grupo que de
 * cualquier otro; cercano a 0, que esta en la frontera entre dos grupos;
 * negativo, que estaria mejor en otro grupo. Como valores de referencia,
 * Kaufman y Rousseeuw (1990) consideran que S &lt; 0.25 indica que no hay una
 * estructura de grupos sustancial.
 * <p>
 * Complementa a la correlacion cofenetica: esta mide que tan fielmente el
 * dendrograma completo conserva las distancias; la silueta mide la calidad
 * de los grupos que se obtienen al cortarlo.
 * <p>
 * Complejidad: O(n²), porque cada articulo se compara con todos los demas.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public final class Silueta {

    private Silueta() {
    }

    /**
     * @param distancias  matriz de distancias n x n
     * @param grupos      la particion, cada grupo como lista de ids de articulos
     * @param indicePorId traduce el id de un articulo a su fila en la matriz
     * @return el coeficiente de silueta promedio S en [-1, 1]
     */
    public static double calcular(double[][] distancias, List<List<Integer>> grupos, Map<Integer, Integer> indicePorId) {
        double suma = 0.0;
        int total = 0;
        for (List<Integer> grupo : grupos) {
            for (int id : grupo) {
                total++;
                if (grupo.size() == 1) continue;  // s(i) = 0 por convencion
                int i = indicePorId.get(id);

                // a(i): distancia promedio a su propio grupo (sin contarse a si mismo).
                double a = 0.0;
                for (int otroId : grupo) {
                    if (otroId != id) a += distancias[i][indicePorId.get(otroId)];
                }
                a /= grupo.size() - 1;

                // b(i): el menor promedio de distancia hacia cualquier otro grupo.
                double b = Double.POSITIVE_INFINITY;
                for (List<Integer> otro : grupos) {
                    if (otro == grupo) continue;
                    double promedio = 0.0;
                    for (int otroId : otro) promedio += distancias[i][indicePorId.get(otroId)];
                    promedio /= otro.size();
                    if (promedio < b) b = promedio;
                }

                double maximo = a > b ? a : b;
                suma += maximo == 0.0 ? 0.0 : (b - a) / maximo;
            }
        }
        return total == 0 ? 0.0 : suma / total;
    }
}
