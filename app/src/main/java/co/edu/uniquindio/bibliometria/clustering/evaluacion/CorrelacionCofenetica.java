package co.edu.uniquindio.bibliometria.clustering.evaluacion;

import co.edu.uniquindio.bibliometria.clustering.NodoDendrograma;

import java.util.List;
import java.util.Map;

/**
 * Metrica matematica de evaluacion INTERNA para comparar la coherencia de
 * distintos resultados de clustering jerarquico, exigida por el
 * Requerimiento 2: el <b>Coeficiente de Correlacion Cofenetica</b>
 * (Sokal &amp; Rohlf, 1962).
 * <p>
 * Definiciones:
 * <ul>
 *     <li><b>Distancia cofenetica</b> entre dos elementos i, j: la altura
 *     (distancia) a la que el dendrograma los fusiona por primera vez en un
 *     mismo cluster.</li>
 *     <li><b>Correlacion cofenetica</b>: coeficiente de correlacion de
 *     Pearson entre las distancias originales (las que entraron al
 *     algoritmo) y las distancias cofeneticas resultantes del
 *     dendrograma.</li>
 * </ul>
 * Un valor cercano a 1 indica que el dendrograma preserva fielmente las
 * distancias/similitudes originales entre los abstracts, es decir, que el
 * agrupamiento es mas coherente con los datos. Se implementa enteramente
 * desde cero (recorrido del arbol + calculo manual de la correlacion de
 * Pearson), sin usar Apache Commons Math ni ninguna libreria estadistica.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class CorrelacionCofenetica {

    /**
     * Calcula la matriz de distancias cofeneticas del dendrograma, indexada
     * en el mismo orden (0..n-1) que la matriz de distancias original.
     *
     * @param raiz          raiz del dendrograma
     * @param indicePorId   mapa articuloId -&gt; indice (0..n-1) usado en la matriz original
     */
    public double[][] calcularMatrizCofenetica(NodoDendrograma raiz, Map<Integer, Integer> indicePorId, int n) {
        double[][] cofenetica = new double[n][n];
        acumularDistancias(raiz, indicePorId, cofenetica);
        return cofenetica;
    }

    private void acumularDistancias(NodoDendrograma nodo, Map<Integer, Integer> indicePorId, double[][] cofenetica) {
        if (nodo.isEsHoja()) {
            return;
        }
        List<Integer> miembrosIzq = nodo.getIzquierdo().getMiembros();
        List<Integer> miembrosDer = nodo.getDerecho().getMiembros();

        for (int idA : miembrosIzq) {
            int i = indicePorId.get(idA);
            for (int idB : miembrosDer) {
                int j = indicePorId.get(idB);
                cofenetica[i][j] = nodo.getAltura();
                cofenetica[j][i] = nodo.getAltura();
            }
        }
        acumularDistancias(nodo.getIzquierdo(), indicePorId, cofenetica);
        acumularDistancias(nodo.getDerecho(), indicePorId, cofenetica);
    }

    /** Coeficiente de correlacion de Pearson entre las distancias originales y las cofeneticas (triangulo superior). */
    public double calcularCorrelacion(double[][] distanciasOriginales, double[][] distanciasCofeneticas) {
        int n = distanciasOriginales.length;
        int m = n * (n - 1) / 2;
        double[] x = new double[m];
        double[] y = new double[m];
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                x[k] = distanciasOriginales[i][j];
                y[k] = distanciasCofeneticas[i][j];
                k++;
            }
        }
        return pearson(x, y);
    }

    private double pearson(double[] x, double[] y) {
        int n = x.length;
        double mediaX = promedio(x);
        double mediaY = promedio(y);

        double numerador = 0.0;
        double sumaCuadX = 0.0;
        double sumaCuadY = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - mediaX;
            double dy = y[i] - mediaY;
            numerador += dx * dy;
            sumaCuadX += dx * dx;
            sumaCuadY += dy * dy;
        }
        double denominador = Math.sqrt(sumaCuadX) * Math.sqrt(sumaCuadY);
        return denominador == 0.0 ? 0.0 : numerador / denominador;
    }

    private double promedio(double[] valores) {
        double suma = 0.0;
        for (double v : valores) suma += v;
        return suma / valores.length;
    }
}
