package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.clustering.ClusteringJerarquico;
import co.edu.uniquindio.bibliometria.clustering.EnlaceSimple;
import co.edu.uniquindio.bibliometria.clustering.EstrategiaEnlace;
import co.edu.uniquindio.bibliometria.clustering.ResultadoClustering;
import co.edu.uniquindio.bibliometria.clustering.evaluacion.CorrelacionCofenetica;
import co.edu.uniquindio.bibliometria.similitud.clasico.JaccardSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.LevenshteinSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.NeedlemanWunschSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.TfIdfVectorizador;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;
import co.edu.uniquindio.bibliometria.texto.Stemmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bateria de pruebas manuales (sin JUnit, ver README para la justificacion
 * de por que no se usan dependencias externas) que verifica, con casos de
 * referencia conocidos, que cada algoritmo implementado desde cero produce
 * el resultado matematicamente correcto.
 * <p>
 * Ejecutar con:
 * <pre>
 * javac -d /tmp/out $(find src/main/java src/test/java -name "*.java")
 * java -cp /tmp/out co.edu.uniquindio.bibliometria.PruebasAlgoritmos
 * </pre>
 * El programa termina con codigo de salida distinto de 0 si alguna prueba falla.
 */
public class PruebasAlgoritmos {

    private static int total = 0;
    private static int fallidas = 0;

