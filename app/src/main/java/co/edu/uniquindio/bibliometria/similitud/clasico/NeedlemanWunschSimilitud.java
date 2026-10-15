package co.edu.uniquindio.bibliometria.similitud.clasico;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.List;

/**
 * Segundo algoritmo clasico basado en distancia/alineamiento de edicion:
 * <b>Needleman-Wunsch</b> (Needleman &amp; Wunsch, 1970), implementado desde
 * cero mediante programacion dinamica.
 * <p>
 * A diferencia de {@link LevenshteinSimilitud} (que se aplica caracter a
 * caracter sobre el texto completo), aqui el algoritmo de alineamiento
 * global se aplica sobre la secuencia de <b>tokens/palabras</b> ya
 * preprocesadas de cada abstract. Esto permite capturar coincidencias de
 * vocabulario permitiendo inserciones y borrados de palabras completas,
 * a diferencia de la comparacion caracter a caracter.
 * <p>
 * Se define un esquema de puntuacion clasico:
 * <ul>
 *     <li>Coincidencia (match): +2</li>
 *     <li>No coincidencia (mismatch): -1</li>
 *     <li>Hueco (gap, insercion/borrado): -1</li>
 * </ul>
 * con la recurrencia:
 * <pre>
 * F[0][j] = j * gap
 * F[i][0] = i * gap
 * F[i][j] = max( F[i-1][j-1] + s(Ai,Bj), F[i-1][j] + gap, F[i][j-1] + gap )
 * </pre>
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class NeedlemanWunschSimilitud {

    public static final String NOMBRE = "Needleman-Wunsch (alineamiento global de tokens)";

    private static final int MATCH = 2;
    private static final int MISMATCH = -1;
    private static final int GAP = -1;

    /**
     * Calcula la matriz F de programacion dinamica del alineamiento global.
     *
     * @param a tokens preprocesados del primer abstract (longitud n)
     * @param b tokens preprocesados del segundo abstract (longitud m)
     * @return matriz F de tamano (n+1) x (m+1) con los puntajes de alineamiento parciales
     */
    public int[][] construirMatriz(List<String> a, List<String> b) {
        int n = a.size();
        int m = b.size();
        int[][] f = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            f[i][0] = i * GAP;
        }
        for (int j = 0; j <= m; j++) {
            f[0][j] = j * GAP;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int puntajeSustitucion = a.get(i - 1).equals(b.get(j - 1)) ? MATCH : MISMATCH;
                int diagonal = f[i - 1][j - 1] + puntajeSustitucion;
                int arriba = f[i - 1][j] + GAP;
                int izquierda = f[i][j - 1] + GAP;
                f[i][j] = Math.max(diagonal, Math.max(arriba, izquierda));
            }
        }
        return f;
    }

    /**
     * Calcula la similitud de Needleman-Wunsch entre dos abstracts, a nivel de tokens.
     *
     * @param abstractA texto crudo del primer abstract
     * @param abstractB texto crudo del segundo abstract
     * @return resultado con la similitud normalizada en [0,1] y el detalle del alineamiento
     */
    public ResultadoSimilitud calcular(String abstractA, String abstractB) {
        List<String> a = Preprocesador.procesarParaVectorial(abstractA);
        List<String> b = Preprocesador.procesarParaVectorial(abstractB);

        int[][] matriz = construirMatriz(a, b);
        int puntajeFinal = matriz[a.size()][b.size()];

        int maxLen = Math.max(a.size(), b.size());
        double maximoPosible = maxLen * (double) MATCH;
        double minimoPosible = maxLen * (double) GAP;
        double similitud;
        if (maximoPosible == minimoPosible) {
            similitud = 1.0;
        } else {
            similitud = (puntajeFinal - minimoPosible) / (maximoPosible - minimoPosible);
        }
        similitud = Math.max(0.0, Math.min(1.0, similitud));

        return new ResultadoSimilitud(NOMBRE, "clasico", similitud)
                .conDetalle("puntajeAlineamiento", puntajeFinal)
                .conDetalle("tokensA", a.size())
                .conDetalle("tokensB", b.size())
                .conDetalle("esquema", "match=+2, mismatch=-1, gap=-1")
                .conDetalle("formula", "similitud = (F[n][m] - minPosible) / (maxPosible - minPosible)");
    }
}
