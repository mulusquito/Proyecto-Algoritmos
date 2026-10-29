package co.edu.uniquindio.bibliometria.similitud;

import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.clasico.CosenoTfIdfSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.JaccardSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.LevenshteinSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.NeedlemanWunschSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.TfIdfVectorizador;
import co.edu.uniquindio.bibliometria.similitud.ia.RepositorioEmbeddings;
import co.edu.uniquindio.bibliometria.similitud.ia.SimilitudEmbeddings;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta los 6 algoritmos de similitud (4 clasicos + 2 con IA) exigidos
 * por el Requerimiento 1 del proyecto. Se encarga de:
 * <ul>
 *     <li>Construir el vectorizador TF-IDF sobre todo el corpus (20 abstracts) una sola vez.</li>
 *     <li>Usar los dos modelos de embeddings preentrenados (GloVe y FastText), uno por algoritmo con IA.</li>
 *     <li>Ejecutar los 6 algoritmos sobre los articulos seleccionados por el usuario (dos o mas).</li>
 * </ul>
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class MotorSimilitud {

    /** Fuente de los 20 articulos (necesaria para entrenar el TF-IDF con todo el corpus). */
    private final RepositorioArticulos repositorioArticulos;
    /** Vectores de palabra preentrenados de cada enfoque de IA. */
    private final RepositorioEmbeddings repositorioGlove;
    private final RepositorioEmbeddings repositorioFastText;

    // Algoritmos que no necesitan entrenamiento previo: se crean directamente.
    private final LevenshteinSimilitud levenshtein = new LevenshteinSimilitud();
    private final NeedlemanWunschSimilitud needlemanWunsch = new NeedlemanWunschSimilitud();
    private final JaccardSimilitud jaccard = new JaccardSimilitud();

    // Algoritmos que dependen del corpus o del modelo de embeddings: se crean en inicializar().
    private TfIdfVectorizador tfIdfVectorizador;
    private CosenoTfIdfSimilitud cosenoTfIdf;
    private SimilitudEmbeddings similitudGlove;
    private SimilitudEmbeddings similitudFastText;

    /** Crea el motor y prepara de inmediato el TF-IDF y los dos algoritmos de embeddings. */
    public MotorSimilitud(RepositorioArticulos repositorioArticulos, RepositorioEmbeddings repositorioGlove,
                          RepositorioEmbeddings repositorioFastText) {
        this.repositorioArticulos = repositorioArticulos;
        this.repositorioGlove = repositorioGlove;
        this.repositorioFastText = repositorioFastText;
        inicializar();
    }

    /**
     * Entrena el vectorizador TF-IDF con los tokens de los 20 abstracts (para
     * que el IDF refleje todo el corpus) y construye los algoritmos que
     * dependen de el y del modelo de embeddings.
     */
    private void inicializar() {
        List<List<String>> corpusTokenizado = new ArrayList<>();
        for (Articulo articulo : repositorioArticulos.listarTodos()) {
            corpusTokenizado.add(articulo.getTokens());
        }
        this.tfIdfVectorizador = new TfIdfVectorizador(corpusTokenizado);
        this.cosenoTfIdf = new CosenoTfIdfSimilitud(tfIdfVectorizador);
        this.similitudGlove = SimilitudEmbeddings.conGlove(repositorioGlove);
        this.similitudFastText = SimilitudEmbeddings.conFastText(repositorioFastText);
    }

    /**
     * Ejecuta los 6 algoritmos de similitud sobre el par de articulos dado.
     * Cada algoritmo recibe la representacion que necesita: texto original
     * (Levenshtein, Needleman-Wunsch y Jaccard hacen su propio
     * preprocesamiento), tokens con stemming (TF-IDF) o tokens sin stemming
     * (embeddings).
     *
     * @return los 6 resultados, en el orden en que se muestran en la interfaz
     */
    public List<ResultadoSimilitud> compararArticulos(Articulo a, Articulo b) {
        List<String> tokensA = a.getTokens();
        List<String> tokensB = b.getTokens();
        List<String> tokensEmbeddingsA = a.getTokensEmbeddings();
        List<String> tokensEmbeddingsB = b.getTokensEmbeddings();

        List<ResultadoSimilitud> resultados = new ArrayList<>();
        resultados.add(levenshtein.calcular(a.getAbstractOriginal(), b.getAbstractOriginal()));
        resultados.add(needlemanWunsch.calcular(a.getAbstractOriginal(), b.getAbstractOriginal()));
        resultados.add(cosenoTfIdf.calcular(tokensA, tokensB));
        resultados.add(jaccard.calcular(a.getAbstractOriginal(), b.getAbstractOriginal()));
        resultados.add(similitudGlove.calcular(tokensEmbeddingsA, tokensEmbeddingsB));
        resultados.add(similitudFastText.calcular(tokensEmbeddingsA, tokensEmbeddingsB));
        return resultados;
    }

    /**
     * Compara dos o mas articulos: ejecuta los 6 algoritmos sobre cada par
     * distinto (k articulos dan k(k-1)/2 pares) y arma, para cada algoritmo,
     * la matriz k x k de similitudes (diagonal = 1, simetrica).
     *
     * @param articulos los articulos seleccionados (2 o mas, sin repetir)
     * @return mapa con la lista de pares (cada uno con sus 6 resultados) y una matriz por algoritmo
     */
    public Map<String, Object> compararVarios(List<Articulo> articulos) {
        int k = articulos.size();
        List<Map<String, Object>> pares = new ArrayList<>();
        // matrices.get(a)[i][j] = similitud del algoritmo a entre los articulos i y j.
        List<double[][]> matrices = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        List<String> categorias = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            for (int j = i + 1; j < k; j++) {
                List<ResultadoSimilitud> resultados = compararArticulos(articulos.get(i), articulos.get(j));
                if (matrices.isEmpty()) {
                    // La primera comparacion fija el orden de los algoritmos y crea las matrices.
                    for (ResultadoSimilitud r : resultados) {
                        double[][] matriz = new double[k][k];
                        for (int d = 0; d < k; d++) matriz[d][d] = 1.0;
                        matrices.add(matriz);
                        nombres.add(r.getAlgoritmo());
                        categorias.add(r.getCategoria());
                    }
                }
                List<Map<String, Object>> resultadosMapa = new ArrayList<>();
                for (int a = 0; a < resultados.size(); a++) {
                    double valor = resultados.get(a).getSimilitud();
                    matrices.get(a)[i][j] = valor;
                    matrices.get(a)[j][i] = valor;
                    resultadosMapa.add(resultados.get(a).aMapa());
                }
                Map<String, Object> par = new LinkedHashMap<>();
                par.put("idA", articulos.get(i).getId());
                par.put("idB", articulos.get(j).getId());
                par.put("resultados", resultadosMapa);
                pares.add(par);
            }
        }

        List<Map<String, Object>> listaMatrices = new ArrayList<>();
        for (int a = 0; a < matrices.size(); a++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("algoritmo", nombres.get(a));
            m.put("categoria", categorias.get(a));
            m.put("valores", matrices.get(a));
            listaMatrices.add(m);
        }
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("pares", pares);
        respuesta.put("matrices", listaMatrices);
        return respuesta;
    }

    /** @return el algoritmo de IA basado en GloVe */
    public SimilitudEmbeddings getSimilitudGlove() {
        return similitudGlove;
    }

    /** @return el algoritmo de IA basado en FastText */
    public SimilitudEmbeddings getSimilitudFastText() {
        return similitudFastText;
    }

    /** @return el vectorizador TF-IDF entrenado (lo reutiliza el clustering del Requerimiento 2) */
    public TfIdfVectorizador getTfIdfVectorizador() {
        return tfIdfVectorizador;
    }

    /** @return la instancia de Levenshtein (usada para generar ejemplos de documentacion) */
    public LevenshteinSimilitud getLevenshtein() {
        return levenshtein;
    }

    /** @return la instancia de Needleman-Wunsch (usada para generar ejemplos de documentacion) */
    public NeedlemanWunschSimilitud getNeedlemanWunsch() {
        return needlemanWunsch;
    }
}
