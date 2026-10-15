package co.edu.uniquindio.bibliometria.ordenamiento;

import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye la lista de frecuencias de terminos del corpus completo (los 20
 * abstracts), que es la entrada de los tres algoritmos de ordenamiento.
 * <p>
 * Se usa el pipeline {@link Preprocesador#procesarParaEmbeddings(String)}
 * (normalizacion, tokenizacion y eliminacion de stopwords, <b>sin</b>
 * stemming) para que el ranking muestre palabras reales del idioma
 * ("learning", "education") y no raices truncadas ("learn", "educ").
 * <p>
 * El conteo usa un {@link LinkedHashMap}: cada termino queda en el orden de
 * su <b>primera aparicion</b> en el corpus (articulo 1, 2, ...). Ese orden
 * es la entrada de los algoritmos, y como los tres son estables, los
 * empates de frecuencia se resuelven siempre igual: primero el termino que
 * aparecio antes en el corpus.
  *
 * @author Melissa Restrepo Hurtado - Universidad del Quindio
*/
public final class ContadorFrecuencias {

    private ContadorFrecuencias() {
    }

    public static TerminoFrecuencia[] contar(List<Articulo> articulos) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (Articulo articulo : articulos) {
            for (String token : Preprocesador.procesarParaEmbeddings(articulo.getAbstractOriginal())) {
                conteo.merge(token, 1, Integer::sum);
            }
        }

        TerminoFrecuencia[] terminos = new TerminoFrecuencia[conteo.size()];
        int i = 0;
        for (Map.Entry<String, Integer> entrada : conteo.entrySet()) {
            terminos[i++] = new TerminoFrecuencia(entrada.getKey(), entrada.getValue());
        }
        return terminos;
    }
}
