package co.edu.uniquindio.bibliometria.clustering;

import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.clasico.TfIdfVectorizador;

import java.util.List;
import java.util.Map;

/**
 * Construye la matriz de distancias N x N entre los 20 abstracts del
 * corpus, necesaria como punto de partida del clustering jerarquico
 * (Requerimiento 2).
 * <p>
 * Se usa como medida base la similitud del coseno sobre TF-IDF (el mismo
 * algoritmo clasico del Requerimiento 1), transformada a distancia con
 * {@code distancia = 1 - similitud}, de forma que 0 significa "identicos"
 * y 2 el maximo desacuerdo posible (vectores opuestos). Esta eleccion se
 * documenta explicitamente porque el enunciado permite research sobre
 * cualquier medida de similitud/distancia razonable para esta etapa.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class DistanciaCorpus {

    public static double[][] construirMatrizDistancias(List<Articulo> articulos, TfIdfVectorizador vectorizador) {
        int n = articulos.size();
        double[][] distancias = new double[n][n];

        Map<String, Double>[] vectores = new Map[n];
        for (int i = 0; i < n; i++) {
            vectores[i] = vectorizador.vectorizar(articulos.get(i).getTokens());
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double similitud = TfIdfVectorizador.similitudCoseno(vectores[i], vectores[j]);
                double distancia = 1.0 - similitud;
                distancias[i][j] = distancia;
                distancias[j][i] = distancia;
            }
        }
        return distancias;
    }
}
