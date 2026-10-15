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
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public final class JsonParser {

    /** Texto JSON completo que se esta leyendo. */
    private final String texto;
    /** Posicion del siguiente caracter por leer. */
    private int pos;

    /** Crea un parser posicionado al inicio del texto (se usa solo desde {@link #parsear}). */
    private JsonParser(String texto) {
        this.texto = texto;
        this.pos = 0;
    }

    /**
     * Convierte un texto JSON en estructuras de Java: los objetos se vuelven
     * {@code Map<String, Object>}, los arreglos {@code List<Object>}, las
     * cadenas {@code String}, los numeros {@code Integer}, {@code Long} o
     * {@code Double}, y true/false/null sus equivalentes.
     *
     * @param texto JSON a leer
     * @return el valor raiz del JSON
     * @throws IllegalArgumentException si el JSON esta mal formado
     */
    public static Object parsear(String texto) {
        JsonParser parser = new JsonParser(texto);
        parser.saltarEspacios();
        Object valor = parser.parsearValor();
        parser.saltarEspacios();
        return valor;
    }

    /**
     * Lee un valor JSON cualquiera. El primer caracter decide el tipo:
     * "{" objeto, "[" arreglo, comillas cadena, t/f booleano, n null y
     * cualquier otro se trata como numero.
     */
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

    /**
     * Lee un objeto {@code {"clave": valor, ...}}. Repite "clave : valor"
     * hasta encontrar la llave de cierre. Usa {@link LinkedHashMap} para
     * conservar el orden de las claves.
     */
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

    /** Lee un arreglo {@code [valor, valor, ...]} hasta encontrar el corchete de cierre. */
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

    /**
     * Lee una cadena entre comillas, traduciendo las secuencias de escape
     * (salto de linea, comillas, barra, codigos unicode de 4 digitos
     * hexadecimales, etc.) al caracter que representan.
     */
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

    /**
     * Lee un numero. Si tiene punto decimal o exponente devuelve un
     * {@code Double}; si no, un {@code Integer} (o {@code Long} si no cabe).
     */
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

    /** Avanza sobre una palabra fija (true, false, null) o lanza error si no esta. */
    private void esperar(String palabra) {
        if (!texto.startsWith(palabra, pos)) {
            throw new IllegalArgumentException("Se esperaba '" + palabra + "' en posicion " + pos);
        }
        pos += palabra.length();
    }

    /** Avanza un caracter si es el esperado; si no, lanza error con la posicion. */
    private void consumir(char esperado) {
        if (actual() != esperado) {
            throw new IllegalArgumentException("Se esperaba '" + esperado + "' en posicion " + pos);
        }
        pos++;
    }

    /** @return el caracter en la posicion actual, sin avanzar */
    private char actual() {
        return texto.charAt(pos);
    }

    /** Avanza sobre espacios, tabulaciones y saltos de linea. */
    private void saltarEspacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }
}
