package co.edu.uniquindio.bibliometria.clustering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nodo de un dendrograma (arbol binario de agrupamiento jerarquico).
 * Un nodo hoja representa un unico articulo del corpus (altura 0); un nodo
 * interno representa la fusion de dos clusteres a una determinada
 * "altura" (la distancia a la que se fusionaron, segun el criterio de
 * enlace usado).
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public class NodoDendrograma {

    private final int id;
    private final boolean esHoja;
    private final Integer articuloId; // solo para hojas
    private final String etiqueta;
    private final double altura;
    private final NodoDendrograma izquierdo;
    private final NodoDendrograma derecho;
    private final List<Integer> miembros = new ArrayList<>(); // ids de articulos contenidos en el subarbol

    /** Constructor para nodo hoja. */
    public NodoDendrograma(int id, int articuloId, String etiqueta) {
        this.id = id;
        this.esHoja = true;
        this.articuloId = articuloId;
        this.etiqueta = etiqueta;
        this.altura = 0.0;
        this.izquierdo = null;
        this.derecho = null;
        this.miembros.add(articuloId);
    }

    /** Constructor para nodo interno (fusion de dos clusteres). */
    public NodoDendrograma(int id, double altura, NodoDendrograma izquierdo, NodoDendrograma derecho) {
        this.id = id;
        this.esHoja = false;
        this.articuloId = null;
        this.etiqueta = null;
        this.altura = altura;
        this.izquierdo = izquierdo;
        this.derecho = derecho;
        this.miembros.addAll(izquierdo.miembros);
        this.miembros.addAll(derecho.miembros);
    }

    public int getId() {
        return id;
    }

    public boolean isEsHoja() {
        return esHoja;
    }

    public Integer getArticuloId() {
        return articuloId;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public double getAltura() {
        return altura;
    }

    public NodoDendrograma getIzquierdo() {
        return izquierdo;
    }

    public NodoDendrograma getDerecho() {
        return derecho;
    }

    public List<Integer> getMiembros() {
        return miembros;
    }

    public int tamano() {
        return miembros.size();
    }

    /** Representacion recursiva en Map/List, lista para serializar a JSON. */
    public Map<String, Object> aMapa() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("esHoja", esHoja);
        m.put("articuloId", articuloId);
        m.put("etiqueta", etiqueta);
        m.put("altura", altura);
        m.put("miembros", miembros);
        m.put("izquierdo", izquierdo == null ? null : izquierdo.aMapa());
        m.put("derecho", derecho == null ? null : derecho.aMapa());
        return m;
    }
}
