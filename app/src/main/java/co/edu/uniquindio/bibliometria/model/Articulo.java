package co.edu.uniquindio.bibliometria.model;

import java.util.List;
import java.util.Objects;

/**
 * Representa un articulo cientifico del corpus (20 PDFs suministrados por el
 * docente). Solo se procesa computacionalmente el {@code abstract}; titulo y
 * autores son metadatos adicionales extraidos para trazabilidad y para
 * futuros requerimientos que crucen esta informacion.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class Articulo {

    /** Numero del articulo en el corpus (1 a 20), igual al prefijo del nombre del PDF. */
    private final int id;
    /** Nombre del archivo PDF original de donde se extrajo el articulo. */
    private final String archivo;
    /** Titulo del articulo (metadato). */
    private final String titulo;
    /** Autores del articulo separados por comas (metadato). */
    private final String autores;
    /** Texto completo del abstract tal como se extrajo del PDF, sin preprocesar. */
    private final String abstractOriginal;

    /** Tokens con stemming (usados por TF-IDF y Jaccard). Se calculan una sola vez y se cachean. */
    private List<String> tokens;

    /** Tokens SIN stemming (usados por los algoritmos de embeddings/IA). */
    private List<String> tokensEmbeddings;

    /**
     * Crea un articulo con sus metadatos y su abstract original. Los tokens
     * preprocesados no se calculan aqui: los asigna {@code RepositorioArticulos}
     * con {@link #setTokens(List)} y {@link #setTokensEmbeddings(List)}.
     *
     * @param id               numero del articulo en el corpus
     * @param archivo          nombre del PDF original
     * @param titulo           titulo del articulo
     * @param autores          autores del articulo
     * @param abstractOriginal texto del abstract sin preprocesar
     */
    public Articulo(int id, String archivo, String titulo, String autores, String abstractOriginal) {
        this.id = id;
        this.archivo = archivo;
        this.titulo = titulo;
        this.autores = autores;
        this.abstractOriginal = abstractOriginal;
    }

    /** @return numero del articulo en el corpus (1 a 20) */
    public int getId() {
        return id;
    }

    /** @return nombre del archivo PDF original */
    public String getArchivo() {
        return archivo;
    }

    /** @return titulo del articulo */
    public String getTitulo() {
        return titulo;
    }

    /** @return autores del articulo */
    public String getAutores() {
        return autores;
    }

    /** @return texto del abstract sin preprocesar */
    public String getAbstractOriginal() {
        return abstractOriginal;
    }

    /** @return tokens del abstract con stemming (para TF-IDF y Jaccard) */
    public List<String> getTokens() {
        return tokens;
    }

    /** Guarda los tokens con stemming ya calculados para no repetir el preprocesamiento. */
    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    /** @return tokens del abstract sin stemming (para los algoritmos con embeddings) */
    public List<String> getTokensEmbeddings() {
        return tokensEmbeddings;
    }

    /** Guarda los tokens sin stemming ya calculados para no repetir el preprocesamiento. */
    public void setTokensEmbeddings(List<String> tokensEmbeddings) {
        this.tokensEmbeddings = tokensEmbeddings;
    }

    /** Dos articulos son iguales si tienen el mismo id, sin importar el resto de sus datos. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Articulo)) return false;
        Articulo articulo = (Articulo) o;
        return id == articulo.id;
    }

    /** Codigo hash basado solo en el id, coherente con {@link #equals(Object)}. */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /** Representacion corta (id y titulo) para depuracion y mensajes en consola. */
    @Override
    public String toString() {
        return "Articulo{id=" + id + ", titulo='" + titulo + "'}";
    }
}
