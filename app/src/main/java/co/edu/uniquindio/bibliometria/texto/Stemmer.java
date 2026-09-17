package co.edu.uniquindio.bibliometria.texto;

/**
 * Implementacion propia (desde cero) de una version simplificada del
 * algoritmo de stemming de Porter (Porter, 1980).
 * <p>
 * El objetivo del stemming es reducir palabras a una raiz comun
 * (por ejemplo "learning", "learns", "learned" -&gt; "learn") para que el
 * calculo de similitud (TF-IDF, Jaccard) no penalice a dos abstracts por
 * usar formas gramaticales distintas de la misma palabra.
 * <p>
 * No se utiliza ninguna libreria de stemming/NLP: todas las reglas de
 * medida silabica (m), deteccion de vocales y sufijos se implementan de
 * forma explicita con operaciones basicas sobre {@code char[]}.
 */
public final class Stemmer {

    private Stemmer() {
    }

    /** Aplica el algoritmo de stemming a una sola palabra en minusculas. */
    public static String stem(String palabraOriginal) {
        if (palabraOriginal == null || palabraOriginal.length() <= 2) {
            return palabraOriginal;
        }
        String palabra = palabraOriginal.toLowerCase();

        palabra = paso1a(palabra);
        palabra = paso1b(palabra);
        palabra = paso1c(palabra);
        palabra = paso2(palabra);
        palabra = paso3(palabra);
        palabra = paso4(palabra);
        palabra = paso5(palabra);
        return palabra;
    }

    // ---------------------------------------------------------------
    // Funciones auxiliares sobre la estructura consonante/vocal (C/V)
    // ---------------------------------------------------------------

    private static boolean esVocal(char[] c, int i) {
        char ch = c[i];
        if (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u') {
            return true;
        }
        if (ch == 'y') {
            // 'y' se considera vocal si la letra anterior es consonante
            return i == 0 || !esVocal(c, i - 1);
        }
        return false;
    }

    /**
     * Calcula la "medida" m de una palabra: el numero de transiciones
     * VC (vocal seguida de consonante) en la secuencia de consonantes y
     * vocales que forman la palabra. Es el criterio central del algoritmo
     * de Porter para decidir si una raiz es "suficientemente larga" como
     * para aplicarle una regla de sufijo.
     */
    private static int medida(String palabra) {
        char[] c = palabra.toCharArray();
        int m = 0;
        int i = 0;
        int n = c.length;
        // saltar consonantes iniciales
        while (i < n && !esVocal(c, i)) i++;
        boolean enVocales = i < n;
        i++;
        while (i < n) {
            if (esVocal(c, i)) {
                enVocales = true;
            } else if (enVocales) {
                m++;
                enVocales = false;
            }
            i++;
        }
        return m;
    }

