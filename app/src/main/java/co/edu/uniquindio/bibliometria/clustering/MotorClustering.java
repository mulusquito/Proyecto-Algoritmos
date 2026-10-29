package co.edu.uniquindio.bibliometria.clustering;

import co.edu.uniquindio.bibliometria.clustering.evaluacion.CorrelacionCofenetica;
import co.edu.uniquindio.bibliometria.clustering.evaluacion.Silueta;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el Requerimiento 2 (Agrupamiento Jerarquico): construye la matriz
 * de distancias del corpus, ejecuta los 3 algoritmos de clustering
 * jerarquico (Single, Complete y Average Linkage) y evalua cada uno con dos
 * metricas internas: la correlacion cofenetica (que tan fielmente el
 * dendrograma conserva las distancias) y el coeficiente de silueta de sus
 * cortes en k = 2..6 grupos (que tan separados quedan los grupos).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class MotorClustering {

    /** Fuente de los 20 articulos. */
    private final RepositorioArticulos repositorioArticulos;
    /** Se usa para reutilizar el vectorizador TF-IDF ya entrenado con el corpus. */
    private final MotorSimilitud motorSimilitud;

    /** Distancias 1 - coseno(TF-IDF) entre cada par de articulos. */
    private double[][] matrizDistancias;
    /** Articulos en el mismo orden que las filas/columnas de la matriz. */
    private List<Articulo> articulos;
    /** Traduce el id de un articulo a su posicion (0..n-1) en la matriz. */
    private Map<Integer, Integer> indicePorId;
    /** Un resultado por cada criterio de enlace (Single, Complete, Average). */
    private List<ResultadoClustering> resultados;

    /**
     * Crea el motor y ejecuta de inmediato los 3 algoritmos de clustering.
     * Como el corpus no cambia, los resultados se calculan una sola vez al
     * arrancar y luego solo se consultan.
     */
    public MotorClustering(RepositorioArticulos repositorioArticulos, MotorSimilitud motorSimilitud) {
        this.repositorioArticulos = repositorioArticulos;
        this.motorSimilitud = motorSimilitud;
        inicializar();
    }

    /**
     * Construye la matriz de distancias, las etiquetas de las hojas y, para
     * cada criterio de enlace, ejecuta el clustering y calcula su correlacion
     * cofenetica.
     */
    private void inicializar() {
        this.articulos = repositorioArticulos.listarTodos();
        this.matrizDistancias = DistanciaCorpus.construirMatrizDistancias(articulos, motorSimilitud.getTfIdfVectorizador());

        // Etiqueta de cada hoja: "[id] titulo resumido".
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

        // Se ejecuta el mismo algoritmo con cada criterio de enlace y se evalua cada dendrograma.
        List<EstrategiaEnlace> estrategias = List.of(new EnlaceSimple(), new EnlaceCompleto(), new EnlacePromedio());
        this.resultados = new ArrayList<>();
        for (EstrategiaEnlace estrategia : estrategias) {
            ResultadoClustering resultado = algoritmo.ejecutar(matrizDistancias, etiquetas, ids, estrategia);
            double[][] cofenetica = evaluador.calcularMatrizCofenetica(resultado.getRaiz(), indicePorId, articulos.size());
            double correlacion = evaluador.calcularCorrelacion(matrizDistancias, cofenetica);
            resultado.setCorrelacionCofenetica(correlacion);
            resultado.setCortes(calcularCortes(resultado));
            resultados.add(resultado);
        }
    }

    /** Menor y mayor numero de grupos en que se corta cada dendrograma para evaluarlo con la silueta. */
    public static final int K_MINIMO = 2;
    public static final int K_MAXIMO = 6;

    /**
     * Corta el dendrograma en k = 2..6 grupos y, para cada corte, registra
     * los grupos (ids de articulos), sus tamanos y el coeficiente de silueta.
     */
    private List<Map<String, Object>> calcularCortes(ResultadoClustering resultado) {
        List<Map<String, Object>> cortes = new ArrayList<>();
        for (int k = K_MINIMO; k <= K_MAXIMO; k++) {
            List<List<Integer>> grupos = CorteDendrograma.cortar(resultado.getRaiz(), k);
            // Tamanos de mayor a menor (insercion: son a lo sumo 6 valores).
            int[] tamanos = new int[grupos.size()];
            for (int g = 0; g < grupos.size(); g++) {
                int t = grupos.get(g).size();
                int j = g - 1;
                while (j >= 0 && tamanos[j] < t) {
                    tamanos[j + 1] = tamanos[j];
                    j--;
                }
                tamanos[j + 1] = t;
            }
            Map<String, Object> corte = new LinkedHashMap<>();
            corte.put("k", k);
            corte.put("silueta", Silueta.calcular(matrizDistancias, grupos, indicePorId));
            corte.put("tamanos", tamanos);
            corte.put("grupos", grupos);
            cortes.add(corte);
        }
        return cortes;
    }

    /** Recorta los titulos largos a 40 caracteres para que quepan debajo del dendrograma. */
    private String resumirTitulo(String titulo) {
        return titulo.length() > 40 ? titulo.substring(0, 37) + "..." : titulo;
    }

    /** @return los resultados de los 3 criterios de enlace */
    public List<ResultadoClustering> getResultados() {
        return resultados;
    }

    /** @return los articulos en el orden de las filas de la matriz de distancias */
    public List<Articulo> getArticulos() {
        return articulos;
    }

    /** @return traduccion del id de un articulo a su fila en la matriz de distancias */
    public Map<Integer, Integer> getIndicePorId() {
        return indicePorId;
    }

    /** @return la matriz de distancias usada como entrada del clustering */
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
