package co.edu.uniquindio.bibliometria.util;

import java.util.Collection;
import java.util.Map;

/**
 * Serializador de JSON minimalista, escrito desde cero, complemento de
 * {@link JsonParser}. Convierte estructuras Java genericas (Map, List,
 * String, Number, Boolean, null) en texto JSON valido. Las clases de
 * resultado de la aplicacion (similitud, clustering, articulos) exponen un
 * metodo {@code aMapa()} que produce esa estructura generica, evitando
 * depender de anotaciones/reflexion de una libreria externa.
  *
 * @author Melissa Restrepo Henao - Universidad del Quindio
*/
public final class JsonEscritor {

    private JsonEscritor() {
    }

    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribirValor(valor, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void escribirValor(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String) {
            escribirCadena((String) valor, sb);
        } else if (valor instanceof Map) {
            escribirObjeto((Map<String, Object>) valor, sb);
        } else if (valor instanceof Collection) {
            escribirArreglo((Collection<Object>) valor, sb);
        } else if (valor instanceof Double || valor instanceof Float) {
            double d = ((Number) valor).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else {
                sb.append(d);
            }
        } else if (valor instanceof Number) {
            sb.append(valor.toString());
        } else if (valor instanceof Boolean) {
            sb.append(valor.toString());
        } else if (valor instanceof int[]) {
            escribirArregloInt((int[]) valor, sb);
        } else if (valor instanceof int[][]) {
            escribirMatrizInt((int[][]) valor, sb);
        } else if (valor instanceof double[][]) {
            escribirMatrizDouble((double[][]) valor, sb);
        } else if (valor.getClass().isArray()) {
            // Fallback generico para otros arreglos de objetos.
            Object[] arreglo = (Object[]) valor;
            sb.append('[');
            for (int i = 0; i < arreglo.length; i++) {
                if (i > 0) sb.append(',');
                escribirValor(arreglo[i], sb);
            }
            sb.append(']');
        } else {
            // Ultimo recurso: representar como cadena.
            escribirCadena(valor.toString(), sb);
        }
    }

    private static void escribirObjeto(Map<String, Object> mapa, StringBuilder sb) {
        sb.append('{');
        boolean primero = true;
        for (Map.Entry<String, Object> entrada : mapa.entrySet()) {
            if (!primero) sb.append(',');
            primero = false;
            escribirCadena(entrada.getKey(), sb);
            sb.append(':');
            escribirValor(entrada.getValue(), sb);
        }
        sb.append('}');
    }

    private static void escribirArreglo(Collection<Object> lista, StringBuilder sb) {
        sb.append('[');
        boolean primero = true;
        for (Object elemento : lista) {
            if (!primero) sb.append(',');
            primero = false;
            escribirValor(elemento, sb);
        }
        sb.append(']');
    }

    private static void escribirArregloInt(int[] arreglo, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < arreglo.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arreglo[i]);
        }
        sb.append(']');
    }

    private static void escribirMatrizInt(int[][] matriz, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < matriz.length; i++) {
            if (i > 0) sb.append(',');
            escribirArregloInt(matriz[i], sb);
        }
        sb.append(']');
    }

    private static void escribirMatrizDouble(double[][] matriz, StringBuilder sb) {
        sb.append('[');
        for (int i = 0; i < matriz.length; i++) {
            if (i > 0) sb.append(',');
            sb.append('[');
            for (int j = 0; j < matriz[i].length; j++) {
                if (j > 0) sb.append(',');
                sb.append(matriz[i][j]);
            }
            sb.append(']');
        }
        sb.append(']');
    }

    private static void escribirCadena(String texto, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
