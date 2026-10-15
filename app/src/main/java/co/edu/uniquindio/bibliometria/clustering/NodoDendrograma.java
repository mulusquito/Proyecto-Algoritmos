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

    /** Identificador del nodo (las hojas usan 0..n-1 y los nodos internos n, n+1, ...). */
    private final int id;
    /** true si el nodo representa un solo articulo; false si es una fusion. */
    private final boolean esHoja;
    private final Integer articuloId; // solo para hojas
    /** Texto que se muestra debajo de la hoja en el dendrograma (solo hojas). */
    private final String etiqueta;
    /** Distancia a la que se fusionaron los dos hijos (0 para las hojas). */
    private final double altura;
    /** Subarbol izquierdo (null en las hojas). */
    private final NodoDendrograma izquierdo;
    /** Subarbol derecho (null en las hojas). */
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

    /** @return identificador del nodo */
    public int getId() {
        return id;
    }

    /** @return true si el nodo es una hoja (un solo articulo) */
    public boolean isEsHoja() {
        return esHoja;
    }

    /** @return id del articulo de la hoja, o null si es un nodo interno */
    public Integer getArticuloId() {
        return articuloId;
    }

    /** @return etiqueta de la hoja, o null si es un nodo interno */
    public String getEtiqueta() {
        return etiqueta;
    }

    /** @return distancia a la que se fusionaron los hijos (0 en las hojas) */
    public double getAltura() {
        return altura;
    }

    /** @return subarbol izquierdo, o null si es una hoja */
    public NodoDendrograma getIzquierdo() {
        return izquierdo;
    }

    /** @return subarbol derecho, o null si es una hoja */
    public NodoDendrograma getDerecho() {
        return derecho;
    }

    /** @return ids de todos los articulos que cuelgan de este nodo */
    public List<Integer> getMiembros() {
        return miembros;
    }

    /** @return numero de articulos que agrupa este nodo */
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
