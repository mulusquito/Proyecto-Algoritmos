package co.edu.uniquindio.bibliometria;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad de una sola ejecucion que dibuja (Java2D) el diagrama de paquetes y
 * clases de la aplicacion para el documento tecnico (seccion 2.4). No forma
 * parte de la aplicacion.
 * <p>
 * Uso: {@code java -cp <clases> co.edu.uniquindio.bibliometria.GenerarDiagramaClases <archivo.png>}
 */
public class GenerarDiagramaClases {

    private static final Color AZUL = new Color(0x1f3a5f);
    private static final Color BORDE_PAQUETE = new Color(0x6b8fb8);
    private static final Color FONDO_PAQUETE = new Color(0xedf3fb);
    private static final Color FONDO_SUB = new Color(0xdfe9f6);
    private static final Color GRIS = new Color(0x666666);
    private static final Color DORADO = new Color(0x9a6b1f);
    private static final Font F_TITULO = new Font("SansSerif", Font.BOLD, 30);
    private static final Font F_PAQUETE = new Font("SansSerif", Font.BOLD, 21);
    private static final Font F_CLASE = new Font("SansSerif", Font.BOLD, 15);
    private static final Font F_ESTEREOTIPO = new Font("SansSerif", Font.ITALIC, 13);
    private static final Font F_METODO = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font F_NOTA = new Font("SansSerif", Font.ITALIC, 13);

    /** Una clase: nombre, estereotipo opcional y metodos clave. */
    private record Clase(String nombre, String estereotipo, String... metodos) {
        static Clase c(String nombre, String... metodos) { return new Clase(nombre, null, metodos); }
        static Clase i(String nombre, String... metodos) { return new Clase(nombre, "«interface»", metodos); }
    }

    /** Posicion de cada clase dibujada, para las flechas. */
    private static final Map<String, int[]> CAJAS = new HashMap<>();

    private static Graphics2D g;

    public static void main(String[] args) throws IOException {
        int ancho = 2900, alto = 2050;
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ancho, alto);

        g.setColor(AZUL);
        g.setFont(F_TITULO);
        g.drawString("Diagrama de paquetes y clases — co.edu.uniquindio.bibliometria", 40, 50);
        g.setFont(F_NOTA);
        g.setColor(GRIS);
        g.drawString("Flecha sólida: crea / contiene.  Flecha punteada gris: usa.  Flecha punteada dorada con punta hueca: implementa la interfaz.  "
                + "Cada motor corresponde a un requerimiento.", 40, 78);

        // ---------------- Fila 1: entrada, web, util ----------------
        paquete("(raíz)", 40, 110, 520, new Clase[]{
                Clase.c("BibliometriaApplication", "+main(args)  [crea todos los objetos]")}, 1);
        paquete("web", 600, 110, 760, new Clase[]{
                Clase.c("ServidorHttp", "+manejarArticulos()  +manejarSimilitud()", "+manejarClustering()  +manejarBusqueda()", "+manejarRanking()"),
                Clase.c("ArticuloDTOSalida")}, 2);
        paquete("util", 1400, 110, 520, new Clase[]{Clase.c("JsonParser"), Clase.c("JsonEscritor")}, 2);

