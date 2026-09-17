package co.edu.uniquindio.bibliometria.model;

import java.util.List;
import java.util.Objects;

/**
 * Representa un articulo cientifico del corpus (20 PDFs suministrados por el
 * docente). Solo se procesa computacionalmente el {@code abstract}; titulo y
 * autores son metadatos adicionales extraidos para trazabilidad y para
 * futuros requerimientos que crucen esta informacion.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public class Articulo {

    private final int id;
    private final String archivo;
    private final String titulo;
    private final String autores;
    private final String abstractOriginal;

    /** Tokens con stemming (usados por TF-IDF y Jaccard). Se calculan una sola vez y se cachean. */
    private List<String> tokens;

    /** Tokens SIN stemming (usados por los algoritmos de embeddings/IA). */
    private List<String> tokensEmbeddings;

    public Articulo(int id, String archivo, String titulo, String autores, String abstractOriginal) {
        this.id = id;
        this.archivo = archivo;
        this.titulo = titulo;
        this.autores = autores;
        this.abstractOriginal = abstractOriginal;
    }

    public int getId() {
        return id;
    }

    public String getArchivo() {
        return archivo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutores() {
        return autores;
    }

    public String getAbstractOriginal() {
        return abstractOriginal;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public List<String> getTokensEmbeddings() {
        return tokensEmbeddings;
    }

    public void setTokensEmbeddings(List<String> tokensEmbeddings) {
        this.tokensEmbeddings = tokensEmbeddings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Articulo)) return false;
        Articulo articulo = (Articulo) o;
        return id == articulo.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Articulo{id=" + id + ", titulo='" + titulo + "'}";
    }
}
