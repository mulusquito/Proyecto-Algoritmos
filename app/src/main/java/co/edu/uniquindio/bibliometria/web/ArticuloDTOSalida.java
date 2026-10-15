package co.edu.uniquindio.bibliometria.web;

import co.edu.uniquindio.bibliometria.model.Articulo;

import java.util.LinkedHashMap;
import java.util.Map;

/** Representacion de un articulo expuesta por la API (evita serializar la lista de tokens internos).  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ArticuloDTOSalida {
    /** Numero del articulo en el corpus. */
    private final int id;
    /** Nombre del PDF original. */
    private final String archivo;
    /** Titulo del articulo. */
    private final String titulo;
    /** Autores del articulo. */
    private final String autores;
    /** Texto del abstract (se expone como "resumen" en el JSON). */
    private final String resumen;
    /** Cantidad de tokens del abstract despues del preprocesamiento con stemming. */
    private final int numeroTokens;

    /** Copia del articulo solo los datos que se envian al navegador. */
    public ArticuloDTOSalida(Articulo a) {
        this.id = a.getId();
        this.archivo = a.getArchivo();
        this.titulo = a.getTitulo();
        this.autores = a.getAutores();
        this.resumen = a.getAbstractOriginal();
        this.numeroTokens = a.getTokens() != null ? a.getTokens().size() : 0;
    }

    // Getters de solo lectura de cada campo.
    public int getId() { return id; }
    public String getArchivo() { return archivo; }
    public String getTitulo() { return titulo; }
    public String getAutores() { return autores; }
    public String getResumen() { return resumen; }
    public int getNumeroTokens() { return numeroTokens; }

    /** @return los datos del articulo como mapa clave -> valor, listo para convertir a JSON */
    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("archivo", archivo);
        m.put("titulo", titulo);
        m.put("autores", autores);
        m.put("resumen", resumen);
        m.put("numeroTokens", numeroTokens);
        return m;
    }
}
