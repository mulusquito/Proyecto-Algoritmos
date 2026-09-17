package co.edu.uniquindio.bibliometria.similitud.clasico;

import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

/**
 * Algoritmo clasico de similitud basado en distancia de edicion:
 * <b>Distancia de Levenshtein</b>, implementado desde cero mediante
 * programacion dinamica (sin usar ninguna libreria que lo calcule por
 * nosotros).
 * <p>
 * La distancia de Levenshtein entre dos cadenas A (longitud n) y B
 * (longitud m) es el numero minimo de operaciones de insercion, borrado o
 * sustitucion de un caracter necesarias para transformar A en B.
 * <p>
 * Se construye una matriz D de tamano (n+1) x (m+1) donde D[i][j] es la
 * distancia entre los primeros i caracteres de A y los primeros j
 * caracteres de B, con la recurrencia:
 * <pre>
 * D[0][j] = j                      (insertar j caracteres)
 * D[i][0] = i                      (borrar i caracteres)
 * D[i][j] = D[i-1][j-1]                       si A[i] == B[j]
 * D[i][j] = 1 + min(D[i-1][j], D[i][j-1], D[i-1][j-1])   en otro caso
 * </pre>
 * El resultado final D[n][m] se normaliza dividiendolo entre la longitud
 * maxima de las dos cadenas, y se convierte en una similitud
 * {@code 1 - distancia_normalizada} para que quede en el rango [0,1], igual
 * que el resto de algoritmos.
 */
public class LevenshteinSimilitud {

    public static final String NOMBRE = "Levenshtein (distancia de edicion)";

    /**
     * Calcula la matriz de programacion dinamica completa. Se expone por
     * separado para poder mostrar el "llenado de la matriz" exigido en la
     * documentacion del caso de estudio.
     */
    public int[][] construirMatriz(String a, String b) {
        int n = a.length();
        int m = b.length();
        int[][] d = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char cb = b.charAt(j - 1);
                if (ca == cb) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    int sustitucion = d[i - 1][j - 1] + 1;
                    int insercion = d[i][j - 1] + 1;
                    int borrado = d[i - 1][j] + 1;
                    d[i][j] = Math.min(sustitucion, Math.min(insercion, borrado));
                }
            }
        }
        return d;
    }

    public ResultadoSimilitud calcular(String abstractA, String abstractB) {
        String a = Preprocesador.normalizarParaEdicion(abstractA);
        String b = Preprocesador.normalizarParaEdicion(abstractB);

        int[][] matriz = construirMatriz(a, b);
        int distancia = matriz[a.length()][b.length()];
        int maxLen = Math.max(a.length(), b.length());
        double distanciaNormalizada = maxLen == 0 ? 0.0 : (double) distancia / maxLen;
        double similitud = 1.0 - distanciaNormalizada;

        return new ResultadoSimilitud(NOMBRE, "clasico", similitud)
                .conDetalle("distanciaAbsoluta", distancia)
                .conDetalle("longitudA", a.length())
                .conDetalle("longitudB", b.length())
                .conDetalle("formula", "similitud = 1 - (distancia / max(|A|,|B|))");
    }
}
