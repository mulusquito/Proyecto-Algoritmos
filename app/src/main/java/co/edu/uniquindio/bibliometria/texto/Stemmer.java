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
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public final class Stemmer {

    /** Constructor privado: la clase solo tiene metodos estaticos y no se instancia. */
    private Stemmer() {
    }

    /**
     * Aplica el algoritmo de stemming a una sola palabra en minusculas.
     *
     * @param palabraOriginal palabra a reducir
     * @return la raiz de la palabra (las palabras de 1 o 2 letras se devuelven igual)
     */
    public static String stem(String palabraOriginal) {
        if (palabraOriginal == null || palabraOriginal.length() <= 2) {
            return palabraOriginal;
        }
        String palabra = palabraOriginal.toLowerCase();

        // Los pasos se aplican en orden; cada uno recibe la salida del anterior.
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

    /**
     * Indica si la letra en la posicion i es vocal segun Porter: a, e, i, o, u
     * siempre, e "y" solo cuando va despues de una consonante (en "toy" la y
     * es consonante; en "happy" es vocal).
     */
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
        // Cada vez que una racha de vocales termina en una consonante se cuenta un VC.
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

    /** @return true si la palabra tiene al menos una vocal */
    private static boolean contieneVocal(String palabra) {
        char[] c = palabra.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (esVocal(c, i)) return true;
        }
        return false;
    }

    /** @return true si la palabra termina en dos consonantes iguales (por ejemplo "pp" en "hopp") */
    private static boolean terminaEnConsonanteDoble(String p) {
        int n = p.length();
        if (n < 2) return false;
        char a = p.charAt(n - 1);
        char b = p.charAt(n - 2);
        return a == b && !esVocalChar(p, n - 1);
    }

    /** Version de {@link #esVocal(char[], int)} que recibe un String. */
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

    /** @return true si la palabra termina con el sufijo dado */
    private static boolean terminaCon(String palabra, String sufijo) {
        return palabra.endsWith(sufijo);
    }

    /** @return la palabra sin el sufijo dado (se asume que termina en el) */
    private static String raiz(String palabra, String sufijo) {
        return palabra.substring(0, palabra.length() - sufijo.length());
    }

    // ---------------------------------------------------------------
    // Pasos del algoritmo (version simplificada de Porter, 1980)
    // ---------------------------------------------------------------

    /**
     * Paso 1a: plurales. "sses" -&gt; "ss", "ies" -&gt; "i", "ss" se deja igual y
     * una "s" final se quita (por ejemplo "studies" -&gt; "studi", "models" -&gt; "model").
     */
    private static String paso1a(String p) {
        if (p.endsWith("sses")) return raiz(p, "sses") + "ss";
        if (p.endsWith("ies")) return raiz(p, "ies") + "i";
        if (p.endsWith("ss")) return p;
        if (p.endsWith("s") && p.length() > 1) return raiz(p, "s");
        return p;
    }

    /**
     * Paso 1b: tiempos verbales. Quita "eed" (si m &gt; 0), "ed" o "ing" (si
     * lo que queda tiene vocal) y luego repara la raiz: agrega "e" en
     * at/bl/iz ("generated" -&gt; "generate"), quita una consonante doble
     * ("hopping" -&gt; "hop") o agrega "e" a raices cortas CVC.
     */
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

    /** Paso 1c: una "y" final se cambia por "i" si la raiz tiene vocal ("happy" -&gt; "happi"). */
    private static String paso1c(String p) {
        if (p.endsWith("y") && p.length() > 1) {
            String tmp = p.substring(0, p.length() - 1);
            if (contieneVocal(tmp)) {
                return tmp + "i";
            }
        }
        return p;
    }

    /** Reglas del paso 2 como pares {sufijo, reemplazo}. */
    private static final String[][] SUFIJOS_PASO2 = {
            {"ational", "ate"}, {"tional", "tion"}, {"enci", "ence"}, {"anci", "ance"},
            {"izer", "ize"}, {"abli", "able"}, {"alli", "al"}, {"entli", "ent"},
            {"eli", "e"}, {"ousli", "ous"}, {"ization", "ize"}, {"ation", "ate"},
            {"ator", "ate"}, {"alism", "al"}, {"iveness", "ive"}, {"fulness", "ful"},
            {"ousness", "ous"}, {"aliti", "al"}, {"iviti", "ive"}, {"biliti", "ble"}
    };

    /**
     * Paso 2: reduce sufijos dobles a uno simple ("relational" -&gt; "relate",
     * "organization" -&gt; "organize") si la raiz tiene m &gt; 0. Solo se aplica
     * la primera regla cuyo sufijo coincida.
     */
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

    /** Reglas del paso 3 como pares {sufijo, reemplazo}. */
    private static final String[][] SUFIJOS_PASO3 = {
            {"icate", "ic"}, {"ative", ""}, {"alize", "al"}, {"iciti", "ic"},
            {"ical", "ic"}, {"ful", ""}, {"ness", ""}
    };

    /** Paso 3: sufijos como "-ical", "-ful", "-ness" ("practical" -&gt; "practic") si la raiz tiene m &gt; 0. */
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

    /** Sufijos que el paso 4 elimina por completo. */
    private static final String[] SUFIJOS_PASO4 = {
            "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment",
            "ent", "ou", "ism", "ate", "iti", "ous", "ive", "ize"
    };

    /**
     * Paso 4: elimina sufijos como "-ment", "-ence", "-ive" y "-ion" (despues
     * de s/t) solo si la raiz que queda es larga (m &gt; 1), para no destruir
     * palabras cortas.
     */
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

    /**
     * Paso 5: limpieza final. Quita una "e" final si la raiz es larga y
     * reduce "ll" a "l" en raices largas ("controll" -&gt; "control").
     */
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
