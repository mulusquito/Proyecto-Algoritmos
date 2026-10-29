package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ResultadoSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.LevenshteinSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.NeedlemanWunschSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.TfIdfVectorizador;
import co.edu.uniquindio.bibliometria.similitud.ia.RepositorioEmbeddings;
import co.edu.uniquindio.bibliometria.similitud.ia.VectorizadorEmbeddings;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad de una sola ejecucion que genera, en Markdown, la demostracion
 * paso a paso del Requerimiento 1 sobre los dos abstracts del caso de
 * estudio (articulos 1 y 2) con el codigo real de cada algoritmo, y las
 * estadisticas de los 190 pares del corpus para el analisis critico. No
 * forma parte de la aplicacion.
 * <p>
 * Ejecutar con el classpath de las pruebas (ver README):
 * {@code java -cp <clases> co.edu.uniquindio.bibliometria.GenerarCasoSimilitud}
 */
public class GenerarCasoSimilitud {

    private static final int ID_A = 1;
    private static final int ID_B = 2;
    /** Tamano de las esquinas de matriz que se imprimen. */
    private static final int ESQUINA = 10;

    public static void main(String[] args) {
        RepositorioArticulos repositorio = new RepositorioArticulos();
        RepositorioEmbeddings glove = RepositorioEmbeddings.glove();
        RepositorioEmbeddings fastText = RepositorioEmbeddings.fastText();
        MotorSimilitud motor = new MotorSimilitud(repositorio, glove, fastText);
        Articulo a = repositorio.buscarPorId(ID_A).get();
        Articulo b = repositorio.buscarPorId(ID_B).get();

        levenshtein(a, b);
        needlemanWunsch(a, b);
        tfIdf(a, b, motor.getTfIdfVectorizador());
        jaccard(a, b);
        embeddings(a, b, glove);
        embeddings(a, b, fastText);
        coberturaCorpus(repositorio, glove, fastText);
        resultados(motor, a, b);
        estadisticasCorpus(repositorio, motor);
    }

    // ------------------------------------------------------------------
    private static void levenshtein(Articulo a, Articulo b) {
        String x = Preprocesador.normalizarParaEdicion(a.getAbstractOriginal());
        String y = Preprocesador.normalizarParaEdicion(b.getAbstractOriginal());
        int[][] d = new LevenshteinSimilitud().construirMatriz(x, y);
        int n = x.length(), m = y.length();
        System.out.println("## Levenshtein\n");
        System.out.println("A normalizado (" + n + " caracteres): `" + x.substring(0, 60) + "...`");
        System.out.println("B normalizado (" + m + " caracteres): `" + y.substring(0, 60) + "...`\n");
        System.out.println("### Esquina superior izquierda D[0.." + ESQUINA + "][0.." + ESQUINA + "]\n");
        imprimirEsquina(d, 0, 0, x, y);
        System.out.println("\n### Esquina inferior derecha (termina en D[" + n + "][" + m + "])\n");
        imprimirEsquina(d, n - ESQUINA, m - ESQUINA, x, y);
        // Explicacion de una celda de la esquina superior.
        System.out.println("\nCelda D[1][1]: A[1]='" + x.charAt(0) + "', B[1]='" + y.charAt(0) + "' -> "
                + (x.charAt(0) == y.charAt(0) ? "iguales, D[0][0] = " + d[0][0]
                : "distintos, 1 + min(" + d[0][0] + "," + d[0][1] + "," + d[1][0] + ") = " + d[1][1]));
        System.out.println("Distancia = D[" + n + "][" + m + "] = " + d[n][m] + "; similitud = 1 - " + d[n][m]
                + "/max(" + n + "," + m + ") = " + String.format("%.4f", 1 - d[n][m] / (double) Math.max(n, m)));
    }

    /** Imprime ESQUINA x ESQUINA celdas de la matriz a partir de (fila0, col0); el espacio se muestra como '_'. */
    private static void imprimirEsquina(int[][] d, int fila0, int col0, String x, String y) {
        StringBuilder sb = new StringBuilder("| i \\ j |");
        for (int j = col0; j <= col0 + ESQUINA; j++) sb.append(" ").append(j).append(etiqueta(y, j)).append(" |");
        System.out.println(sb);
        sb = new StringBuilder("|---|");
        for (int j = col0; j <= col0 + ESQUINA; j++) sb.append("---|");
        System.out.println(sb);
        for (int i = fila0; i <= fila0 + ESQUINA; i++) {
            sb = new StringBuilder("| **" + i + etiqueta(x, i) + "** |");
            for (int j = col0; j <= col0 + ESQUINA; j++) sb.append(" ").append(d[i][j]).append(" |");
            System.out.println(sb);
        }
    }

