package co.edu.uniquindio.bibliometria.busqueda;

import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orquesta el Requerimiento 3: busca un patron en los 20 abstracts con KMP,
 * Boyer-Moore y Rabin-Karp, valida que los tres encuentren exactamente las
 * mismas ocurrencias y mide por separado el tiempo de <b>preprocesamiento</b>
 * del patron y el de <b>busqueda</b> sobre el corpus.
 * <p>
 * Medicion: igual que en el seguimiento de ordenamiento, se hace un
 * calentamiento de la JVM sin medir (para que el compilador JIT optimice el
 * codigo) y luego R repeticiones intercaladas entre algoritmos, medidas con
 * {@link System#nanoTime()}. Se reporta la mediana, que no se ve afectada
 * por pausas aisladas del recolector de basura.
 *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
 */
public class MotorBusqueda {

    /** Repeticiones medidas si no se indica otro valor. */
    public static final int REPETICIONES_POR_DEFECTO = 50;
    /** Longitud maxima aceptada para el patron. */
    public static final int LONGITUD_MAXIMA_PATRON = 200;
    /** Ejecuciones sin medir antes de empezar a medir. */
    private static final int CALENTAMIENTO = 100;
    /** Pasos de traza que se guardan como maximo por algoritmo. */
    private static final int LIMITE_TRAZA = 3000;
    /** Caracteres de contexto a cada lado de una ocurrencia en los fragmentos. */
    private static final int CONTEXTO = 40;

    private final List<Articulo> articulos;
    /** Abstract de cada articulo tal como esta (para buscar distinguiendo mayusculas). */
    private final List<char[]> textosOriginales = new ArrayList<>();
    /** Abstract de cada articulo en minusculas (para buscar sin distinguir mayusculas). */
    private final List<char[]> textosMinusculas = new ArrayList<>();
    /** Suma de las longitudes de los 20 abstracts (n total del corpus). */
    private final int caracteresTotales;

    /** Prepara una sola vez los textos de los 20 abstracts como arreglos de caracteres. */
    public MotorBusqueda(RepositorioArticulos repositorioArticulos) {
        this.articulos = repositorioArticulos.listarTodos();
        int total = 0;
        for (Articulo a : articulos) {
            char[] original = a.getAbstractOriginal().toCharArray();
            textosOriginales.add(original);
            textosMinusculas.add(aMinusculas(original));
            total += original.length;
        }
        this.caracteresTotales = total;
    }

    /**
     * Convierte a minusculas caracter por caracter. Se hace asi (y no con
     * {@code String.toLowerCase}) para que el texto conserve exactamente la
     * misma longitud y las posiciones encontradas sirvan sobre el original.
     */
    public static char[] aMinusculas(char[] texto) {
        char[] resultado = new char[texto.length];
        for (int i = 0; i < texto.length; i++) {
            resultado[i] = Character.toLowerCase(texto[i]);
        }
        return resultado;
    }

    /** @return los tres algoritmos, en el orden en que se ejecutan y se muestran */
    public static List<AlgoritmoBusqueda<?>> crearAlgoritmos(long moduloRabinKarp) {
        List<AlgoritmoBusqueda<?>> lista = new ArrayList<>();
        lista.add(new KnuthMorrisPratt());
        lista.add(new BoyerMoore());
        lista.add(new RabinKarp(moduloRabinKarp));
        return lista;
    }

    public int getCaracteresTotales() {
        return caracteresTotales;
    }

    /**
     * Ejecuta la busqueda completa y arma la respuesta de la API. Es
     * {@code synchronized} para que dos peticiones simultaneas no compitan
     * por la CPU y distorsionen los tiempos.
     *
     * @param patronTexto          patron a buscar (1 a 200 caracteres)
     * @param distinguirMayusculas si es false, patron y textos se comparan en minusculas
     * @param moduloRabinKarp      primo q de Rabin-Karp
     * @param articuloTraza        id del articulo cuya traza paso a paso se devuelve, o null
     * @param repeticiones         repeticiones medidas por algoritmo
     */
    public synchronized Map<String, Object> buscar(String patronTexto, boolean distinguirMayusculas,
                                                   long moduloRabinKarp, Integer articuloTraza, int repeticiones) {
        char[] patron = distinguirMayusculas ? patronTexto.toCharArray() : aMinusculas(patronTexto.toCharArray());
        List<char[]> textos = distinguirMayusculas ? textosOriginales : textosMinusculas;
        List<AlgoritmoBusqueda<?>> algoritmos = crearAlgoritmos(moduloRabinKarp);

        List<Map<String, Object>> resultadosAlgoritmos = new ArrayList<>();
        List<List<List<Integer>>> posicionesPorAlgoritmo = new ArrayList<>();
        for (AlgoritmoBusqueda<?> algoritmo : algoritmos) {
            List<List<Integer>> posiciones = new ArrayList<>();
            resultadosAlgoritmos.add(ejecutar(algoritmo, patron, textos, repeticiones, posiciones));
            posicionesPorAlgoritmo.add(posiciones);
        }

        // Validacion: los tres deben encontrar las mismas posiciones en cada abstract.
        boolean coinciden = true;
        for (int a = 1; a < algoritmos.size(); a++) {
            coinciden &= posicionesPorAlgoritmo.get(a).equals(posicionesPorAlgoritmo.get(0));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("patron", patronTexto);
        respuesta.put("longitudPatron", patron.length);
        respuesta.put("distinguirMayusculas", distinguirMayusculas);
        respuesta.put("moduloRabinKarp", moduloRabinKarp);
        respuesta.put("articulos", articulos.size());
        respuesta.put("caracteresTotales", caracteresTotales);
        respuesta.put("repeticiones", repeticiones);
        respuesta.put("algoritmos", resultadosAlgoritmos);
        respuesta.put("losTresCoinciden", coinciden);
        respuesta.put("ocurrencias", ocurrenciasPorArticulo(posicionesPorAlgoritmo.get(0), patron.length));
        if (articuloTraza != null) {
            respuesta.put("traza", trazaDeArticulo(algoritmos, patron, textos, articuloTraza));
        }
        return respuesta;
    }

    /**
     * Ejecuta un algoritmo sobre los 20 abstracts: primero el calentamiento,
     * luego R repeticiones midiendo por separado preprocesar y buscar.
     * Metodo generico para conservar el tipo de la estructura auxiliar E.
     */
    private <E> Map<String, Object> ejecutar(AlgoritmoBusqueda<E> algoritmo, char[] patron, List<char[]> textos,
                                             int repeticiones, List<List<Integer>> posicionesSalida) {
        // 1. Calentamiento sin medir.
        for (int r = 0; r < CALENTAMIENTO; r++) {
            E estructura = algoritmo.preprocesar(patron);
            for (char[] texto : textos) {
                algoritmo.buscar(texto, patron, estructura, null);
            }
        }

        // 2. Medicion. El preprocesamiento se hace UNA vez por patron (no una vez por abstract),
        //    porque la estructura solo depende del patron y se reutiliza en los 20 textos.
        double[] tiemposPre = new double[repeticiones];
        double[] tiemposBusqueda = new double[repeticiones];
        E estructura = null;
        for (int r = 0; r < repeticiones; r++) {
            long inicio = System.nanoTime();
            estructura = algoritmo.preprocesar(patron);
            long finPre = System.nanoTime();
            for (char[] texto : textos) {
                algoritmo.buscar(texto, patron, estructura, null);
            }
            long finBusqueda = System.nanoTime();
            tiemposPre[r] = (finPre - inicio) / 1_000_000.0;
            tiemposBusqueda[r] = (finBusqueda - finPre) / 1_000_000.0;
        }

        // 3. Una ejecucion mas (fuera de la medicion) para recoger posiciones y contadores.
        long comparaciones = 0, alineamientos = 0, coincidenciasHash = 0, colisiones = 0;
        int totalOcurrencias = 0;
        for (char[] texto : textos) {
            ResultadoBusqueda resultado = algoritmo.buscar(texto, patron, estructura, null);
            posicionesSalida.add(resultado.getPosiciones());
            totalOcurrencias += resultado.getPosiciones().size();
            comparaciones += resultado.getComparaciones();
            alineamientos += resultado.getAlineamientos();
            coincidenciasHash += resultado.getCoincidenciasHash();
            colisiones += resultado.getColisiones();
        }

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("algoritmo", algoritmo.getNombre());
        mapa.put("complejidadPreprocesamiento", algoritmo.getComplejidadPreprocesamiento());
        mapa.put("complejidadBusqueda", algoritmo.getComplejidadBusqueda());
        mapa.put("ocurrencias", totalOcurrencias);
        mapa.put("comparaciones", comparaciones);
        mapa.put("alineamientos", alineamientos);
        mapa.put("comparacionesPorCaracter", caracteresTotales == 0 ? 0.0 : comparaciones / (double) caracteresTotales);
        if (algoritmo instanceof RabinKarp) {
            mapa.put("coincidenciasHash", coincidenciasHash);
            mapa.put("colisiones", colisiones);
        }
        mapa.put("preprocesamientoMedianaMs", mediana(tiemposPre));
        mapa.put("busquedaMedianaMs", mediana(tiemposBusqueda));
        mapa.put("busquedaMinimoMs", minimo(tiemposBusqueda));
        mapa.put("estructura", algoritmo.describirEstructura(patron, estructura));
        return mapa;
    }

    /** Agrupa las ocurrencias por articulo, con un fragmento de contexto por cada una. */
    private List<Map<String, Object>> ocurrenciasPorArticulo(List<List<Integer>> posiciones, int m) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int i = 0; i < articulos.size(); i++) {
            List<Integer> pos = posiciones.get(i);
            if (pos.isEmpty()) continue;
            String texto = articulos.get(i).getAbstractOriginal();
            List<Map<String, Object>> fragmentos = new ArrayList<>();
            for (int p : pos) {
                Map<String, Object> fragmento = new LinkedHashMap<>();
                fragmento.put("posicion", p);
                fragmento.put("antes", texto.substring(Math.max(0, p - CONTEXTO), p));
                fragmento.put("coincidencia", texto.substring(p, p + m));
                fragmento.put("despues", texto.substring(p + m, Math.min(texto.length(), p + m + CONTEXTO)));
                fragmentos.add(fragmento);
            }
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("articuloId", articulos.get(i).getId());
            mapa.put("titulo", articulos.get(i).getTitulo());
            mapa.put("total", pos.size());
            mapa.put("fragmentos", fragmentos);
            lista.add(mapa);
        }
        return lista;
    }

    /** Traza paso a paso de los tres algoritmos sobre un solo abstract. */
    private Map<String, Object> trazaDeArticulo(List<AlgoritmoBusqueda<?>> algoritmos, char[] patron,
                                                List<char[]> textos, int articuloId) {
        int indice = -1;
        for (int i = 0; i < articulos.size(); i++) {
            if (articulos.get(i).getId() == articuloId) indice = i;
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        if (indice < 0) {
            mapa.put("error", "Articulo no encontrado: " + articuloId);
            return mapa;
        }
        mapa.put("articuloId", articuloId);
        mapa.put("longitudTexto", textos.get(indice).length);
        List<Map<String, Object>> porAlgoritmo = new ArrayList<>();
        for (AlgoritmoBusqueda<?> algoritmo : algoritmos) {
            porAlgoritmo.add(trazar(algoritmo, patron, textos.get(indice)));
        }
        mapa.put("algoritmos", porAlgoritmo);
        return mapa;
    }

    private <E> Map<String, Object> trazar(AlgoritmoBusqueda<E> algoritmo, char[] patron, char[] texto) {
        Traza traza = new Traza(LIMITE_TRAZA);
        ResultadoBusqueda resultado = algoritmo.buscar(texto, patron, algoritmo.preprocesar(patron), traza);
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("algoritmo", algoritmo.getNombre());
        mapa.put("posiciones", resultado.getPosiciones());
        mapa.put("comparaciones", resultado.getComparaciones());
        mapa.put("totalPasos", traza.getTotalPasos());
        mapa.put("pasos", traza.getPasos());
        return mapa;
    }

    /** Mediana de una copia del arreglo, ordenada con insercion (R es pequeno). */
    static double mediana(double[] valores) {
        double[] copia = new double[valores.length];
        for (int i = 0; i < valores.length; i++) copia[i] = valores[i];
        for (int i = 1; i < copia.length; i++) {
            double actual = copia[i];
            int j = i - 1;
            while (j >= 0 && copia[j] > actual) {
                copia[j + 1] = copia[j];
                j--;
            }
            copia[j + 1] = actual;
        }
        int mitad = copia.length / 2;
        return copia.length % 2 == 1 ? copia[mitad] : (copia[mitad - 1] + copia[mitad]) / 2.0;
    }

    static double minimo(double[] valores) {
        double min = Double.MAX_VALUE;
        for (double v : valores) min = Math.min(min, v);
        return min;
    }
}
