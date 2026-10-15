package co.edu.uniquindio.bibliometria.similitud.clasico;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Algoritmo clasico de similitud basado en teoria de conjuntos:
 * <b>Coeficiente de Jaccard</b>, implementado desde cero.
 * <p>
 * Cada abstract se representa como el <i>conjunto</i> de tokens distintos
 * que contiene (tras preprocesamiento). El coeficiente se define como:
 * <pre>
 * J(A,B) = |A intersecto B| / |A union B|
 * </pre>
 * Un valor de 1 indica que ambos abstracts usan exactamente el mismo
 * vocabulario (sin importar la frecuencia ni el orden de las palabras); un
 * valor de 0 indica que no comparten ninguna palabra.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class JaccardSimilitud {

    /** Nombre del algoritmo que se muestra en la interfaz. */
    public static final String NOMBRE = "Coeficiente de Jaccard";

    /**
     * Calcula el coeficiente de Jaccard entre los conjuntos de tokens de dos abstracts.
     *
     * @param abstractA texto crudo del primer abstract
     * @param abstractB texto crudo del segundo abstract
     * @return resultado con la similitud J(A,B) en [0,1] y el detalle de los conjuntos
     */
    public ResultadoSimilitud calcular(String abstractA, String abstractB) {
        List<String> tokensA = Preprocesador.procesarParaVectorial(abstractA);
        List<String> tokensB = Preprocesador.procesarParaVectorial(abstractB);

        // Al pasar a conjunto se descartan las repeticiones: solo importa si la palabra esta o no.
        Set<String> conjuntoA = new HashSet<>(tokensA);
        Set<String> conjuntoB = new HashSet<>(tokensB);

        // Interseccion: palabras presentes en ambos (TreeSet para listarlas en orden alfabetico).
        Set<String> interseccion = new TreeSet<>(conjuntoA);
        interseccion.retainAll(conjuntoB);

        // Union: palabras presentes en al menos uno de los dos.
        Set<String> union = new HashSet<>(conjuntoA);
        union.addAll(conjuntoB);

        double similitud = union.isEmpty() ? 0.0 : (double) interseccion.size() / union.size();

        return new ResultadoSimilitud(NOMBRE, "clasico", similitud)
                .conDetalle("tamanoConjuntoA", conjuntoA.size())
                .conDetalle("tamanoConjuntoB", conjuntoB.size())
                .conDetalle("tamanoInterseccion", interseccion.size())
                .conDetalle("tamanoUnion", union.size())
                .conDetalle("palabrasComunes", interseccion.size() <= 40
                        ? new ArrayList<>(interseccion) : List.of("(> 40 palabras, omitido)"))
                .conDetalle("formula", "J(A,B) = |A ∩ B| / |A ∪ B|");
    }
}
