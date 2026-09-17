package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.clustering.MotorClustering;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;
import co.edu.uniquindio.bibliometria.similitud.ia.RepositorioEmbeddings;
import co.edu.uniquindio.bibliometria.web.ServidorHttp;

/**
 * Punto de entrada de la aplicacion.
 * <p>
 * Proyecto: "Analisis de algoritmos en el contexto de la bibliometria"
 * Universidad del Quindio - Programa de Ingenieria de Sistemas y Computacion.
 * <p>
 * La aplicacion es una aplicacion Java estandar (sin framework de
 * inyeccion de dependencias): este metodo construye a mano el pequeno
 * grafo de objetos de la aplicacion (repositorios -&gt; motores -&gt;
 * servidor HTTP), lo cual es perfectamente manejable dado el tamano del
 * proyecto y evita depender de Spring Boot (ver {@link ServidorHttp} para
 * la justificacion completa).
 */
public class BibliometriaApplication {

    public static void main(String[] args) throws Exception {
        int puerto = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        System.out.println("Cargando corpus de articulos...");
        RepositorioArticulos repositorioArticulos = new RepositorioArticulos();
        System.out.println("  -> " + repositorioArticulos.total() + " articulos cargados.");

        System.out.println("Cargando modelo de embeddings (GloVe filtrado)...");
        RepositorioEmbeddings repositorioEmbeddings = new RepositorioEmbeddings();
        System.out.println("  -> " + repositorioEmbeddings.tamano() + " vectores de palabra cargados.");

        System.out.println("Inicializando motor de similitud (Requerimiento 1)...");
        MotorSimilitud motorSimilitud = new MotorSimilitud(repositorioArticulos, repositorioEmbeddings);

        System.out.println("Inicializando motor de clustering jerarquico (Requerimiento 2)...");
        MotorClustering motorClustering = new MotorClustering(repositorioArticulos, motorSimilitud);

        ServidorHttp servidor = new ServidorHttp(puerto, repositorioArticulos, motorSimilitud, motorClustering);
        servidor.iniciar();

        System.out.println("Aplicacion iniciada. Escuchando en el puerto " + puerto);
        System.out.println("Interfaz web disponible en http://localhost:" + puerto + "/");
    }
}