    /** Caracter de la posicion k (base 1) de la cadena, para encabezar filas y columnas. */
    private static String etiqueta(String s, int k) {
        if (k == 0) return " ∅";
        char c = s.charAt(k - 1);
        return " " + (c == ' ' ? "_" : String.valueOf(c));
    }

    // ------------------------------------------------------------------
    private static void needlemanWunsch(Articulo a, Articulo b) {
        List<String> x = a.getTokens();
        List<String> y = b.getTokens();
        int[][] f = new NeedlemanWunschSimilitud().construirMatriz(x, y);
        int n = x.size(), m = y.size();
        System.out.println("\n## Needleman-Wunsch\n");
        System.out.println("Tokens A (" + n + "): " + x.subList(0, 12) + " ...");
        System.out.println("Tokens B (" + m + "): " + y.subList(0, 12) + " ...\n");
        System.out.println("### Esquina superior izquierda F[0..8][0..8]\n");
        imprimirEsquinaTokens(f, 0, 0, x, y, 8);
        System.out.println("\n### Esquina inferior derecha (termina en F[" + n + "][" + m + "])\n");
        imprimirEsquinaTokens(f, n - 6, m - 6, x, y, 6);
        int max = Math.max(n, m);
        System.out.println("\nPuntaje F[" + n + "][" + m + "] = " + f[n][m] + "; maximo posible = 2*" + max + " = " + 2 * max
                + "; minimo = -" + max + "; similitud = (" + f[n][m] + " - (-" + max + ")) / (" + 2 * max + " - (-" + max
                + ")) = " + String.format("%.4f", (f[n][m] + max) / (3.0 * max)));
        // Cuantas coincidencias exactas tiene el alineamiento optimo (reconstruccion hacia atras).
        int i = n, j = m, coincidencias = 0, gaps = 0, sustituciones = 0;
        while (i > 0 && j > 0) {
            int s = x.get(i - 1).equals(y.get(j - 1)) ? 2 : -1;
            if (f[i][j] == f[i - 1][j - 1] + s) {
                if (s == 2) coincidencias++; else sustituciones++;
                i--; j--;
            } else if (f[i][j] == f[i - 1][j] - 1) { gaps++; i--; } else { gaps++; j--; }
        }
        gaps += i + j;
        System.out.println("Alineamiento optimo: " + coincidencias + " coincidencias (+2), " + sustituciones
                + " sustituciones (-1), " + gaps + " gaps (-1) -> " + (2 * coincidencias - sustituciones - gaps));
    }

    private static void imprimirEsquinaTokens(int[][] f, int fila0, int col0, List<String> x, List<String> y, int t) {
        StringBuilder sb = new StringBuilder("| |");
        for (int j = col0; j <= col0 + t; j++) sb.append(" ").append(j == 0 ? "∅" : j + " " + y.get(j - 1)).append(" |");
        System.out.println(sb);
        sb = new StringBuilder("|---|");
        for (int j = col0; j <= col0 + t; j++) sb.append("---|");
        System.out.println(sb);
        for (int i = fila0; i <= fila0 + t; i++) {
            sb = new StringBuilder("| **" + (i == 0 ? "∅" : i + " " + x.get(i - 1)) + "** |");
            for (int j = col0; j <= col0 + t; j++) sb.append(" ").append(f[i][j]).append(" |");
            System.out.println(sb);
        }
    }

