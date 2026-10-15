package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.similitud.clasico.LevenshteinSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.NeedlemanWunschSimilitud;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.List;

/**
 * Utilidad de una sola ejecucion para generar, en formato tabla Markdown,
 * el ejemplo ilustrativo de las matrices de programacion dinamica que se
 * incluye en el documento tecnico (docs/DOCUMENTO_TECNICO.md). No forma
 * parte de la aplicacion en si.
 */
public class GenerarEjemploDocumentacion {
    /**
     * Imprime en consola la matriz de Levenshtein para "kitten" vs "sitting"
     * y la matriz de Needleman-Wunsch para dos frases cortas, listas para
     * copiar al documento tecnico.
     */
    public static void main(String[] args) {
        String a = "AI literacy adult education";
        String b = "AI literacy higher education";

        // Ejemplo classico y compacto para ilustrar el llenado de la matriz caracter a caracter.
        String ejemploLevA = "kitten";
        String ejemploLevB = "sitting";
        System.out.println("### Levenshtein: \"" + ejemploLevA + "\" vs \"" + ejemploLevB + "\" (ejemplo clasico ilustrativo)\n");
        LevenshteinSimilitud lev = new LevenshteinSimilitud();
        String an = Preprocesador.normalizarParaEdicion(ejemploLevA);
        String bn = Preprocesador.normalizarParaEdicion(ejemploLevB);
        System.out.println("Normalizado A: `" + an + "`  (longitud " + an.length() + ")");
        System.out.println("Normalizado B: `" + bn + "`  (longitud " + bn.length() + ")\n");
        int[][] m = lev.construirMatriz(an, bn);
        imprimirMatrizMarkdown(m, an, bn);
        var resultado = lev.calcular(ejemploLevA, ejemploLevB);
        System.out.println("\nDistancia final = " + resultado.getDetalle().get("distanciaAbsoluta"));
        System.out.println("Similitud = 1 - (" + resultado.getDetalle().get("distanciaAbsoluta") + " / max(" +
                an.length() + "," + bn.length() + ")) = " + String.format("%.4f", resultado.getSimilitud()));

        System.out.println("\n\n### Needleman-Wunsch (a nivel de tokens preprocesados)\n");
        NeedlemanWunschSimilitud nw = new NeedlemanWunschSimilitud();
        List<String> ta = Preprocesador.procesarParaVectorial(a);
        List<String> tb = Preprocesador.procesarParaVectorial(b);
        System.out.println("Tokens A: " + ta);
        System.out.println("Tokens B: " + tb + "\n");
        int[][] f = nw.construirMatriz(ta, tb);
        imprimirMatrizMarkdownTokens(f, ta, tb);
        var resultadoNw = nw.calcular(a, b);
        System.out.println("\nPuntaje final F[n][m] = " + resultadoNw.getDetalle().get("puntajeAlineamiento"));
        System.out.println("Similitud normalizada = " + String.format("%.4f", resultadoNw.getSimilitud()));
    }

    /**
     * Imprime una matriz de Levenshtein como tabla Markdown: columnas = letras
     * de b, filas = letras de a, y el simbolo vacio para la fila/columna 0.
     */
    private static void imprimirMatrizMarkdown(int[][] m, String a, String b) {
        StringBuilder encabezado = new StringBuilder("| |");
        encabezado.append(" ∅ |");
        for (int j = 0; j < b.length(); j++) encabezado.append(" ").append(b.charAt(j)).append(" |");
        System.out.println(encabezado);

        StringBuilder separador = new StringBuilder("|---|---|");
        for (int j = 0; j < b.length(); j++) separador.append("---|");
        System.out.println(separador);

        for (int i = 0; i < m.length; i++) {
            StringBuilder fila = new StringBuilder("| ");
            fila.append(i == 0 ? "∅" : a.charAt(i - 1)).append(" |");
            for (int j = 0; j < m[i].length; j++) {
                fila.append(" ").append(m[i][j]).append(" |");
            }
            System.out.println(fila);
        }
    }

    /** Igual que {@link #imprimirMatrizMarkdown}, pero con palabras (tokens) en lugar de letras. */
    private static void imprimirMatrizMarkdownTokens(int[][] f, List<String> a, List<String> b) {
        StringBuilder encabezado = new StringBuilder("| |");
        encabezado.append(" ∅ |");
        for (String t : b) encabezado.append(" ").append(t).append(" |");
        System.out.println(encabezado);

        StringBuilder separador = new StringBuilder("|---|---|");
        for (int j = 0; j < b.size(); j++) separador.append("---|");
        System.out.println(separador);

        for (int i = 0; i < f.length; i++) {
            StringBuilder fila = new StringBuilder("| ");
            fila.append(i == 0 ? "∅" : a.get(i - 1)).append(" |");
            for (int j = 0; j < f[i].length; j++) {
                fila.append(" ").append(f[i][j]).append(" |");
            }
            System.out.println(fila);
        }
    }
}
