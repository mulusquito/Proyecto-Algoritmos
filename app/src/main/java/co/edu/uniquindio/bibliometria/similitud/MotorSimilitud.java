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

    private final RepositorioArticulos repositorioArticulos;
    private final RepositorioEmbeddings repositorioEmbeddings;

    private final LevenshteinSimilitud levenshtein = new LevenshteinSimilitud();
    private final NeedlemanWunschSimilitud needlemanWunsch = new NeedlemanWunschSimilitud();
    private final JaccardSimilitud jaccard = new JaccardSimilitud();

    private TfIdfVectorizador tfIdfVectorizador;
    private CosenoTfIdfSimilitud cosenoTfIdf;
    private VectorizadorEmbeddings vectorizadorEmbeddings;
    private CosenoEmbeddingSimilitud cosenoEmbedding;
    private EuclidianaEmbeddingSimilitud euclidianaEmbedding;

    public MotorSimilitud(RepositorioArticulos repositorioArticulos, RepositorioEmbeddings repositorioEmbeddings) {
        this.repositorioArticulos = repositorioArticulos;
        this.repositorioEmbeddings = repositorioEmbeddings;
        inicializar();
    }

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

    /** Ejecuta los 6 algoritmos de similitud sobre el par de articulos dado. */
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

    public TfIdfVectorizador getTfIdfVectorizador() {
        return tfIdfVectorizador;
    }

    public LevenshteinSimilitud getLevenshtein() {
        return levenshtein;
    }

    public NeedlemanWunschSimilitud getNeedlemanWunsch() {
        return needlemanWunsch;
    }
}