    // ------------------------------------------------------------------
    private static void tfIdf(Articulo a, Articulo b, TfIdfVectorizador v) {
        Map<String, Double> va = v.vectorizar(a.getTokens());
        Map<String, Double> vb = v.vectorizar(b.getTokens());
        Map<String, Integer> ca = contar(a.getTokens());
        Map<String, Integer> cb = contar(b.getTokens());
        Map<String, Double> idf = v.getIdf();
        System.out.println("\n## Coseno TF-IDF\n");
        System.out.println("N = " + v.getNumeroDocumentos() + "; |A| = " + a.getTokens().size() + " tokens, " + va.size()
                + " terminos distintos; |B| = " + b.getTokens().size() + " tokens, " + vb.size() + " terminos distintos\n");
        System.out.println("| Termino | cuenta A | tf A | cuenta B | tf B | df | idf | w A | w B | w A * w B |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|");
        double producto = 0;
        for (Map.Entry<String, Double> e : va.entrySet()) {
            if (!vb.containsKey(e.getKey())) continue;
            String t = e.getKey();
            double idfT = idf.get(t);
            int df = (int) Math.round((v.getNumeroDocumentos() + 1) / Math.exp(idfT - 1) - 1);
            double p = e.getValue() * vb.get(t);
            producto += p;
            System.out.printf("| %s | %d | %d/%d = %.4f | %d | %d/%d = %.4f | %d | ln(21/%d)+1 = %.4f | %.4f | %.4f | %.6f |%n",
                    t, ca.get(t), ca.get(t), a.getTokens().size(), ca.get(t) / (double) a.getTokens().size(),
                    cb.get(t), cb.get(t), b.getTokens().size(), cb.get(t) / (double) b.getTokens().size(),
                    df, df + 1, idfT, e.getValue(), vb.get(t), p);
        }
        double na = TfIdfVectorizador.norma(va), nb = TfIdfVectorizador.norma(vb);
        double cos = producto / (na * nb);
        System.out.printf("%nProducto punto = %.6f; ||A|| = %.6f; ||B|| = %.6f; cos = %.6f / (%.6f * %.6f) = %.4f; angulo = arccos(%.4f) = %.2f grados%n",
                producto, na, nb, producto, na, nb, cos, cos, Math.toDegrees(Math.acos(cos)));
        // Terminos que mas aportan a la norma de cada vector (no compartidos incluidos).
        System.out.println("Mayor peso A: " + top(va, 5) + "; mayor peso B: " + top(vb, 5));
    }

