package co.edu.uniquindio.bibliometria.texto;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Lista estatica de "stopwords" (palabras vacias) del ingles, idioma en el
 * que estan escritos los 20 abstracts del corpus. Se define manualmente
 * (sin depender de una libreria de NLP externa) para cumplir con la
 * restriccion del proyecto de no usar funciones de alto nivel que
 * reemplacen el trabajo algoritmico que se pide implementar.
 */
public final class ListaStopwords {

    public static final Set<String> STOPWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any",
            "are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below",
            "between", "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't",
            "do", "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for",
            "from", "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he",
            "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself",
            "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
            "isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
            "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought",
            "our", "ours", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd",
            "she'll", "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's",
            "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these",
            "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to",
            "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're",
            "we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's",
            "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would",
            "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself",
            "yourselves",
            // conectores academicos muy frecuentes en abstracts que no aportan significado
            // semantico al comparar textos (ruido para similitud / clustering)
            "also", "however", "therefore", "thus", "furthermore", "moreover", "e", "g", "i.e",
            "e.g", "et", "al", "study", "paper", "article", "review", "research"
    )));

    private ListaStopwords() {
    }
}
