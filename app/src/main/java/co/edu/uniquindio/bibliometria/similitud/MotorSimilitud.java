package co.edu.uniquindio.bibliometria.similitud;

import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.clasico.CosenoTfIdfSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.JaccardSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.LevenshteinSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.NeedlemanWunschSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.TfIdfVectorizador;
import co.edu.uniquindio.bibliometria.similitud.ia.CosenoEmbeddingSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.EuclidianaEmbeddingSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.RepositorioEmbeddings;
import co.edu.uniquindio.bibliometria.similitud.ia.VectorizadorEmbeddings;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta los 6 algoritmos de similitud (4 clasicos + 2 con IA) exigidos
 * por el Requerimiento 1 del proyecto. Se encarga de:
 * <ul>
 *     <li>Construir el vectorizador TF-IDF sobre todo el corpus (20 abstracts) una sola vez.</li>
 *     <li>Cargar el modelo de embeddings preentrenado.</li>
 *     <li>Ejecutar los 6 algoritmos sobre un par de articulos seleccionado por el usuario.</li>
 * </ul>
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class MotorSimilitud {

    /** Fuente de los 20 articulos (necesaria para entrenar el TF-IDF con todo el corpus). */
    private final RepositorioArticulos repositorioArticulos;
    /** Vectores de palabra preentrenados (GloVe) para los algoritmos con IA. */
    private final RepositorioEmbeddings repositorioEmbeddings;

    // Algoritmos que no necesitan entrenamiento previo: se crean directamente.
    private final LevenshteinSimilitud levenshtein = new LevenshteinSimilitud();
    private final NeedlemanWunschSimilitud needlemanWunsch = new NeedlemanWunschSimilitud();
    private final JaccardSimilitud jaccard = new JaccardSimilitud();

    // Algoritmos que dependen del corpus o del modelo de embeddings: se crean en inicializar().
    private TfIdfVectorizador tfIdfVectorizador;
    private CosenoTfIdfSimilitud cosenoTfIdf;
    private VectorizadorEmbeddings vectorizadorEmbeddings;
    private CosenoEmbeddingSimilitud cosenoEmbedding;
    private EuclidianaEmbeddingSimilitud euclidianaEmbedding;

    /** Crea el motor y prepara de inmediato el TF-IDF y los vectorizadores de embeddings. */
    public MotorSimilitud(RepositorioArticulos repositorioArticulos, RepositorioEmbeddings repositorioEmbeddings) {
        this.repositorioArticulos = repositorioArticulos;
        this.repositorioEmbeddings = repositorioEmbeddings;
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
        this.vectorizadorEmbeddings = new VectorizadorEmbeddings(repositorioEmbeddings);
        this.cosenoEmbedding = new CosenoEmbeddingSimilitud(vectorizadorEmbeddings);
        this.euclidianaEmbedding = new EuclidianaEmbeddingSimilitud(vectorizadorEmbeddings);
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
        resultados.add(cosenoEmbedding.calcular(tokensEmbeddingsA, tokensEmbeddingsB));
        resultados.add(euclidianaEmbedding.calcular(tokensEmbeddingsA, tokensEmbeddingsB));
        return resultados;
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