    private static Map<String, Integer> contar(List<String> tokens) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (String t : tokens) conteo.merge(t, 1, Integer::sum);
        return conteo;
    }

    private static String top(Map<String, Double> v, int k) {
        List<Map.Entry<String, Double>> e = new ArrayList<>(v.entrySet());
        e.sort((p, q) -> Double.compare(q.getValue(), p.getValue()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < k; i++) sb.append(e.get(i).getKey()).append(String.format("=%.4f ", e.get(i).getValue()));
        return sb.toString();
    }

    // ------------------------------------------------------------------
    private static void jaccard(Articulo a, Articulo b) {
        var r = new co.edu.uniquindio.bibliometria.similitud.clasico.JaccardSimilitud()
                .calcular(a.getAbstractOriginal(), b.getAbstractOriginal());
        System.out.println("\n## Jaccard\n");
        System.out.println(r.getDetalle());
    }

    // ------------------------------------------------------------------
    private static void embeddings(Articulo a, Articulo b, RepositorioEmbeddings repo) {
        VectorizadorEmbeddings vec = new VectorizadorEmbeddings(repo);
        VectorizadorEmbeddings.VectorDocumento va = vec.vectorizar(a.getTokensEmbeddings());
        VectorizadorEmbeddings.VectorDocumento vb = vec.vectorizar(b.getTokensEmbeddings());
        System.out.println("\n## Embeddings: " + repo.getNombreModelo() + " (" + repo.getDimensiones() + " dimensiones)\n");
        System.out.println("Tokens A (" + a.getTokensEmbeddings().size() + "): " + a.getTokensEmbeddings().subList(0, 8) + " ...");
        System.out.println("Tokens B (" + b.getTokensEmbeddings().size() + "): " + b.getTokensEmbeddings().subList(0, 8) + " ...");
        System.out.println("Encontradas: A " + va.palabrasEncontradas + "/" + va.palabrasTotales + ", B "
                + vb.palabrasEncontradas + "/" + vb.palabrasTotales);
        List<String> faltan = new ArrayList<>();
        for (String t : a.getTokensEmbeddings()) if (!repo.contiene(t) && !faltan.contains(t)) faltan.add(t);
        System.out.println("Palabras de A sin vector: " + faltan);
        faltan = new ArrayList<>();
        for (String t : b.getTokensEmbeddings()) if (!repo.contiene(t) && !faltan.contains(t)) faltan.add(t);
        System.out.println("Palabras de B sin vector: " + faltan + "\n");
        System.out.println("| Palabra | d1 | d2 | d3 | d4 | d5 |");
        System.out.println("|---|---|---|---|---|---|");
        for (String p : new String[]{"artificial", "intelligence", "literacy", "education", "students"}) {
            double[] v = repo.obtenerVector(p);
            if (v != null) System.out.printf("| %s | %.4f | %.4f | %.4f | %.4f | %.4f |%n", p, v[0], v[1], v[2], v[3], v[4]);
        }
        System.out.printf("| **promedio A** | %.4f | %.4f | %.4f | %.4f | %.4f |%n", va.vector[0], va.vector[1], va.vector[2], va.vector[3], va.vector[4]);
        System.out.printf("| **promedio B** | %.4f | %.4f | %.4f | %.4f | %.4f |%n", vb.vector[0], vb.vector[1], vb.vector[2], vb.vector[3], vb.vector[4]);

        double punto = 0, na = 0, nb = 0, sumaCuad = 0, punto5 = 0, cuad5 = 0;
        for (int i = 0; i < va.vector.length; i++) {
            punto += va.vector[i] * vb.vector[i];
            na += va.vector[i] * va.vector[i];
            nb += vb.vector[i] * vb.vector[i];
            double dif = va.vector[i] - vb.vector[i];
            sumaCuad += dif * dif;
            if (i < 5) { punto5 += va.vector[i] * vb.vector[i]; cuad5 += dif * dif; }
        }
        System.out.printf("%nPrimeros 5 terminos del producto punto: %.6f; de la suma de cuadrados: %.6f%n", punto5, cuad5);
        System.out.printf("Producto punto (" + va.vector.length + " dim) = %.6f; ||A|| = %.6f; ||B|| = %.6f; cos = %.4f; angulo = %.2f grados%n",
                punto, Math.sqrt(na), Math.sqrt(nb), punto / (Math.sqrt(na) * Math.sqrt(nb)),
                Math.toDegrees(Math.acos(punto / (Math.sqrt(na) * Math.sqrt(nb)))));
        double dist = Math.sqrt(sumaCuad);
        System.out.printf("Suma de cuadrados = %.6f; distancia = %.6f; similitud = 1/(1+%.6f) = %.4f%n", sumaCuad, dist, dist, 1 / (1 + dist));
    }

    /** Palabras distintas del corpus sin vector en cada modelo, y vecinos semanticos de ejemplo. */
    private static void coberturaCorpus(RepositorioArticulos repositorio, RepositorioEmbeddings... modelos) {
        java.util.Set<String> vocabulario = new java.util.LinkedHashSet<>();
        for (Articulo x : repositorio.listarTodos()) vocabulario.addAll(x.getTokensEmbeddings());
        System.out.println("\n## Cobertura del vocabulario del corpus (" + vocabulario.size() + " palabras distintas)\n");
        for (RepositorioEmbeddings m : modelos) {
            List<String> faltan = new ArrayList<>();
            for (String p : vocabulario) if (!m.contiene(p)) faltan.add(p);
            System.out.println(m.getNombreModelo() + ": " + (vocabulario.size() - faltan.size()) + " con vector; sin vector: " + faltan);
        }
        System.out.println("\nCoseno entre palabras (muestra de lo que cada modelo aprendio):");
        String[][] pares = {{"teacher", "student"}, {"teacher", "educator"}, {"assessment", "evaluation"},
                {"learning", "education"}, {"literacy", "skills"}, {"artificial", "intelligence"}};
        for (String[] p : pares) {
            StringBuilder sb = new StringBuilder("  " + p[0] + " / " + p[1] + ":");
            for (RepositorioEmbeddings m : modelos) {
                double[] u = m.obtenerVector(p[0]), v = m.obtenerVector(p[1]);
                if (u == null || v == null) { sb.append(" ").append(m.getNombreModelo(), 0, 8).append(" -"); continue; }
                double d = 0, nu = 0, nv = 0;
                for (int i = 0; i < u.length; i++) { d += u[i] * v[i]; nu += u[i] * u[i]; nv += v[i] * v[i]; }
                sb.append(String.format(" %s=%.4f", m.getNombreModelo().substring(0, 8), d / Math.sqrt(nu * nv)));
            }
            System.out.println(sb);
        }
    }

    // ------------------------------------------------------------------
    private static void resultados(MotorSimilitud motor, Articulo a, Articulo b) {
        System.out.println("\n## Resultados de la aplicacion (1 vs 2)\n");
        for (ResultadoSimilitud r : motor.compararArticulos(a, b)) {
            System.out.printf("%s = %.4f %s%n", r.getAlgoritmo(), r.getSimilitud(), r.getDetalle());
        }
    }

    /** Estadisticas de cada algoritmo sobre los 190 pares y la comparacion de un par afin con uno lejano. */
    private static void estadisticasCorpus(RepositorioArticulos repositorio, MotorSimilitud motor) {
        List<Articulo> todos = repositorio.listarTodos();
        List<String> nombres = new ArrayList<>();
        List<List<Double>> valores = new ArrayList<>();
        List<int[]> pares = new ArrayList<>();
        for (int i = 0; i < todos.size(); i++) {
            for (int j = i + 1; j < todos.size(); j++) {
                List<ResultadoSimilitud> r = motor.compararArticulos(todos.get(i), todos.get(j));
                if (nombres.isEmpty()) {
                    for (ResultadoSimilitud x : r) { nombres.add(x.getAlgoritmo()); valores.add(new ArrayList<>()); }
                    // Distancia euclidiana de cada modelo de IA, expresada como 1/(1+d), como dato complementario.
                    nombres.add("(detalle) Euclidiana GloVe 1/(1+d)"); valores.add(new ArrayList<>());
                    nombres.add("(detalle) Euclidiana FastText 1/(1+d)"); valores.add(new ArrayList<>());
                }
                for (int k = 0; k < r.size(); k++) valores.get(k).add(r.get(k).getSimilitud());
                valores.get(r.size()).add(1 / (1 + (Double) r.get(4).getDetalle().get("distanciaEuclidiana")));
                valores.get(r.size() + 1).add(1 / (1 + (Double) r.get(5).getDetalle().get("distanciaEuclidiana")));
                pares.add(new int[]{todos.get(i).getId(), todos.get(j).getId()});
            }
        }
        System.out.println("\n## Estadisticas de los " + pares.size() + " pares\n");
        System.out.println("| Algoritmo | Minimo | Maximo | Promedio | Desv. est. | Rango (max-min) | Coef. variacion | Pearson con TF-IDF | Par mas similar | Par menos similar |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|");
        for (int k = 0; k < nombres.size(); k++) {
            List<Double> v = valores.get(k);
            double min = 1e9, max = -1e9, suma = 0;
            int iMin = 0, iMax = 0;
            for (int p = 0; p < v.size(); p++) {
                if (v.get(p) < min) { min = v.get(p); iMin = p; }
                if (v.get(p) > max) { max = v.get(p); iMax = p; }
                suma += v.get(p);
            }
            double media = suma / v.size(), var = 0;
            for (double x : v) var += (x - media) * (x - media);
            double desv = Math.sqrt(var / v.size());
            System.out.printf("| %s | %.4f | %.4f | %.4f | %.4f | %.4f | %.3f | %.3f | %d-%d | %d-%d |%n", nombres.get(k), min, max,
                    media, desv, max - min, desv / media, pearson(v, valores.get(2)),
                    pares.get(iMax)[0], pares.get(iMax)[1], pares.get(iMin)[0], pares.get(iMin)[1]);
        }
        System.out.printf("%nPearson GloVe vs FastText (coseno, 190 pares) = %.3f%n", pearson(valores.get(4), valores.get(5)));
        // Posicion del par 1-2 en el ranking de cada algoritmo (1 = mas similar de los 190).
        int indice12 = 0;
        for (int p = 0; p < pares.size(); p++) if (pares.get(p)[0] == 1 && pares.get(p)[1] == 2) indice12 = p;
        System.out.println("\nPosicion del par 1-2 (de 190, 1 = mas similar) y percentil:");
        for (int k = 0; k < nombres.size(); k++) {
            double x = valores.get(k).get(indice12);
            int mayores = 0;
            for (double y : valores.get(k)) if (y > x) mayores++;
            System.out.printf("  %s: puesto %d%n", nombres.get(k), mayores + 1);
        }
    }

    private static double pearson(List<Double> x, List<Double> y) {
        double mx = 0, my = 0;
        for (int i = 0; i < x.size(); i++) { mx += x.get(i); my += y.get(i); }
        mx /= x.size(); my /= y.size();
        double num = 0, sx = 0, sy = 0;
        for (int i = 0; i < x.size(); i++) {
            num += (x.get(i) - mx) * (y.get(i) - my);
            sx += (x.get(i) - mx) * (x.get(i) - mx);
            sy += (y.get(i) - my) * (y.get(i) - my);
        }
        return num / Math.sqrt(sx * sy);
    }
}
