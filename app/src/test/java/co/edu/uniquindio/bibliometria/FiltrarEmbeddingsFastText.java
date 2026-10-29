package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utilidad de preparacion de datos (no forma parte de la aplicacion): filtra
 * el modelo FastText preentrenado {@code wiki-news-300d-1M.vec.zip}
 * (https://fasttext.cc/docs/en/english-vectors.html, ~1 millon de palabras,
 * 300 dimensiones, ~680 MB comprimido) para conservar solo las palabras que
 * aparecen en los 20 abstracts, y escribe el resultado en el formato que
 * carga {@code RepositorioEmbeddings}: {@code palabra<TAB>v1 v2 ... v300}.
 * <p>
 * El vocabulario se construye con el mismo preprocesamiento que usa la
 * aplicacion para los embeddings ({@code Preprocesador.procesarParaEmbeddings}:
 * minusculas, sin stopwords, sin stemming), asi que se guardan exactamente
 * las palabras que la aplicacion va a buscar. FastText distingue mayusculas;
 * si una palabra no esta en minusculas se usa la primera variante que
 * coincide sin distinguir mayusculas (el archivo esta ordenado por
 * frecuencia, asi que es la mas frecuente).
 * <p>
 * Uso: {@code java -cp <clases> co.edu.uniquindio.bibliometria.FiltrarEmbeddingsFastText
 * <ruta a wiki-news-300d-1M.vec.zip> <archivo de salida .tsv>}
 */
public class FiltrarEmbeddingsFastText {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: FiltrarEmbeddingsFastText <wiki-news-300d-1M.vec.zip> <salida.tsv>");
            System.exit(1);
        }
        // 1. Vocabulario del corpus, con el mismo preprocesamiento de la aplicacion.
        Set<String> vocabulario = new LinkedHashSet<>();
        for (Articulo a : new RepositorioArticulos().listarTodos()) {
            vocabulario.addAll(a.getTokensEmbeddings());
        }
        System.out.println("Vocabulario del corpus: " + vocabulario.size() + " palabras");

        // 2. Recorrer el archivo .vec (dentro del zip) una sola vez.
        Map<String, String> exactas = new LinkedHashMap<>();
        Map<String, String> sinMayusculas = new LinkedHashMap<>();
        int lineas = 0;
        try (ZipFile zip = new ZipFile(args[0])) {
            ZipEntry entrada = zip.entries().nextElement();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(zip.getInputStream(entrada), StandardCharsets.UTF_8), 1 << 20)) {
                String cabecera = br.readLine();  // "999994 300"
                System.out.println("Archivo " + entrada.getName() + ", cabecera: " + cabecera);
                String linea;
                while ((linea = br.readLine()) != null) {
                    lineas++;
                    int espacio = linea.indexOf(' ');
                    if (espacio <= 0) continue;
                    String palabra = linea.substring(0, espacio);
                    String valores = linea.substring(espacio + 1).trim();
                    if (vocabulario.contains(palabra)) {
                        exactas.putIfAbsent(palabra, valores);
                    } else {
                        String minuscula = palabra.toLowerCase();
                        if (vocabulario.contains(minuscula)) {
                            sinMayusculas.putIfAbsent(minuscula, valores);
                        }
                    }
                }
            }
        }

        // 3. Escribir en el orden del vocabulario: primero la forma exacta, si no la variante en mayusculas.
        List<String> faltantes = new ArrayList<>();
        int desdeMayusculas = 0;
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(args[1]), StandardCharsets.UTF_8))) {
            for (String palabra : vocabulario) {
                String valores = exactas.get(palabra);
                if (valores == null) {
                    valores = sinMayusculas.get(palabra);
                    if (valores != null) desdeMayusculas++;
                }
                if (valores == null) {
                    faltantes.add(palabra);
                    continue;
                }
                if (valores.split(" ").length != 300) {
                    throw new IllegalStateException("Vector de dimension inesperada para " + palabra);
                }
                bw.write(palabra + "\t" + valores);
                bw.newLine();
            }
        }
        System.out.println("Lineas leidas: " + lineas);
        System.out.println("Palabras escritas: " + (vocabulario.size() - faltantes.size())
                + " (" + desdeMayusculas + " desde una variante con mayusculas)");
        System.out.println("Sin vector en FastText (" + faltantes.size() + "): " + faltantes);
    }
}
