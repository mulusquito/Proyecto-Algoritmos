package co.edu.uniquindio.bibliometria.similitud.clasico;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // 1. Conjuntos: se descartan las repeticiones, solo importa si la palabra esta o no.
        //    La tabla hash se usa unicamente como estructura de pertenencia (agregar / consultar).
        Set<String> conjuntoA = new HashSet<>();
        List<String> distintosA = construirConjunto(tokensA, conjuntoA);
        Set<String> conjuntoB = new HashSet<>();
        List<String> distintosB = construirConjunto(tokensB, conjuntoB);

        // 2. Interseccion: se recorre el conjunto mas pequeno y se cuenta cada palabra
        //    que tambien pertenece al otro.
        List<String> recorrido = distintosA.size() <= distintosB.size() ? distintosA : distintosB;
        Set<String> otro = recorrido == distintosA ? conjuntoB : conjuntoA;
        List<String> interseccion = new ArrayList<>();
        for (String palabra : recorrido) {
            if (otro.contains(palabra)) {
                interseccion.add(palabra);
            }
        }

        // 3. Union por inclusion-exclusion: |A u B| = |A| + |B| - |A n B|
        //    (las palabras comunes se contarian dos veces).
        int tamanoUnion = distintosA.size() + distintosB.size() - interseccion.size();

        double similitud = tamanoUnion == 0 ? 0.0 : (double) interseccion.size() / tamanoUnion;

        return new ResultadoSimilitud(NOMBRE, "clasico", similitud)
                .conDetalle("tamanoConjuntoA", distintosA.size())
                .conDetalle("tamanoConjuntoB", distintosB.size())
                .conDetalle("tamanoInterseccion", interseccion.size())
                .conDetalle("tamanoUnion", tamanoUnion)
                .conDetalle("calculo", interseccion.size() + " / (" + distintosA.size() + " + " + distintosB.size()
                        + " - " + interseccion.size() + ") = " + interseccion.size() + " / " + tamanoUnion)
                .conDetalle("palabrasComunes", interseccion.size() <= 40
                        ? interseccion : List.of("(> 40 palabras, omitido)"))
                .conDetalle("formula", "J(A,B) = |A ∩ B| / |A ∪ B|,  |A ∪ B| = |A| + |B| - |A ∩ B|");
    }

    /**
     * Recorre los tokens una vez y conserva solo la primera aparicion de
     * cada palabra.
     *
     * @param tokens    tokens del abstract (con repeticiones)
     * @param conjunto  tabla de pertenencia que se llena con las palabras distintas
     * @return las palabras distintas, en orden de primera aparicion
     */
    private static List<String> construirConjunto(List<String> tokens, Set<String> conjunto) {
        List<String> distintos = new ArrayList<>();
        for (String token : tokens) {
            if (!conjunto.contains(token)) {
                conjunto.add(token);
                distintos.add(token);
            }
        }
        return distintos;
    }
}