        // ---------------- Fila 2: los cuatro motores ----------------
        int y2 = 400;
        // similitud (Requerimiento 1)
        int xs = 40, ws = 700;
        int finSim = paqueteConSub("similitud  —  Requerimiento 1", xs, y2, ws,
                new Clase[]{Clase.c("MotorSimilitud", "+compararArticulos(a, b)", "+compararVarios(articulos)"), Clase.c("ResultadoSimilitud")},
                new String[]{"clasico", "ia"},
                new Clase[][]{
                        {Clase.c("LevenshteinSimilitud"), Clase.c("NeedlemanWunschSimilitud"), Clase.c("CosenoTfIdfSimilitud"),
                                Clase.c("TfIdfVectorizador"), Clase.c("JaccardSimilitud")},
                        {Clase.c("SimilitudEmbeddings", "+conGlove(...)  +conFastText(...)"), Clase.c("VectorizadorEmbeddings"),
                                Clase.c("RepositorioEmbeddings", "+glove()  +fastText()")}});
        // clustering (Requerimiento 2)
        int xc = 780, wc = 700;
        int finClu = paqueteConSub("clustering  —  Requerimiento 2", xc, y2, wc,
                new Clase[]{Clase.c("MotorClustering", "+getResultados()  +getMejorResultado()"),
                        Clase.c("ClusteringJerarquico", "+ejecutar(distancias, ..., estrategia)"),
                        Clase.i("EstrategiaEnlace", "+distanciaFusion(dIM, dJM, |I|, |J|, |M|)"),
                        Clase.c("EnlaceSimple"), Clase.c("EnlaceCompleto"), Clase.c("EnlacePromedio"),
                        Clase.c("DistanciaCorpus"), Clase.c("CorteDendrograma"),
                        Clase.c("NodoDendrograma"), Clase.c("PasoFusion"), Clase.c("ResultadoClustering")},
                new String[]{"evaluacion"},
                new Clase[][]{{Clase.c("CorrelacionCofenetica"), Clase.c("Silueta")}});
        // busqueda (Requerimiento 3)
        int xb = 1520, wb = 660;
        int finBus = paqueteConSub("busqueda  —  Requerimiento 3", xb, y2, wb,
                new Clase[]{Clase.c("MotorBusqueda", "+buscar(patron, ...)"),
                        Clase.i("AlgoritmoBusqueda<E>", "+preprocesar(patron) : E", "+buscar(texto, patron, E, traza)"),
                        Clase.c("KnuthMorrisPratt"), Clase.c("BoyerMoore"), Clase.c("RabinKarp"),
                        Clase.c("ResultadoBusqueda"), Clase.c("Traza")},
                new String[0], new Clase[0][]);
        // ordenamiento (seguimiento)
        int xo = 2220, wo = 640;
        int finOrd = paqueteConSub("ordenamiento  —  Seguimiento", xo, y2, wo,
                new Clase[]{Clase.c("MotorRanking", "+ejecutar(top, repeticiones)"),
                        Clase.i("AlgoritmoOrdenamiento", "+ordenarDescendente(entrada)"),
                        Clase.c("CountingSort"), Clase.c("RadixSort"), Clase.c("BucketSort"),
                        Clase.c("ContadorFrecuencias"), Clase.c("TerminoFrecuencia"), Clase.c("ResultadoOrdenamiento")},
                new String[0], new Clase[0][]);

        // ---------------- Fila 3: base comun ----------------
        int y3 = Math.max(Math.max(finSim, finClu), Math.max(finBus, finOrd)) + 90;
        paquete("texto", 40, y3, 760, new Clase[]{
                Clase.c("Preprocesador", "+procesarParaVectorial(texto)", "+procesarParaEmbeddings(texto)"),
                Clase.c("Stemmer"), Clase.c("ListaStopwords")}, 2);
        paquete("datos", 900, y3, 560, new Clase[]{Clase.c("RepositorioArticulos", "+listarTodos()  +buscarPorId(id)")}, 1);
        paquete("model", 1560, y3, 560, new Clase[]{Clase.c("Articulo", "id, titulo, autores, abstract, tokens")}, 1);

        // ---------------- Relaciones ----------------
        flecha("BibliometriaApplication", "ServidorHttp", false, AZUL);
        // La capa web usa los cuatro motores: flechas de paquete a paquete (verticales, sin cruzar clases).
        flechaVertical("pkg:web", "pkg:similitud", 0.2, GRIS);
        flechaVertical("pkg:web", "pkg:clustering", 0.5, GRIS);
        flechaVertical("pkg:web", "pkg:busqueda", 0.8, GRIS);
        flechaVertical("pkg:web", "pkg:ordenamiento", 0.97, GRIS);
        flecha("ServidorHttp", "JsonEscritor", true, GRIS);
        flecha("MotorClustering", "MotorSimilitud", true, GRIS);
        flecha("EnlaceSimple", "EstrategiaEnlace", true, DORADO);
        flecha("EnlaceCompleto", "EstrategiaEnlace", true, DORADO);
        flecha("EnlacePromedio", "EstrategiaEnlace", true, DORADO);
        flecha("KnuthMorrisPratt", "AlgoritmoBusqueda<E>", true, DORADO);
        flecha("BoyerMoore", "AlgoritmoBusqueda<E>", true, DORADO);
        flecha("RabinKarp", "AlgoritmoBusqueda<E>", true, DORADO);
        flecha("CountingSort", "AlgoritmoOrdenamiento", true, DORADO);
        flecha("RadixSort", "AlgoritmoOrdenamiento", true, DORADO);
        flecha("BucketSort", "AlgoritmoOrdenamiento", true, DORADO);
        // Los cuatro motores leen el corpus de datos/RepositorioArticulos; similitud usa el preprocesamiento.
        flechaVertical("pkg:similitud", "pkg:texto", 0.5, GRIS);
        flechaVertical("pkg:clustering", "pkg:datos", 0.5, GRIS);
        flechaPaquetes("pkg:similitud", "pkg:datos", GRIS);
        flechaPaquetes("pkg:busqueda", "pkg:datos", GRIS);
        flechaPaquetes("pkg:ordenamiento", "pkg:datos", GRIS);
        flecha("RepositorioArticulos", "Articulo", false, AZUL);
        flecha("RepositorioArticulos", "Preprocesador", true, GRIS);

