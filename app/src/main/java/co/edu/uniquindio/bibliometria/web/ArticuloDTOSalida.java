package co.edu.uniquindio.bibliometria.web;

import co.edu.uniquindio.bibliometria.model.Articulo;

import java.util.LinkedHashMap;
import java.util.Map;

/** Representacion de un articulo expuesta por la API (evita serializar la lista de tokens internos).  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class ArticuloDTOSalida {
    private final int id;
    private final String archivo;
    private final String titulo;
    private final String autores;
    private final String resumen;
    private final int numeroTokens;

    public ArticuloDTOSalida(Articulo a) {
        this.id = a.getId();
        this.archivo = a.getArchivo();
        this.titulo = a.getTitulo();
        this.autores = a.getAutores();
        this.resumen = a.getAbstractOriginal();
        this.numeroTokens = a.getTokens() != null ? a.getTokens().size() : 0;
    }

    public int getId() { return id; }
    public String getArchivo() { return archivo; }
    public String getTitulo() { return titulo; }
    public String getAutores() { return autores; }
    public String getResumen() { return resumen; }
    public int getNumeroTokens() { return numeroTokens; }

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
