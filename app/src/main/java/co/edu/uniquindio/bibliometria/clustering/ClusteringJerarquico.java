package co.edu.uniquindio.bibliometria.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algoritmo de <b>clustering (agrupamiento) jerarquico aglomerativo</b>,
 * implementado desde cero mediante las formulas de actualizacion de
 * Lance-Williams (Lance &amp; Williams, 1967), que permiten expresar los
 * tres criterios de enlace exigidos por el Requerimiento 2
 * ({@link EnlaceSimple}, {@link EnlaceCompleto}, {@link EnlacePromedio})
 * como una unica rutina generica.
 * <p>
 * El algoritmo parte de N clusteres unitarios (uno por abstract) y, en
 * cada iteracion, fusiona el par de clusteres con menor distancia entre
 * si, hasta que queda un unico cluster (la raiz del dendrograma). No se usa
 * ninguna libreria de clustering (scikit-learn, Weka, Apache Commons Math,
 * etc.): la busqueda del par minimo y la actualizacion de distancias se
 * hacen con estructuras basicas (mapas y bucles).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ClusteringJerarquico {

    /**
     * Ejecuta el algoritmo sobre una matriz de distancias N x N (simetrica,
     * diagonal en 0) y devuelve el dendrograma resultante junto con la
     * secuencia de fusiones realizadas.
     *
     * @param distancias  matriz de distancias entre los N elementos originales
     * @param etiquetas   etiqueta legible de cada elemento (mismo orden que la matriz)
     * @param idsOriginales id de articulo correspondiente a cada fila/columna
     * @param estrategia  criterio de enlace (single/complete/average)
     * @return dendrograma (raiz) y lista de las n-1 fusiones realizadas
     */
    public ResultadoClustering ejecutar(double[][] distancias, List<String> etiquetas,
                                         List<Integer> idsOriginales, EstrategiaEnlace estrategia) {
        int n = distancias.length;

        // Estado del algoritmo, indexado por id de cluster:
        //  - nodos:   subarbol del dendrograma que representa cada cluster
        //  - tamanos: numero de articulos originales en cada cluster
        //  - dist:    distancia entre cada par de clusteres activos (dist.get(a).get(b))
        Map<Integer, NodoDendrograma> nodos = new HashMap<>();
        Map<Integer, Integer> tamanos = new HashMap<>();
        Map<Integer, Map<Integer, Double>> dist = new HashMap<>();

        // Inicio: cada articulo es un cluster de tamano 1 (una hoja).
        for (int i = 0; i < n; i++) {
            nodos.put(i, new NodoDendrograma(i, idsOriginales.get(i), etiquetas.get(i)));
            tamanos.put(i, 1);
            Map<Integer, Double> fila = new HashMap<>();
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    fila.put(j, distancias[i][j]);
                }
            }
            dist.put(i, fila);
        }

        List<PasoFusion> pasos = new ArrayList<>();
        // Los clusteres nuevos reciben ids a partir de n para no chocar con las hojas.
        int siguienteId = n;
        // Clusteres que todavia no se han fusionado con otro.
        List<Integer> activos = new ArrayList<>();
        for (int i = 0; i < n; i++) activos.add(i);

        // Cada vuelta fusiona dos clusteres; termina cuando solo queda uno (n-1 vueltas).
        while (activos.size() > 1) {
            // 1. Buscar el par de clusteres activos con la menor distancia.
            int mejorI = -1, mejorJ = -1;
            double mejorDistancia = Double.POSITIVE_INFINITY;

            for (int a = 0; a < activos.size(); a++) {
                for (int b = a + 1; b < activos.size(); b++) {
                    int ci = activos.get(a);
                    int cj = activos.get(b);
                    double d = dist.get(ci).get(cj);
                    if (d < mejorDistancia) {
                        mejorDistancia = d;
                        mejorI = ci;
                        mejorJ = cj;
                    }
                }
            }

            // 2. Fusionarlos en un nuevo nodo cuya altura es la distancia a la que se unieron.
            int tamI = tamanos.get(mejorI);
            int tamJ = tamanos.get(mejorJ);
            NodoDendrograma nuevoNodo = new NodoDendrograma(siguienteId, mejorDistancia,
                    nodos.get(mejorI), nodos.get(mejorJ));
            int tamNuevo = tamI + tamJ;

            pasos.add(new PasoFusion(mejorI, mejorJ, mejorDistancia, siguienteId, tamNuevo,
                    nodos.get(mejorI).getMiembros(), nodos.get(mejorJ).getMiembros()));

            // 3. Distancia del nuevo cluster a cada cluster M restante, con la formula
            //    de Lance-Williams del criterio de enlace elegido.
            Map<Integer, Double> filaNueva = new HashMap<>();
            for (int m : activos) {
                if (m == mejorI || m == mejorJ) continue;
                double dIM = dist.get(mejorI).get(m);
                double dJM = dist.get(mejorJ).get(m);
                double nuevaDistancia = estrategia.distanciaFusion(dIM, dJM, tamI, tamJ, tamanos.get(m));
                filaNueva.put(m, nuevaDistancia);
                dist.get(m).put(siguienteId, nuevaDistancia);
            }
            dist.put(siguienteId, filaNueva);

            // 4. Actualizar el estado: salen I y J, entra el nuevo cluster.
            activos.remove(Integer.valueOf(mejorI));
            activos.remove(Integer.valueOf(mejorJ));
            activos.add(siguienteId);

            nodos.put(siguienteId, nuevoNodo);
            tamanos.put(siguienteId, tamNuevo);
            dist.remove(mejorI);
            dist.remove(mejorJ);
            for (Map<Integer, Double> fila : dist.values()) {
                fila.remove(mejorI);
                fila.remove(mejorJ);
            }

            siguienteId++;
        }

        // El unico cluster que queda contiene todos los articulos: es la raiz del dendrograma.
        NodoDendrograma raiz = nodos.get(activos.get(0));
        return new ResultadoClustering(estrategia.getNombre(), raiz, pasos);
    }
}
