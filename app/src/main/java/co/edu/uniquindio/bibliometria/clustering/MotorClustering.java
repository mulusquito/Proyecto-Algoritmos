package co.edu.uniquindio.bibliometria.clustering;

import co.edu.uniquindio.bibliometria.clustering.evaluacion.CorrelacionCofenetica;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el Requerimiento 2 (Agrupamiento Jerarquico): construye la matriz
 * de distancias del corpus, ejecuta los 3 algoritmos de clustering
 * jerarquico (Single, Complete y Average Linkage) y evalua cada uno con la
 * correlacion cofenetica para determinar cual produce el agrupamiento mas
 * coherente.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class MotorClustering {

    private final RepositorioArticulos repositorioArticulos;
    private final MotorSimilitud motorSimilitud;

    private double[][] matrizDistancias;
    private List<Articulo> articulos;
    private Map<Integer, Integer> indicePorId;
    private List<ResultadoClustering> resultados;

    public MotorClustering(RepositorioArticulos repositorioArticulos, MotorSimilitud motorSimilitud) {
        this.repositorioArticulos = repositorioArticulos;
        this.motorSimilitud = motorSimilitud;
        inicializar();
    }

    private void inicializar() {
        this.articulos = repositorioArticulos.listarTodos();
        this.matrizDistancias = DistanciaCorpus.construirMatrizDistancias(articulos, motorSimilitud.getTfIdfVectorizador());

        this.indicePorId = new HashMap<>();
        List<String> etiquetas = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < articulos.size(); i++) {
            Articulo a = articulos.get(i);
            indicePorId.put(a.getId(), i);
            etiquetas.add("[" + a.getId() + "] " + resumirTitulo(a.getTitulo()));
            ids.add(a.getId());
        }

        ClusteringJerarquico algoritmo = new ClusteringJerarquico();
        CorrelacionCofenetica evaluador = new CorrelacionCofenetica();

        List<EstrategiaEnlace> estrategias = List.of(new EnlaceSimple(), new EnlaceCompleto(), new EnlacePromedio());
        this.resultados = new ArrayList<>();
        for (EstrategiaEnlace estrategia : estrategias) {
            ResultadoClustering resultado = algoritmo.ejecutar(matrizDistancias, etiquetas, ids, estrategia);
            double[][] cofenetica = evaluador.calcularMatrizCofenetica(resultado.getRaiz(), indicePorId, articulos.size());
            double correlacion = evaluador.calcularCorrelacion(matrizDistancias, cofenetica);
            resultado.setCorrelacionCofenetica(correlacion);
            resultados.add(resultado);
        }
    }

    private String resumirTitulo(String titulo) {
        return titulo.length() > 40 ? titulo.substring(0, 37) + "..." : titulo;
    }

    public List<ResultadoClustering> getResultados() {
        return resultados;
    }

    public double[][] getMatrizDistancias() {
        return matrizDistancias;
    }

    /** El algoritmo (de los 3) cuyo dendrograma mejor preserva las distancias originales. */
    public ResultadoClustering getMejorResultado() {
        return resultados.stream()
                .max((a, b) -> Double.compare(a.getCorrelacionCofenetica(), b.getCorrelacionCofenetica()))
                .orElseThrow();
    }
}
