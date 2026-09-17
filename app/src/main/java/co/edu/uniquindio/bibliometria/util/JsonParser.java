package co.edu.uniquindio.bibliometria.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser de JSON minimalista, escrito desde cero, usado exclusivamente para
 * leer el archivo de datos {@code data/articulos.json} y los parametros de
 * las peticiones HTTP entrantes.
 * <p>
 * No se trata de ninguno de los algoritmos exigidos por el proyecto (no es
 * un algoritmo de similitud ni de clustering): es infraestructura de
 * lectura de datos que se implemento a mano porque el entorno de
 * desarrollo no tenia acceso a un repositorio de dependencias (Maven
 * Central) para traer una libreria como Jackson o Gson. Soporta el
 * subconjunto de JSON necesario para este proyecto: objetos, arreglos,
 * cadenas (con escapes basicos), numeros, booleanos y null.
 */
public final class JsonParser {

    private final String texto;
    private int pos;

    private JsonParser(String texto) {
        this.texto = texto;
        this.pos = 0;
    }

    public static Object parsear(String texto) {
        JsonParser parser = new JsonParser(texto);
        parser.saltarEspacios();
        Object valor = parser.parsearValor();
        parser.saltarEspacios();
        return valor;
    }

    private Object parsearValor() {
        char c = actual();
        switch (c) {
            case '{': return parsearObjeto();
            case '[': return parsearArreglo();
            case '"': return parsearCadena();
            case 't':
                esperar("true");
                return Boolean.TRUE;
            case 'f':
                esperar("false");
                return Boolean.FALSE;
            case 'n':
                esperar("null");
                return null;
            default:
                return parsearNumero();
        }
    }

    private Map<String, Object> parsearObjeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        consumir('{');
        saltarEspacios();
        if (actual() == '}') {
            pos++;
            return mapa;
        }
        while (true) {
            saltarEspacios();
            String clave = parsearCadena();
            saltarEspacios();
            consumir(':');
            saltarEspacios();
            Object valor = parsearValor();
            mapa.put(clave, valor);
            saltarEspacios();
            char c = actual();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                break;
            } else {
                throw new IllegalArgumentException("JSON invalido en posicion " + pos);
            }
        }
        return mapa;
    }

    private List<Object> parsearArreglo() {
        List<Object> lista = new ArrayList<>();
        consumir('[');
        saltarEspacios();
        if (actual() == ']') {
            pos++;
            return lista;
        }
        while (true) {
            saltarEspacios();
            lista.add(parsearValor());
            saltarEspacios();
            char c = actual();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                break;
            } else {
                throw new IllegalArgumentException("JSON invalido en posicion " + pos);
            }
        }
        return lista;
    }

    private String parsearCadena() {
        consumir('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = texto.charAt(pos++);
            if (c == '"') break;
            if (c == '\\') {
                char siguiente = texto.charAt(pos++);
                switch (siguiente) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        String hex = texto.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                        break;
                    default: sb.append(siguiente);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Object parsearNumero() {
        int inicio = pos;
        if (actual() == '-') pos++;
        while (pos < texto.length() && (Character.isDigit(texto.charAt(pos)) || texto.charAt(pos) == '.'
                || texto.charAt(pos) == 'e' || texto.charAt(pos) == 'E' || texto.charAt(pos) == '+'
                || texto.charAt(pos) == '-')) {
            pos++;
        }
        String numero = texto.substring(inicio, pos);
        if (numero.contains(".") || numero.contains("e") || numero.contains("E")) {
            return Double.parseDouble(numero);
        }
        try {
            return Integer.parseInt(numero);
        } catch (NumberFormatException e) {
            return Long.parseLong(numero);
        }
    }

    private void esperar(String palabra) {
        if (!texto.startsWith(palabra, pos)) {
            throw new IllegalArgumentException("Se esperaba '" + palabra + "' en posicion " + pos);
        }
        pos += palabra.length();
    }

    private void consumir(char esperado) {
        if (actual() != esperado) {
            throw new IllegalArgumentException("Se esperaba '" + esperado + "' en posicion " + pos);
        }
        pos++;
    }

    private char actual() {
        return texto.charAt(pos);
    }

    private void saltarEspacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }
}
