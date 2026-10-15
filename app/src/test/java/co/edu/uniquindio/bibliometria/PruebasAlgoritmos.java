package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.clustering.ClusteringJerarquico;
import co.edu.uniquindio.bibliometria.clustering.EnlaceSimple;
import co.edu.uniquindio.bibliometria.clustering.EstrategiaEnlace;
import co.edu.uniquindio.bibliometria.clustering.ResultadoClustering;
import co.edu.uniquindio.bibliometria.clustering.evaluacion.CorrelacionCofenetica;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.ordenamiento.AlgoritmoOrdenamiento;
import co.edu.uniquindio.bibliometria.ordenamiento.BucketSort;
import co.edu.uniquindio.bibliometria.ordenamiento.CountingSort;
import co.edu.uniquindio.bibliometria.ordenamiento.MotorRanking;
import co.edu.uniquindio.bibliometria.ordenamiento.RadixSort;
import co.edu.uniquindio.bibliometria.ordenamiento.ResultadoOrdenamiento;
import co.edu.uniquindio.bibliometria.ordenamiento.TerminoFrecuencia;
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

    /** Numero de aserciones ejecutadas. */
    private static int total = 0;
    /** Numero de aserciones que fallaron. */
    private static int fallidas = 0;

    /** Ejecuta todas las pruebas, imprime el resumen y termina con codigo 1 si alguna fallo. */
    public static void main(String[] args) {
        probarStemmer();
        probarLevenshtein();
        probarNeedlemanWunsch();
        probarJaccard();
        probarTfIdfCoseno();
        probarClusteringYCofenetica();
        probarOrdenamientosNoComparativos();

        System.out.println();
        System.out.println(total + " pruebas ejecutadas, " + fallidas + " fallidas.");
        if (fallidas > 0) {
            System.exit(1);
        }
    }

    /** Verifica el stemmer con palabras cuya raiz de Porter es conocida. */
    private static void probarStemmer() {
        seccion("Stemmer (Porter simplificado)");
        afirmarIgual("learning -> learn", "learn", Stemmer.stem("learning"));
        afirmarIgual("learned -> learn", "learn", Stemmer.stem("learned"));
        afirmarIgual("studies -> studi", "studi", Stemmer.stem("studies"));
        afirmarIgual("relational -> relat", "relat", Stemmer.stem("relational"));
        afirmarIgual("happy -> happi", "happi", Stemmer.stem("happy"));
    }

    /** Verifica Levenshtein con el caso clasico kitten/sitting, cadenas iguales y cadenas sin letras comunes. */
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

    /** Verifica Needleman-Wunsch: un alineamiento identico debe dar el puntaje maximo y similitud 1. */
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

    /** Verifica Jaccard: 1 para textos iguales y 0 para textos sin palabras en comun. */
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

    /** Verifica el coseno TF-IDF: 1 de un vector consigo mismo y 0 contra un vector vacio. */
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

    /** Verifica el clustering y la correlacion cofenetica con 4 puntos formados por dos pares bien separados. */
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

    /**
     * Verifica Counting, Radix y Bucket Sort: orden descendente y estable,
     * arreglo vacio, entrada intacta, 5000 claves aleatorias contra un
     * oraculo y coincidencia de los tres sobre el corpus real.
     */
    private static void probarOrdenamientosNoComparativos() {
        seccion("Ordenamiento no comparativo (Counting, Radix, Bucket)");
        List<AlgoritmoOrdenamiento> algoritmos = List.of(new CountingSort(), new RadixSort(), new BucketSort());

        // Caso pequeno con empates: el orden esperado respeta la estabilidad
        // (a igual frecuencia, primero el que venia antes en la entrada).
        TerminoFrecuencia[] entrada = {
                new TerminoFrecuencia("ai", 5), new TerminoFrecuencia("data", 12),
                new TerminoFrecuencia("model", 5), new TerminoFrecuencia("learning", 105),
                new TerminoFrecuencia("ethics", 1), new TerminoFrecuencia("study", 12)
        };
        String esperado = "[learning=105, data=12, study=12, ai=5, model=5, ethics=1]";
        for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
            afirmarIgual(algoritmo.getNombre() + ": orden descendente y estable", esperado,
                    java.util.Arrays.toString(algoritmo.ordenarDescendente(entrada)));
            afirmarIgual(algoritmo.getNombre() + ": arreglo vacio", 0,
                    algoritmo.ordenarDescendente(new TerminoFrecuencia[0]).length);
        }
        afirmarIgual("la entrada no se modifica", "ai=5", entrada[0].toString());

        // Caso aleatorio grande (distribucion sesgada como la de Zipf, con
        // claves de 1 a 4 digitos) comparado contra un oraculo independiente.
        java.util.Random aleatorio = new java.util.Random(42);
        TerminoFrecuencia[] grande = new TerminoFrecuencia[5000];
        for (int i = 0; i < grande.length; i++) {
            int frecuencia = aleatorio.nextInt(10) < 7 ? 1 + aleatorio.nextInt(3) : aleatorio.nextInt(5000);
            grande[i] = new TerminoFrecuencia("t" + i, frecuencia);
        }
        TerminoFrecuencia[] oraculo = grande.clone();
        // Oraculo de prueba: ordenamiento estable de la libreria (solo en las pruebas).
        java.util.Arrays.sort(oraculo, (x, y) -> Integer.compare(y.getFrecuencia(), x.getFrecuencia()));
        for (AlgoritmoOrdenamiento algoritmo : algoritmos) {
            afirmar(algoritmo.getNombre() + ": 5000 claves aleatorias = oraculo",
                    MotorRanking.mismoOrden(oraculo, algoritmo.ordenarDescendente(grande)));
        }

        // Sobre el corpus real: los tres algoritmos deben coincidir en el ranking completo.
        MotorRanking motor = new MotorRanking(new RepositorioArticulos());
        List<ResultadoOrdenamiento> resultados = motor.ejecutar(15, 3);
        for (ResultadoOrdenamiento r : resultados) {
            afirmar(r.getAlgoritmo() + ": corpus ordenado descendentemente",
                    MotorRanking.estaOrdenadoDescendente(r.getOrdenado()));
            afirmar(r.getAlgoritmo() + ": mismo ranking que Counting Sort",
                    MotorRanking.mismoOrden(resultados.get(0).getOrdenado(), r.getOrdenado()));
        }
    }

    // -----------------------------------------------------------------
    /** Imprime un titulo para agrupar las pruebas de un mismo algoritmo en la consola. */
    private static void seccion(String nombre) {
        System.out.println("\n== " + nombre + " ==");
    }

    /** Cuenta una prueba e imprime [OK] si esperado y obtenido son iguales, o [FALLO] con ambos valores. */
    private static void afirmarIgual(String descripcion, Object esperado, Object obtenido) {
        total++;
        if (java.util.Objects.equals(esperado, obtenido)) {
            System.out.println("  [OK] " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLO] " + descripcion + " (esperado=" + esperado + ", obtenido=" + obtenido + ")");
        }
    }

    /** Version para decimales: acepta diferencias de hasta {@code tolerancia} por errores de redondeo. */
    private static void afirmarIgual(String descripcion, double esperado, double obtenido, double tolerancia) {
        total++;
        if (Math.abs(esperado - obtenido) <= tolerancia) {
            System.out.println("  [OK] " + descripcion);
        } else {
            fallidas++;
            System.out.println("  [FALLO] " + descripcion + " (esperado=" + esperado + ", obtenido=" + obtenido + ")");
        }
    }

    /** Cuenta una prueba e imprime [OK] si la condicion es verdadera, o [FALLO] si no. */
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