    private static boolean contieneVocal(String palabra) {
        char[] c = palabra.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (esVocal(c, i)) return true;
        }
        return false;
    }

    private static boolean terminaEnConsonanteDoble(String p) {
        int n = p.length();
        if (n < 2) return false;
        char a = p.charAt(n - 1);
        char b = p.charAt(n - 2);
        return a == b && !esVocalChar(p, n - 1);
    }

    private static boolean esVocalChar(String p, int idx) {
        return esVocal(p.toCharArray(), idx);
    }

    /** Patron consonante-vocal-consonante final, donde la ultima consonante no es w, x, y. */
    private static boolean terminaEnCVC(String p) {
        int n = p.length();
        if (n < 3) return false;
        char[] c = p.toCharArray();
        boolean c1 = !esVocal(c, n - 3);
        boolean v = esVocal(c, n - 2);
        boolean c2 = !esVocal(c, n - 1);
        char ultima = c[n - 1];
        return c1 && v && c2 && ultima != 'w' && ultima != 'x' && ultima != 'y';
    }

    private static boolean terminaCon(String palabra, String sufijo) {
        return palabra.endsWith(sufijo);
    }

    private static String raiz(String palabra, String sufijo) {
        return palabra.substring(0, palabra.length() - sufijo.length());
    }

    // ---------------------------------------------------------------
    // Pasos del algoritmo (version simplificada de Porter, 1980)
    // ---------------------------------------------------------------

    private static String paso1a(String p) {
        if (p.endsWith("sses")) return raiz(p, "sses") + "ss";
        if (p.endsWith("ies")) return raiz(p, "ies") + "i";
        if (p.endsWith("ss")) return p;
        if (p.endsWith("s") && p.length() > 1) return raiz(p, "s");
        return p;
    }

    private static String paso1b(String p) {
        if (p.endsWith("eed")) {
            String r = raiz(p, "eed");
            if (medida(r) > 0) return r + "ee";
            return p;
        }
        boolean aplicado = false;
        String r = null;
        if (p.endsWith("ed")) {
            String tmp = raiz(p, "ed");
            if (contieneVocal(tmp)) {
                r = tmp;
                aplicado = true;
            }
        }
        if (!aplicado && p.endsWith("ing")) {
            String tmp = raiz(p, "ing");
            if (contieneVocal(tmp)) {
                r = tmp;
                aplicado = true;
            }
        }
        if (!aplicado) {
            return p;
        }
        // post-procesamiento tras remover ed/ing
        if (r.endsWith("at") || r.endsWith("bl") || r.endsWith("iz")) {
            return r + "e";
        }
        if (terminaEnConsonanteDoble(r) && !r.endsWith("l") && !r.endsWith("s") && !r.endsWith("z")) {
            return r.substring(0, r.length() - 1);
        }
        if (medida(r) == 1 && terminaEnCVC(r)) {
            return r + "e";
        }
        return r;
    }

    private static String paso1c(String p) {
        if (p.endsWith("y") && p.length() > 1) {
            String tmp = p.substring(0, p.length() - 1);
            if (contieneVocal(tmp)) {
                return tmp + "i";
            }
        }
        return p;
    }

    private static final String[][] SUFIJOS_PASO2 = {
            {"ational", "ate"}, {"tional", "tion"}, {"enci", "ence"}, {"anci", "ance"},
            {"izer", "ize"}, {"abli", "able"}, {"alli", "al"}, {"entli", "ent"},
            {"eli", "e"}, {"ousli", "ous"}, {"ization", "ize"}, {"ation", "ate"},
            {"ator", "ate"}, {"alism", "al"}, {"iveness", "ive"}, {"fulness", "ful"},
            {"ousness", "ous"}, {"aliti", "al"}, {"iviti", "ive"}, {"biliti", "ble"}
    };

    private static String paso2(String p) {
        for (String[] regla : SUFIJOS_PASO2) {
            String sufijo = regla[0];
            String reemplazo = regla[1];
            if (p.endsWith(sufijo)) {
                String r = raiz(p, sufijo);
                if (medida(r) > 0) {
                    return r + reemplazo;
                }
                return p;
            }
        }
        return p;
    }

    private static final String[][] SUFIJOS_PASO3 = {
            {"icate", "ic"}, {"ative", ""}, {"alize", "al"}, {"iciti", "ic"},
            {"ical", "ic"}, {"ful", ""}, {"ness", ""}
    };

    private static String paso3(String p) {
        for (String[] regla : SUFIJOS_PASO3) {
            String sufijo = regla[0];
            String reemplazo = regla[1];
            if (p.endsWith(sufijo)) {
                String r = raiz(p, sufijo);
                if (medida(r) > 0) {
                    return r + reemplazo;
                }
                return p;
            }
        }
        return p;
    }

    private static final String[] SUFIJOS_PASO4 = {
            "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment",
            "ent", "ou", "ism", "ate", "iti", "ous", "ive", "ize"
    };

    private static String paso4(String p) {
        for (String sufijo : SUFIJOS_PASO4) {
            if (p.endsWith(sufijo)) {
                String r = raiz(p, sufijo);
                if (medida(r) > 1) {
                    return r;
                }
                return p;
            }
        }
        if (p.endsWith("sion") || p.endsWith("tion")) {
            String r = p.substring(0, p.length() - 3);
            if (medida(r) > 1) {
                return r;
            }
        }
        return p;
    }

    private static String paso5(String p) {
        if (p.endsWith("e")) {
            String r = raiz(p, "e");
            int m = medida(r);
            if (m > 1 || (m == 1 && !terminaEnCVC(r))) {
                p = r;
            }
        }
        if (p.endsWith("ll") && medida(p) > 1) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
