package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.busqueda.AlgoritmoBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.BoyerMoore;
import co.edu.uniquindio.bibliometria.busqueda.KnuthMorrisPratt;
import co.edu.uniquindio.bibliometria.busqueda.MotorBusqueda;
import co.edu.uniquindio.bibliometria.busqueda.RabinKarp;
import co.edu.uniquindio.bibliometria.busqueda.ResultadoBusqueda;
import co.edu.uniquindio.bibliometria.clustering.ClusteringJerarquico;
import co.edu.uniquindio.bibliometria.clustering.CorteDendrograma;
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
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.JaccardSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.RepositorioEmbeddings;
import co.edu.uniquindio.bibliometria.similitud.ia.SimilitudEmbeddings;
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
        probarComparacionVarios();
        probarTfIdfCoseno();
        probarEmbeddings();
        probarClusteringYCofenetica();
        probarClusteringCorpus();
        probarOrdenamientosNoComparativos();
        probarBusquedaDePatrones();

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

        // {algorithm, cluster, dendrogram} vs {cluster, dendrogram, network}: 2 comunes, union 4 -> 0.5.
        // Las palabras repetidas no cuentan dos veces.
        var r = jaccard.calcular("algorithm cluster dendrogram cluster", "cluster dendrogram network network");
        afirmarIgual("J({a,c,d},{c,d,n}) = 2/4 = 0.5", 0.5, r.getSimilitud(), 1e-9);
        afirmarIgual("Jaccard: |A u B| = |A| + |B| - |A n B| = 4", 4, r.getDetalle().get("tamanoUnion"));

        // Contra el oraculo de la libreria estandar (solo en la prueba) sobre todos los pares del corpus.
        List<co.edu.uniquindio.bibliometria.model.Articulo> corpus = new RepositorioArticulos().listarTodos();
        int diferencias = 0;
        for (int i = 0; i < corpus.size(); i++) {
            for (int j = i + 1; j < corpus.size(); j++) {
                String a = corpus.get(i).getAbstractOriginal(), b = corpus.get(j).getAbstractOriginal();
                java.util.Set<String> ca = new java.util.HashSet<>(Preprocesador.procesarParaVectorial(a));
                java.util.Set<String> cb = new java.util.HashSet<>(Preprocesador.procesarParaVectorial(b));
                java.util.Set<String> inter = new java.util.HashSet<>(ca);
                inter.retainAll(cb);
                java.util.Set<String> union = new java.util.HashSet<>(ca);
                union.addAll(cb);
                double esperado = (double) inter.size() / union.size();
                if (Math.abs(esperado - jaccard.calcular(a, b).getSimilitud()) > 1e-12) diferencias++;
            }
        }
        afirmarIgual("Jaccard explicito = oraculo en los 190 pares del corpus", 0, diferencias);
    }

    /**
     * Verifica los dos enfoques de IA (GloVe y FastText): carga de cada modelo
     * con sus dimensiones, cobertura del vocabulario, cos(x,x) = 1 con
     * distancia 0, y el coseno contra un calculo independiente sobre dos
     * vectores pequenos conocidos.
     */
    private static void probarEmbeddings() {
        seccion("Embeddings (GloVe y FastText)");
        RepositorioEmbeddings glove = RepositorioEmbeddings.glove();
        RepositorioEmbeddings fastText = RepositorioEmbeddings.fastText();
        afirmarIgual("GloVe: 100 dimensiones", 100, glove.obtenerVector("education").length);
        afirmarIgual("FastText: 300 dimensiones", 300, fastText.obtenerVector("education").length);
        afirmar("FastText cubre al menos 970 palabras del corpus", fastText.tamano() >= 970);

        List<String> texto = List.of("artificial", "intelligence", "education", "students");
        for (SimilitudEmbeddings algoritmo : List.of(SimilitudEmbeddings.conGlove(glove), SimilitudEmbeddings.conFastText(fastText))) {
            var r = algoritmo.calcular(texto, texto);
            afirmarIgual(algoritmo.getNombre() + ": cos(x,x) = 1", 1.0, r.getSimilitud(), 1e-9);
            afirmarIgual(algoritmo.getNombre() + ": dist(x,x) = 0", 0.0, (Double) r.getDetalle().get("distanciaEuclidiana"), 1e-9);
        }

        // Una sola palabra por documento: el vector del documento es el de la palabra, asi que el
        // coseno debe ser el de los dos vectores, calculado aqui de forma independiente.
        double[] u = fastText.obtenerVector("teacher");
        double[] v = fastText.obtenerVector("student");
        double punto = 0, nu = 0, nv = 0;
        for (int i = 0; i < u.length; i++) { punto += u[i] * v[i]; nu += u[i] * u[i]; nv += v[i] * v[i]; }
        double esperado = punto / (Math.sqrt(nu) * Math.sqrt(nv));
        double obtenido = SimilitudEmbeddings.conFastText(fastText)
                .calcular(List.of("teacher"), List.of("student")).getSimilitud();
        afirmarIgual("FastText: cos(teacher, student) = calculo independiente", esperado, obtenido, 1e-12);
    }

    /**
     * Verifica la comparacion de dos o mas articulos (Requerimiento 1): numero
     * de pares, matrices simetricas con diagonal 1 y valores iguales a los de
     * la comparacion individual de cada par.
     */
    private static void probarComparacionVarios() {
        seccion("Similitud entre dos o mas articulos");
        RepositorioArticulos repositorio = new RepositorioArticulos();
        MotorSimilitud motor = new MotorSimilitud(repositorio, RepositorioEmbeddings.glove(), RepositorioEmbeddings.fastText());
        List<co.edu.uniquindio.bibliometria.model.Articulo> seleccion = List.of(
                repositorio.buscarPorId(1).get(), repositorio.buscarPorId(2).get(),
                repositorio.buscarPorId(16).get(), repositorio.buscarPorId(19).get());
        Map<String, Object> respuesta = motor.compararVarios(seleccion);

        afirmarIgual("4 articulos -> 4*3/2 = 6 pares", 6, ((List<?>) respuesta.get("pares")).size());
        List<?> matrices = (List<?>) respuesta.get("matrices");
        afirmarIgual("una matriz por algoritmo (6)", 6, matrices.size());

        boolean simetricas = true, diagonalUno = true, igualesAlPar = true;
        for (int a = 0; a < matrices.size(); a++) {
            double[][] v = (double[][]) ((Map<?, ?>) matrices.get(a)).get("valores");
            for (int i = 0; i < 4; i++) {
                diagonalUno &= v[i][i] == 1.0;
                for (int j = 0; j < 4; j++) simetricas &= v[i][j] == v[j][i];
            }
            // Par (articulo 2, articulo 16) = posiciones 1 y 2.
            double individual = motor.compararArticulos(seleccion.get(1), seleccion.get(2)).get(a).getSimilitud();
            igualesAlPar &= Math.abs(v[1][2] - individual) < 1e-12;
        }
        afirmar("matrices simetricas", simetricas);
        afirmar("diagonal = 1", diagonalUno);
        afirmar("cada celda = comparacion individual del par", igualesAlPar);
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

        // Corte en 2 grupos: deben quedar los dos pares {1,2} y {3,4}.
        List<List<Integer>> dos = CorteDendrograma.cortar(resultado.getRaiz(), 2);
        List<String> textoGrupos = new ArrayList<>();
        for (List<Integer> g : dos) {
            List<Integer> copia = new ArrayList<>(g);
            copia.sort(null);
            textoGrupos.add(copia.toString());
        }
        textoGrupos.sort(null);
        afirmarIgual("corte en k=2 = {1,2} y {3,4}", "[[1, 2], [3, 4]]", textoGrupos.toString());
        // Silueta: a(i) = 1 y b(i) = (10 + 11) / 2 = 10.5 para los 4 puntos -> S = 9.5 / 10.5.
        afirmarIgual("silueta de los 2 pares = 9.5/10.5", 9.5 / 10.5,
                co.edu.uniquindio.bibliometria.clustering.evaluacion.Silueta.calcular(distancias, dos, indicePorId), 1e-12);
    }

    /**
     * Verifica el clustering sobre el corpus real contra una implementacion
     * ingenua usada solo como oraculo: en cada paso recalcula la distancia
     * entre clusters directamente sobre todos los pares de articulos (minimo,
     * maximo o promedio), sin la formula de Lance-Williams. Las 19 alturas de
     * fusion deben coincidir. Tambien verifica los cortes en k grupos.
     */
    private static void probarClusteringCorpus() {
        seccion("Clustering sobre el corpus (contra oraculo ingenuo)");
        RepositorioArticulos repositorio = new RepositorioArticulos();
        MotorSimilitud motorSimilitud = new MotorSimilitud(repositorio, RepositorioEmbeddings.glove(), RepositorioEmbeddings.fastText());
        co.edu.uniquindio.bibliometria.clustering.MotorClustering motor =
                new co.edu.uniquindio.bibliometria.clustering.MotorClustering(repositorio, motorSimilitud);
        double[][] d = motor.getMatrizDistancias();
        String[] esperadoK6 = {"[15, 1, 1, 1, 1, 1]", "[7, 5, 5, 1, 1, 1]", "[15, 1, 1, 1, 1, 1]"};
        for (int t = 0; t < 3; t++) {
            ResultadoClustering r = motor.getResultados().get(t);
            List<Double> oraculo = alturasIngenuas(d, t);
            double maxDif = 0;
            for (int k = 0; k < oraculo.size(); k++) maxDif = Math.max(maxDif, Math.abs(oraculo.get(k) - r.getPasos().get(k).altura));
            afirmar(r.getNombreEnlace() + ": 19 alturas = oraculo ingenuo", r.getPasos().size() == 19 && maxDif < 1e-12);

            boolean particionesValidas = true;
            for (Map<String, Object> corte : r.getCortes()) {
                @SuppressWarnings("unchecked")
                List<List<Integer>> grupos = (List<List<Integer>>) corte.get("grupos");
                java.util.Set<Integer> vistos = new java.util.HashSet<>();
                int total = 0;
                for (List<Integer> g : grupos) { total += g.size(); vistos.addAll(g); }
                particionesValidas &= grupos.size() == (Integer) corte.get("k") && total == 20 && vistos.size() == 20;
            }
            afirmar(r.getNombreEnlace() + ": cortes k=2..6 son particiones de los 20 articulos", particionesValidas);
            afirmarIgual(r.getNombreEnlace() + ": tamanos del corte en k=6", esperadoK6[t],
                    java.util.Arrays.toString((int[]) r.getCortes().get(4).get("tamanos")));
        }
    }

    /** Oraculo de prueba: clustering aglomerativo ingenuo que recalcula cada distancia sobre todos los pares. */
    private static List<Double> alturasIngenuas(double[][] d, int tipo) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < d.length; i++) clusters.add(new ArrayList<>(List.of(i)));
        List<Double> alturas = new ArrayList<>();
        while (clusters.size() > 1) {
            double mejor = Double.MAX_VALUE;
            int bi = -1, bj = -1;
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double min = Double.MAX_VALUE, max = -1, suma = 0;
                    for (int x : clusters.get(i)) for (int y : clusters.get(j)) {
                        min = Math.min(min, d[x][y]); max = Math.max(max, d[x][y]); suma += d[x][y];
                    }
                    double dist = tipo == 0 ? min : tipo == 1 ? max : suma / (clusters.get(i).size() * clusters.get(j).size());
                    if (dist < mejor) { mejor = dist; bi = i; bj = j; }
                }
            }
            clusters.get(bi).addAll(clusters.get(bj));
            clusters.remove(bj);
            alturas.add(mejor);
        }
        return alturas;
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

    /**
     * Verifica KMP, Boyer-Moore y Rabin-Karp: tabla de fallos conocida
     * (Cormen et al.), casos pequenos con solapamiento, 3000 casos aleatorios
     * contra una busqueda ingenua usada solo como oraculo, colisiones de
     * Rabin-Karp con un modulo pequeno y coincidencia de los tres sobre el
     * corpus real.
     */
    private static void probarBusquedaDePatrones() {
        seccion("Busqueda de patrones (KMP, Boyer-Moore, Rabin-Karp)");

        // Funcion de prefijo de "ababaca" (ejemplo de Cormen et al., cap. 32.4).
        afirmarIgual("KMP: pi(\"ababaca\") = [0, 0, 1, 2, 3, 0, 1]", "[0, 0, 1, 2, 3, 0, 1]",
                java.util.Arrays.toString(new KnuthMorrisPratt().preprocesar("ababaca".toCharArray())));

        List<AlgoritmoBusqueda<?>> algoritmos = List.of(new KnuthMorrisPratt(), new BoyerMoore(),
                new RabinKarp(), new RabinKarp(RabinKarp.MODULO_DEMOSTRACION));
        String[][] casos = {
                {"ababcabcabababd", "ababd", "[10]"},
                {"aaaaa", "aa", "[0, 1, 2, 3]"},
                {"abc", "abcd", "[]"},
                {"the learning of machine learning", "learning", "[4, 24]"},
                {"abababab", "abab", "[0, 2, 4]"},
        };
        for (String[] caso : casos) {
            for (AlgoritmoBusqueda<?> algoritmo : algoritmos) {
                afirmarIgual(etiqueta(algoritmo) + ": \"" + caso[1] + "\" en \"" + caso[0] + "\"", caso[2],
                        buscarPosiciones(algoritmo, caso[0], caso[1]).toString());
            }
        }

        // Casos aleatorios sobre alfabetos pequenos (muchas repeticiones parciales,
        // que es donde fallan las tablas mal construidas) contra el oraculo ingenuo.
        java.util.Random aleatorio = new java.util.Random(7);
        int[] fallosPorAlgoritmo = new int[algoritmos.size()];
        for (int prueba = 0; prueba < 3000; prueba++) {
            int alfabeto = 2 + aleatorio.nextInt(3);
            String texto = cadenaAleatoria(aleatorio, 1 + aleatorio.nextInt(60), alfabeto);
            String patron = cadenaAleatoria(aleatorio, 1 + aleatorio.nextInt(8), alfabeto);
            List<Integer> esperado = busquedaIngenua(texto, patron);
            for (int a = 0; a < algoritmos.size(); a++) {
                if (!esperado.equals(buscarPosiciones(algoritmos.get(a), texto, patron))) fallosPorAlgoritmo[a]++;
            }
        }
        for (int a = 0; a < algoritmos.size(); a++) {
            afirmarIgual(etiqueta(algoritmos.get(a)) + ": 3000 casos aleatorios = oraculo", 0, fallosPorAlgoritmo[a]);
        }

        // Rabin-Karp con q = 101: debe haber colisiones espurias y aun asi el resultado es correcto.
        String parrafo = new RepositorioArticulos().listarTodos().get(0).getAbstractOriginal().toLowerCase();
        RabinKarp rkPequeno = new RabinKarp(RabinKarp.MODULO_DEMOSTRACION);
        char[] p = "learning".toCharArray();
        ResultadoBusqueda r = rkPequeno.buscar(parrafo.toCharArray(), p, rkPequeno.preprocesar(p), null);
        afirmar("Rabin-Karp (q=101): hay colisiones espurias en el abstract 1", r.getColisiones() > 0);
        afirmarIgual("Rabin-Karp (q=101): mismas ocurrencias que el oraculo",
                busquedaIngenua(parrafo, "learning"), r.getPosiciones());

        // Sobre el corpus real: los tres algoritmos coinciden para varios patrones.
        MotorBusqueda motor = new MotorBusqueda(new RepositorioArticulos());
        for (String patronCorpus : new String[]{"learning", "artificial intelligence", "ai", "e", "chatgpt"}) {
            Map<String, Object> respuesta = motor.buscar(patronCorpus, false, RabinKarp.MODULO_POR_DEFECTO, null, 2);
            afirmar("corpus: los tres coinciden para \"" + patronCorpus + "\"", (Boolean) respuesta.get("losTresCoinciden"));
        }
    }

    /** Nombre del algoritmo; en Rabin-Karp incluye el modulo para distinguir las dos variantes. */
    private static String etiqueta(AlgoritmoBusqueda<?> algoritmo) {
        return algoritmo instanceof RabinKarp
                ? algoritmo.getNombre() + " (q=" + ((RabinKarp) algoritmo).getModulo() + ")" : algoritmo.getNombre();
    }

    /** Ejecuta un algoritmo de busqueda completo (preprocesar + buscar) y devuelve las posiciones. */
    private static <E> List<Integer> buscarPosiciones(AlgoritmoBusqueda<E> algoritmo, String texto, String patron) {
        char[] p = patron.toCharArray();
        return algoritmo.buscar(texto.toCharArray(), p, algoritmo.preprocesar(p), null).getPosiciones();
    }

    /** Oraculo de prueba: compara el patron en cada posicion posible, sin ninguna optimizacion. */
    private static List<Integer> busquedaIngenua(String texto, String patron) {
        List<Integer> posiciones = new ArrayList<>();
        for (int s = 0; s + patron.length() <= texto.length(); s++) {
            int j = 0;
            while (j < patron.length() && texto.charAt(s + j) == patron.charAt(j)) j++;
            if (j == patron.length()) posiciones.add(s);
        }
        return posiciones;
    }

    /** Cadena aleatoria de la longitud dada con letras de 'a' en adelante. */
    private static String cadenaAleatoria(java.util.Random aleatorio, int longitud, int alfabeto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longitud; i++) sb.append((char) ('a' + aleatorio.nextInt(alfabeto)));
        return sb.toString();
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