        g.dispose();
        File salida = new File(args.length > 0 ? args[0] : "05-diagrama-clases.png");
        // Recortar el alto sobrante.
        int altoUsado = 0;
        for (int[] c : CAJAS.values()) altoUsado = Math.max(altoUsado, c[1] + c[3]);
        ImageIO.write(img.getSubimage(0, 0, ancho, Math.min(alto, altoUsado + 60)), "png", salida);
        System.out.println("Diagrama: " + salida.getPath() + " (" + CAJAS.size() + " clases)");
    }

    // ------------------------------------------------------------------
    private static int altoClase(Clase c) {
        return 38 + (c.estereotipo() != null ? 18 : 0) + c.metodos().length * 19;
    }

    /** Dibuja las clases en 'columnas' columnas dentro del rectangulo; devuelve el y final. */
    private static int clases(Clase[] lista, int x, int y, int w, int columnas) {
        int gap = 12;
        int anchoClase = (w - gap * (columnas - 1)) / columnas;
        int[] yCol = new int[columnas];
        for (int k = 0; k < columnas; k++) yCol[k] = y;
        for (int idx = 0; idx < lista.length; idx++) {
            Clase c = lista[idx];
            // Las clases con metodos ocupan todo el ancho; las demas se reparten en columnas.
            boolean ancha = c.metodos().length > 0 && columnas > 1;
            int col = 0;
            if (ancha) {
                int yMax = 0;
                for (int v : yCol) yMax = Math.max(yMax, v);
                for (int k = 0; k < columnas; k++) yCol[k] = yMax;
            } else {
                for (int k = 1; k < columnas; k++) if (yCol[k] < yCol[col]) col = k;
            }
            int cx = ancho(ancha) ? x : x + col * (anchoClase + gap);
            int cw = ancha ? w : anchoClase;
            int h = altoClase(c);
            caja(c, cx, yCol[col], cw, h);
            if (ancha) {
                int siguiente = yCol[0] + h + gap;
                for (int k = 0; k < columnas; k++) yCol[k] = siguiente;
            } else {
                yCol[col] += h + gap;
            }
        }
        int fin = y;
        for (int v : yCol) fin = Math.max(fin, v);
        return fin - gap;
    }

    private static boolean ancho(boolean ancha) {
        return ancha;
    }

    private static void caja(Clase c, int x, int y, int w, int h) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(AZUL);
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(x, y, w, h, 10, 10);
        int ty = y + 24;
        if (c.estereotipo() != null) {
            g.setFont(F_ESTEREOTIPO);
            centrado(c.estereotipo(), x, w, ty - 4);
            ty += 18;
        }
        g.setFont(F_CLASE);
        centrado(c.nombre(), x, w, ty);
        if (c.metodos().length > 0) {
            g.setColor(new Color(0xcccccc));
            g.drawLine(x + 8, ty + 8, x + w - 8, ty + 8);
            g.setColor(new Color(0x333333));
            g.setFont(F_METODO);
            int my = ty + 26;
            for (String m : c.metodos()) {
                g.drawString(m, x + 12, my);
                my += 19;
            }
        }
        CAJAS.put(c.nombre(), new int[]{x, y, w, h});
    }

    private static void centrado(String s, int x, int w, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, x + (w - fm.stringWidth(s)) / 2, y);
    }

    private static int paquete(String nombre, int x, int y, int w, Clase[] lista, int columnas) {
        // Primero se mide el alto dibujando en una imagen auxiliar.
        int alto = medir(lista, w - 32, columnas) + 70;
        marco(nombre, x, y, w, alto, FONDO_PAQUETE);
        clases(lista, x + 16, y + 52, w - 32, columnas);
        return y + alto;
    }

    private static int paqueteConSub(String nombre, int x, int y, int w, Clase[] propias, String[] subnombres, Clase[][] subclases) {
        int interior = w - 32;
        int alto = 52 + medir(propias, interior, 2) + 18;
        List<Integer> altosSub = new ArrayList<>();
        for (Clase[] s : subclases) {
            int a = medir(s, interior - 28, 2) + 60;
            altosSub.add(a);
            alto += a + 14;
        }
        marco(nombre, x, y, w, alto + 4, FONDO_PAQUETE);
        int cy = clases(propias, x + 16, y + 52, interior, 2) + 18;
        for (int k = 0; k < subclases.length; k++) {
            marco(subnombres[k], x + 16, cy, interior, altosSub.get(k), FONDO_SUB);
            clases(subclases[k], x + 30, cy + 46, interior - 28, 2);
            cy += altosSub.get(k) + 14;
        }
        return y + alto + 4;
    }

    private static int medir(Clase[] lista, int w, int columnas) {
        Graphics2D real = g;
        Map<String, int[]> copia = new HashMap<>(CAJAS);
        BufferedImage tmp = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        g = tmp.createGraphics();
        int fin = clases(lista, 0, 0, w, columnas);
        g.dispose();
        g = real;
        CAJAS.clear();
        CAJAS.putAll(copia);
        return fin;
    }

    private static void marco(String nombre, int x, int y, int w, int h, Color fondo) {
        // El paquete se registra por su primera palabra ("similitud", "web", ...) para las flechas entre paquetes.
        CAJAS.put("pkg:" + nombre.split(" ")[0], new int[]{x, y, w, h});
        g.setColor(fondo);
        g.fillRoundRect(x, y, w, h, 16, 16);
        g.setColor(BORDE_PAQUETE);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 16, 16);
        g.setColor(AZUL);
        g.setFont(F_PAQUETE);
        g.drawString(nombre, x + 18, y + 32);
    }

    /** Flecha del borde de una caja al borde de la otra. */
    private static void flecha(String desde, String hasta, boolean punteada, Color color) {
        int[] a = CAJAS.get(desde), b = CAJAS.get(hasta);
        if (a == null || b == null) throw new IllegalStateException("Falta la caja " + (a == null ? desde : hasta));
        double ax = a[0] + a[2] / 2.0, ay = a[1] + a[3] / 2.0, bx = b[0] + b[2] / 2.0, by = b[1] + b[3] / 2.0;
        double[] p1 = borde(a, bx - ax, by - ay), p2 = borde(b, ax - bx, ay - by);
        g.setColor(color);
        g.setStroke(punteada ? new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7f, 5f}, 0f)
                : new BasicStroke(1.8f));
        g.drawLine((int) p1[0], (int) p1[1], (int) p2[0], (int) p2[1]);
        double ang = Math.atan2(p2[1] - p1[1], p2[0] - p1[0]);
        int largo = 13;
        Polygon punta = new Polygon();
        punta.addPoint((int) p2[0], (int) p2[1]);
        punta.addPoint((int) (p2[0] - largo * Math.cos(ang - 0.4)), (int) (p2[1] - largo * Math.sin(ang - 0.4)));
        punta.addPoint((int) (p2[0] - largo * Math.cos(ang + 0.4)), (int) (p2[1] - largo * Math.sin(ang + 0.4)));
        g.setStroke(new BasicStroke(1.6f));
        if (color == DORADO) {
            g.setColor(Color.WHITE);
            g.fillPolygon(punta);
            g.setColor(color);
            g.drawPolygon(punta);
        } else {
            g.fillPolygon(punta);
        }
    }

    /**
     * Flecha vertical del borde inferior del paquete 'desde' al borde superior de 'hasta', en la posicion
     * horizontal 'fraccion' del paquete destino (asi no atraviesa las clases de ningun paquete).
     */
    private static void flechaVertical(String desde, String hasta, double fraccion, Color color) {
        int[] a = CAJAS.get(desde), b = CAJAS.get(hasta);
        int x = (int) (b[0] + b[2] * Math.min(fraccion, 0.95));
        int xa = Math.max(a[0] + 20, Math.min(a[0] + a[2] - 20, x));
        int y1 = a[1] + a[3], y2 = b[1];
        int yMedio = (y1 + y2) / 2;
        g.setColor(color);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{7f, 5f}, 0f));
        g.drawLine(xa, y1, xa, yMedio);
        g.drawLine(xa, yMedio, x, yMedio);
        g.drawLine(x, yMedio, x, y2);
        Polygon punta = new Polygon();
        punta.addPoint(x, y2);
        punta.addPoint(x - 7, y2 - 13);
        punta.addPoint(x + 7, y2 - 13);
        g.fillPolygon(punta);
    }

    /** Flecha recta entre los bordes de dos paquetes. */
    private static void flechaPaquetes(String desde, String hasta, Color color) {
        flecha(desde, hasta, true, color);
    }

    /** Punto donde la recta desde el centro de la caja en direccion (dx, dy) corta su borde. */
    private static double[] borde(int[] c, double dx, double dy) {
        double cx = c[0] + c[2] / 2.0, cy = c[1] + c[3] / 2.0;
        double tx = dx == 0 ? Double.MAX_VALUE : (c[2] / 2.0) / Math.abs(dx);
        double ty = dy == 0 ? Double.MAX_VALUE : (c[3] / 2.0) / Math.abs(dy);
        double t = Math.min(tx, ty);
        return new double[]{cx + dx * t, cy + dy * t};
    }
}