    public static void main(String[] args) {
        probarStemmer();
        probarLevenshtein();
        probarNeedlemanWunsch();
        probarJaccard();
        probarTfIdfCoseno();
        probarClusteringYCofenetica();

        System.out.println();
        System.out.println(total + " pruebas ejecutadas, " + fallidas + " fallidas.");
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    private static void probarStemmer() {
        seccion("Stemmer (Porter simplificado)");
        afirmarIgual("learning -> learn", "learn", Stemmer.stem("learning"));
        afirmarIgual("learned -> learn", "learn", Stemmer.stem("learned"));
        afirmarIgual("studies -> studi", "studi", Stemmer.stem("studies"));
        afirmarIgual("relational -> relat", "relat", Stemmer.stem("relational"));
        afirmarIgual("happy -> happi", "happi", Stemmer.stem("happy"));
    }

    private static void probarLevenshtein() {
        seccion("Levenshtein");
        LevenshteinSimilitud algoritmo = new LevenshteinSimilitud();

        // Caso de referencia clasico: distancia("kitten", "sitting") = 3
        int[][] matriz = algoritmo.construirMatriz("kitten", "sitting");
        afirmarIgual("distancia(kitten, sitting) = 3", 3, matriz[6][7]);

        // Cadenas identicas -> similitud 1
        double sim = algoritmo.calcular("hola mundo", "hola mundo").getSimilitud();
        afirmarIgual("similitud(x, x) = 1.0", 1.0, sim, 1e-9);

        // Cadenas completamente distintas de igual longitud -> distancia = longitud
        int[][] m2 = algoritmo.construirMatriz("aaaa", "bbbb");
        afirmarIgual("distancia(aaaa, bbbb) = 4", 4, m2[4][4]);
    }

    private static void probarNeedlemanWunsch() {
        seccion("Needleman-Wunsch");
        NeedlemanWunschSimilitud algoritmo = new NeedlemanWunschSimilitud();
        List<String> a = List.of("the", "cat", "sat");
        List<String> b = List.of("the", "cat", "sat");
        int[][] matriz = algoritmo.construirMatriz(a, b);
        // 3 coincidencias exactas -> puntaje = 3 * match(+2) = 6
        afirmarIgual("alineamiento identico de 3 tokens = 6", 6, matriz[3][3]);

        double sim = algoritmo.calcular("the cat sat", "the cat sat").getSimilitud();
        afirmarIgual("similitud(x, x) = 1.0", 1.0, sim, 1e-9);
    }

    private static void probarJaccard() {
        seccion("Jaccard");
        JaccardSimilitud jaccard = new JaccardSimilitud();

        // Mismo conjunto de palabras -> J = 1
        double sim1 = jaccard.calcular("machine learning models", "machine learning models").getSimilitud();
        afirmarIgual("J(x,x) = 1.0", 1.0, sim1, 1e-9);

        // Sin palabras en comun (tras quitar stopwords) -> J = 0
        double sim2 = jaccard.calcular("elephants swim rivers", "guitars sound loud").getSimilitud();
        afirmarIgual("J(disjuntos) = 0.0", 0.0, sim2, 1e-9);
    }

    private static void probarTfIdfCoseno() {
        seccion("TF-IDF + Similitud del Coseno");
        List<List<String>> corpus = new ArrayList<>();
        corpus.add(Preprocesador.procesarParaVectorial("the cat sat on the mat"));
        corpus.add(Preprocesador.procesarParaVectorial("the dog sat on the log"));
        corpus.add(Preprocesador.procesarParaVectorial("cats and dogs are pets"));

        TfIdfVectorizador vectorizador = new TfIdfVectorizador(corpus);
        Map<String, Double> v1 = vectorizador.vectorizar(corpus.get(0));
        double simIdentico = TfIdfVectorizador.similitudCoseno(v1, v1);
        afirmarIgual("cos(v, v) = 1.0", 1.0, simIdentico, 1e-9);

        Map<String, Double> vacio = new HashMap<>();
        double simVacio = TfIdfVectorizador.similitudCoseno(v1, vacio);
        afirmarIgual("cos(v, 0) = 0.0", 0.0, simVacio, 1e-9);
    }

    private static void probarClusteringYCofenetica() {
        seccion("Clustering jerarquico + correlacion cofenetica");
        // 4 puntos en linea: dos pares claramente mas cercanos entre si (0,1) y (2,3)
        double[][] distancias = {
                {0, 1, 10, 11},
                {1, 0, 11, 10},
                {10, 11, 0, 1},
                {11, 10, 1, 0}
        };
        List<String> etiquetas = List.of("A", "B", "C", "D");
        List<Integer> ids = List.of(1, 2, 3, 4);

        EstrategiaEnlace enlace = new EnlaceSimple();
        ClusteringJerarquico algoritmo = new ClusteringJerarquico();
        ResultadoClustering resultado = algoritmo.ejecutar(distancias, etiquetas, ids, enlace);

        // El primer merge debe unir el par mas cercano (distancia 1): (A,B) o (C,D)
        afirmarIgual("primera fusion a distancia 1", 1.0, resultado.getPasos().get(0).altura, 1e-9);
        afirmarIgual("numero de fusiones = n-1 = 3", 3, resultado.getPasos().size());

        Map<Integer, Integer> indicePorId = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) indicePorId.put(ids.get(i), i);

        CorrelacionCofenetica evaluador = new CorrelacionCofenetica();
        double[][] cofenetica = evaluador.calcularMatrizCofenetica(resultado.getRaiz(), indicePorId, 4);
        double correlacion = evaluador.calcularCorrelacion(distancias, cofenetica);
        // Esta estructura de datos (2 pares muy separados) debe dar una correlacion alta
        afirmar("correlacion cofenetica alta (>0.9) para estructura clara", correlacion > 0.9);
    }

    // -----------------------------------------------------------------
    private static void seccion(String nombre) {
        System.out.println("\n== " + nombre + " ==");
    }

    private static void afirmarIgual(String descripcion, Object esperado, Object obtenido) {
        total++;
        if (java.util.Objects.equals(esperado, obtenido)) {
            System.out.println("  [OK] " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLO] " + descripcion + " (esperado=" + esperado + ", obtenido=" + obtenido + ")");
        }
    }

    private static void afirmarIgual(String descripcion, double esperado, double obtenido, double tolerancia) {
        total++;
        if (Math.abs(esperado - obtenido) <= tolerancia) {
            System.out.println("  [OK] " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLO] " + descripcion + " (esperado=" + esperado + ", obtenido=" + obtenido + ")");
        }
    }

    private static void afirmar(String descripcion, boolean condicion) {
        total++;
        if (condicion) {
            System.out.println("  [OK] " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLO] " + descripcion);
        }
    }
}
