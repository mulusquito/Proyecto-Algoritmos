package co.edu.uniquindio.bibliometria.texto;

import java.util.ArrayList;
import java.util.List;

/**
 * Preprocesamiento de texto implementado desde cero (sin librerias de NLP)
 * sobre los abstracts de los articulos. Encapsula las etapas descritas en el
 * documento tecnico:
 * <ol>
 *     <li>Normalizacion (minusculas, remocion de acentos y puntuacion)</li>
 *     <li>Tokenizacion (segmentacion en palabras)</li>
 *     <li>Eliminacion de stopwords</li>
 *     <li>Stemming (reduccion a raiz gramatical)</li>
 * </ol>
 * <p>
 * Se exponen dos variantes: {@link #procesarParaVectorial(String)}
 * (tokens normalizados, sin stopwords y con stemming, usados por TF-IDF,
 * Jaccard y los algoritmos de IA basados en embeddings) y
 * {@link #normalizarParaEdicion(String)} (texto normalizado a nivel de
 * caracter, usado por Levenshtein y Needleman-Wunsch, que comparan cadenas
 * completas y no bolsas de palabras).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public final class Preprocesador {

    /** Constructor privado: la clase solo tiene metodos estaticos y no se instancia. */
    private Preprocesador() {
    }

    /**
     * Normaliza una cadena para los algoritmos de distancia de edicion:
     * minusculas, remocion de acentos, colapso de espacios y eliminacion de
     * signos de puntuacion (se conservan letras, digitos y espacios).
     */
    public static String normalizarParaEdicion(String texto) {
        if (texto == null) return "";
        String sinAcentos = quitarAcentos(texto.toLowerCase());
        StringBuilder sb = new StringBuilder(sinAcentos.length());
        boolean ultimoEspacio = false;
        for (int i = 0; i < sinAcentos.length(); i++) {
            char c = sinAcentos.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                ultimoEspacio = false;
            } else if (!ultimoEspacio) {
                sb.append(' ');
                ultimoEspacio = true;
            }
        }
        return sb.toString().trim();
    }

    /** Segmenta un texto ya normalizado en tokens (palabras separadas por espacios). */
    public static List<String> tokenizar(String texto) {
        String normalizado = normalizarParaEdicion(texto);
        List<String> tokens = new ArrayList<>();
        for (String t : normalizado.split("\\s+")) {
            if (!t.isEmpty() && !esSoloDigitos(t)) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    /** @return true si el texto esta formado solo por digitos (por ejemplo "2023"); esos tokens se descartan */
    private static boolean esSoloDigitos(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    /** Elimina las palabras vacias (stopwords) de una lista de tokens. */
    public static List<String> eliminarStopwords(List<String> tokens) {
        List<String> resultado = new ArrayList<>();
        for (String t : tokens) {
            if (!ListaStopwords.STOPWORDS.contains(t) && t.length() > 1) {
                resultado.add(t);
            }
        }
        return resultado;
    }

    /** Aplica el algoritmo de stemming (ver {@link Stemmer}) a cada token. */
    public static List<String> aplicarStemming(List<String> tokens) {
        List<String> resultado = new ArrayList<>(tokens.size());
        for (String t : tokens) {
            resultado.add(Stemmer.stem(t));
        }
        return resultado;
    }

    /**
     * Pipeline completo usado por los algoritmos clasicos de tipo "bolsa de
     * palabras" (TF-IDF, Jaccard): tokenizar -&gt; quitar stopwords -&gt;
     * stemming. El stemming ayuda a estos algoritmos porque compara
     * palabras por coincidencia exacta de cadena, y queremos que
     * "learning"/"learned"/"learn" cuenten como el mismo termino.
     */
    public static List<String> procesarParaVectorial(String texto) {
        List<String> tokens = tokenizar(texto);
        tokens = eliminarStopwords(tokens);
        tokens = aplicarStemming(tokens);
        return tokens;
    }

    /**
     * Pipeline usado por los algoritmos de IA basados en embeddings:
     * tokenizar -&gt; quitar stopwords, <b>sin</b> aplicar stemming. A
     * diferencia de TF-IDF/Jaccard, aqui NO conviene stemizar: el modelo de
     * embeddings preentrenado (GloVe) tiene vectores para palabras reales
     * del idioma ("literacy", "assessment"), pero normalmente no tiene
     * entradas para raices truncadas artificiales como "literaci" o
     * "assess" que produce el stemmer. Aplicar stemming aqui reduciria
     * drasticamente cuantas palabras logran encontrar su vector.
     */
    public static List<String> procesarParaEmbeddings(String texto) {
        List<String> tokens = tokenizar(texto);
        return eliminarStopwords(tokens);
    }

    /** Quita diacriticos/acentos usando descomposicion Unicode (NFD) manual. */
    private static String quitarAcentos(String texto) {
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(normalizado.length());
        for (int i = 0; i < normalizado.length(); i++) {
            char c = normalizado.charAt(i);
            // Se descartan los caracteres del bloque de marcas diacriticas combinantes.
            if (c < 0x0300 || c > 0x036F) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
