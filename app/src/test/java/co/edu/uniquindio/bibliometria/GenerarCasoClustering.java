package co.edu.uniquindio.bibliometria;

import co.edu.uniquindio.bibliometria.clustering.CorteDendrograma;
import co.edu.uniquindio.bibliometria.clustering.EnlaceCompleto;
import co.edu.uniquindio.bibliometria.clustering.EnlacePromedio;
import co.edu.uniquindio.bibliometria.clustering.EnlaceSimple;
import co.edu.uniquindio.bibliometria.clustering.EstrategiaEnlace;
import co.edu.uniquindio.bibliometria.clustering.MotorClustering;
import co.edu.uniquindio.bibliometria.clustering.NodoDendrograma;
import co.edu.uniquindio.bibliometria.clustering.PasoFusion;
import co.edu.uniquindio.bibliometria.clustering.ResultadoClustering;
import co.edu.uniquindio.bibliometria.clustering.evaluacion.CorrelacionCofenetica;
import co.edu.uniquindio.bibliometria.datos.RepositorioArticulos;
import co.edu.uniquindio.bibliometria.model.Articulo;
import co.edu.uniquindio.bibliometria.similitud.MotorSimilitud;
import co.edu.uniquindio.bibliometria.similitud.clasico.TfIdfVectorizador;
import co.edu.uniquindio.bibliometria.similitud.ia.RepositorioEmbeddings;
import co.edu.uniquindio.bibliometria.texto.Preprocesador;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad de una sola ejecucion que genera, con el codigo real del
 * Requerimiento 2, los datos del caso de estudio del documento tecnico
 * (preprocesamiento de un abstract, fragmento de la matriz de distancias,
 * primeras fusiones de cada criterio, ejemplo de Lance-Williams, distancias
 * cofeneticas y cortes con silueta) y las imagenes PNG de los 3 dendrogramas.
 * No forma parte de la aplicacion.
 * <p>
 * Uso: {@code java -cp <clases> co.edu.uniquindio.bibliometria.GenerarCasoClustering <carpeta de imagenes>}
 */
public class GenerarCasoClustering {

    private static final Color[] COLORES = {new Color(0x2f6690), new Color(0xb3552b), new Color(0x1e7d4f),
            new Color(0x8a4fbf), new Color(0xc48a00), new Color(0xd0417e)};
    /** Grupos con que se colorea cada imagen. */
    private static final int K_IMAGEN = 4;

    public static void main(String[] args) throws IOException {
        String carpeta = args.length > 0 ? args[0] : ".";
        RepositorioArticulos repositorio = new RepositorioArticulos();
        MotorSimilitud motorSimilitud = new MotorSimilitud(repositorio, RepositorioEmbeddings.glove(), RepositorioEmbeddings.fastText());
        MotorClustering motor = new MotorClustering(repositorio, motorSimilitud);
        double[][] d = motor.getMatrizDistancias();
        List<Articulo> articulos = motor.getArticulos();
        Map<Integer, Integer> indice = motor.getIndicePorId();

        // 1. Preprocesamiento de un abstract.
        Articulo a1 = articulos.get(0);
        String primeraOracion = a1.getAbstractOriginal().substring(0, a1.getAbstractOriginal().indexOf('.') + 1);
        System.out.println("## 1. Preprocesamiento (articulo 1, primera oracion)\n");
        System.out.println("Original: " + primeraOracion);
        System.out.println("Normalizado: " + Preprocesador.normalizarParaEdicion(primeraOracion));
        List<String> tokens = Preprocesador.tokenizar(primeraOracion);
        System.out.println("Tokens (" + tokens.size() + "): " + tokens);
        List<String> sinStop = Preprocesador.eliminarStopwords(tokens);
        System.out.println("Sin stopwords (" + sinStop.size() + "): " + sinStop);
        System.out.println("Con stemming: " + Preprocesador.aplicarStemming(sinStop));
        TfIdfVectorizador v = motorSimilitud.getTfIdfVectorizador();
        Map<String, Double> vector = v.vectorizar(a1.getTokens());
        System.out.println("Abstract completo: " + a1.getTokens().size() + " tokens, vector TF-IDF de " + vector.size() + " dimensiones");

        // 2. Rango de distancias y fragmento de la matriz.
        double min = 9, max = -1;
        for (int i = 0; i < d.length; i++) for (int j = i + 1; j < d.length; j++) { min = Math.min(min, d[i][j]); max = Math.max(max, d[i][j]); }
        System.out.printf("%n## 2. Matriz de distancias: 20x20, minimo %.4f, maximo %.4f%n%n", min, max);
        // Articulos de las primeras fusiones de Single Linkage.
        List<Integer> ids = new ArrayList<>();
        for (PasoFusion p : motor.getResultados().get(0).getPasos().subList(0, 5)) {
            for (int id : p.miembrosA) if (!ids.contains(id)) ids.add(id);
            for (int id : p.miembrosB) if (!ids.contains(id)) ids.add(id);
        }
        ids = ids.subList(0, Math.min(8, ids.size()));
        StringBuilder sb = new StringBuilder("| |");
        for (int id : ids) sb.append(" [").append(id).append("] |");
        System.out.println(sb);
        sb = new StringBuilder("|---|");
        for (int ignored : ids) sb.append("---|");
        System.out.println(sb);
        for (int f : ids) {
            sb = new StringBuilder("| **[" + f + "]** |");
            for (int c : ids) sb.append(f == c ? " 0 |" : String.format(" %.4f |", d[indice.get(f)][indice.get(c)]));
            System.out.println(sb);
        }
        for (int id : ids) System.out.println("  [" + id + "] " + articulos.get(indice.get(id)).getTitulo());

        // 3. Primeras fusiones de cada criterio.
        for (ResultadoClustering r : motor.getResultados()) {
            System.out.println("\n## 3. " + r.getNombreEnlace() + ": primeras 6 fusiones y las 3 ultimas\n");
            List<PasoFusion> pasos = r.getPasos();
            for (int k = 0; k < pasos.size(); k++) {
                if (k >= 6 && k < pasos.size() - 3) continue;
                PasoFusion p = pasos.get(k);
                System.out.printf("%d | %.4f | %s | %s | %d%n", k + 1, p.altura, p.miembrosA, p.miembrosB, p.tamanoResultante);
            }
        }

        // 4. Ejemplo de Lance-Williams: la primera fusion de Complete Linkage que une dos clusters y uno de ellos ya
        //    tiene mas de un articulo, comparando las 3 formulas para varios clusters M.
        PasoFusion primera = motor.getResultados().get(0).getPasos().get(0);
        int i0 = indice.get(primera.miembrosA.get(0)), j0 = indice.get(primera.miembrosB.get(0));
        System.out.printf("%n## 4. Lance-Williams tras la primera fusion: I=[%d], J=[%d], d(I,J)=%.4f%n%n",
                primera.miembrosA.get(0), primera.miembrosB.get(0), d[i0][j0]);
        EstrategiaEnlace[] e = {new EnlaceSimple(), new EnlaceCompleto(), new EnlacePromedio()};
        int mostrados = 0;
        for (Articulo m : articulos) {
            int mi = indice.get(m.getId());
            if (mi == i0 || mi == j0 || mostrados >= 4) continue;
            mostrados++;
            System.out.printf("M=[%d]: d(I,M)=%.4f d(J,M)=%.4f -> single=%.4f complete=%.4f average=%.4f%n", m.getId(), d[i0][mi], d[j0][mi],
                    e[0].distanciaFusion(d[i0][mi], d[j0][mi], 1, 1, 1), e[1].distanciaFusion(d[i0][mi], d[j0][mi], 1, 1, 1),
                    e[2].distanciaFusion(d[i0][mi], d[j0][mi], 1, 1, 1));
        }

        // 5. Distancias cofeneticas de ejemplo (Average Linkage).
        ResultadoClustering promedio = motor.getResultados().get(2);
        double[][] cof = new CorrelacionCofenetica().calcularMatrizCofenetica(promedio.getRaiz(), indice, d.length);
        System.out.println("\n## 5. Distancias original vs cofenetica (Average Linkage)\n");
        int[][] paresEjemplo = {{ids.get(0), ids.get(1)}, {ids.get(0), ids.get(2)}, {1, 2}, {8, 10}, {14, 19}, {4, 11}};
        for (int[] p : paresEjemplo) {
            int x = indice.get(p[0]), y = indice.get(p[1]);
            System.out.printf("[%d]-[%d]: original %.4f, cofenetica %.4f%n", p[0], p[1], d[x][y], cof[x][y]);
        }

        // 6. Metricas y cortes.
        System.out.println("\n## 6. Metricas\n");
        for (ResultadoClustering r : motor.getResultados()) {
            StringBuilder linea = new StringBuilder(String.format("%s | cofenetica %.4f |", r.getNombreEnlace(), r.getCorrelacionCofenetica()));
            for (Map<String, Object> c : r.getCortes()) {
                linea.append(String.format(" k%d S=%.3f %s |", (Integer) c.get("k"), (Double) c.get("silueta"),
                        java.util.Arrays.toString((int[]) c.get("tamanos"))));
            }
            System.out.println(linea);
            @SuppressWarnings("unchecked")
            List<List<Integer>> g4 = (List<List<Integer>>) r.getCortes().get(K_IMAGEN - 2).get("grupos");
            System.out.println("   grupos k=4: " + g4);
        }

        // 7. Imagenes.
        String[] archivos = {"06-dendrograma-single.png", "07-dendrograma-complete.png", "08-dendrograma-average.png"};
        for (int t = 0; t < 3; t++) {
            ResultadoClustering r = motor.getResultados().get(t);
            File salida = new File(carpeta, archivos[t]);
            dibujar(r, salida);
            System.out.println("Imagen: " + salida.getPath());
        }
    }

    // ------------------------------------------------------------------
    // Dibujo del dendrograma (Java2D), con el mismo esquema de la interfaz web.
    // ------------------------------------------------------------------
    private static final int ESPACIO_X = 62, MARGEN_IZQ = 90, MARGEN_DER = 150, MARGEN_SUP = 70, ALTO_PLOT = 430, ALTO_ETIQUETAS = 300;

    private static void dibujar(ResultadoClustering r, File salida) throws IOException {
        NodoDendrograma raiz = r.getRaiz();
        List<NodoDendrograma> hojas = new ArrayList<>();
        recolectarHojas(raiz, hojas);
        int ancho = MARGEN_IZQ + (hojas.size() - 1) * ESPACIO_X + MARGEN_DER;
        int alto = MARGEN_SUP + ALTO_PLOT + ALTO_ETIQUETAS;
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ancho, alto);

        // Escala vertical: el eje empieza un poco por debajo de la fusion mas baja (todas estan entre ~0.70 y ~0.97).
        double alturaMin = Double.MAX_VALUE;
        for (PasoFusion p : r.getPasos()) alturaMin = Math.min(alturaMin, p.altura);
        double base = Math.floor((alturaMin - 0.03) * 20) / 20.0;
        double tope = Math.ceil(raiz.getAltura() * 20) / 20.0;

        Map<NodoDendrograma, Double> x = new HashMap<>();
        for (int i = 0; i < hojas.size(); i++) x.put(hojas.get(i), (double) (MARGEN_IZQ + i * ESPACIO_X));
        asignarX(raiz, x);

        // Grupos del corte.
        @SuppressWarnings("unchecked")
        List<List<Integer>> grupos = (List<List<Integer>>) r.getCortes().get(K_IMAGEN - 2).get("grupos");
        Map<Integer, Integer> grupoDe = new HashMap<>();
        for (int k = 0; k < grupos.size(); k++) for (int id : grupos.get(k)) grupoDe.put(id, k);

        // Titulo.
        g.setColor(new Color(0x1f3a5f));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(r.getNombreEnlace() + "  -  correlacion cofenetica " + String.format("%.4f", r.getCorrelacionCofenetica()), 20, 32);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(0x555555));
        g.drawString("Distancia = 1 - coseno(TF-IDF). Colores: corte en " + K_IMAGEN + " grupos (linea punteada). El eje vertical empieza en "
                + String.format("%.2f", base) + ".", 20, 54);

        // Eje vertical con marcas cada 0.05.
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (double v = base; v <= tope + 1e-9; v += 0.05) {
            int yy = y(v, base, tope);
            g.setColor(new Color(0xe3e8ee));
            g.drawLine(MARGEN_IZQ - 10, yy, ancho - MARGEN_DER + 10, yy);
            g.setColor(new Color(0x555555));
            g.drawString(String.format("%.2f", v), 30, yy + 4);
        }

        // Ramas.
        g.setStroke(new BasicStroke(2f));
        dibujarNodo(g, raiz, x, base, tope);

        // Linea de corte.
        List<PasoFusion> pasos = r.getPasos();
        double alturaCorte = (pasos.get(pasos.size() - K_IMAGEN).altura + pasos.get(pasos.size() - K_IMAGEN + 1).altura) / 2;
        int yc = y(alturaCorte, base, tope);
        g.setColor(new Color(0xb3552b));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
        g.drawLine(MARGEN_IZQ - 20, yc, ancho - MARGEN_DER + 20, yc);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("corte en " + K_IMAGEN + " grupos (altura " + String.format("%.4f", alturaCorte) + ")", MARGEN_IZQ, yc - 6);

        // Hojas y etiquetas.
        int yHoja = y(base, base, tope);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (NodoDendrograma h : hojas) {
            Color c = COLORES[grupoDe.get(h.getArticuloId()) % COLORES.length];
            int xx = (int) Math.round(x.get(h));
            g.setColor(c);
            g.fillOval(xx - 5, yHoja - 5, 10, 10);
            AffineTransform original = g.getTransform();
            g.translate(xx + 4, yHoja + 14);
            g.rotate(Math.toRadians(65));
            g.drawString(h.getEtiqueta(), 0, 0);
            g.setTransform(original);
        }
        g.dispose();
        ImageIO.write(img, "png", salida);
    }

    private static int y(double altura, double base, double tope) {
        return (int) Math.round(MARGEN_SUP + ALTO_PLOT * (1 - (altura - base) / (tope - base)));
    }

    private static void recolectarHojas(NodoDendrograma n, List<NodoDendrograma> hojas) {
        if (n.isEsHoja()) { hojas.add(n); return; }
        recolectarHojas(n.getIzquierdo(), hojas);
        recolectarHojas(n.getDerecho(), hojas);
    }

    private static double asignarX(NodoDendrograma n, Map<NodoDendrograma, Double> x) {
        if (n.isEsHoja()) return x.get(n);
        double medio = (asignarX(n.getIzquierdo(), x) + asignarX(n.getDerecho(), x)) / 2;
        x.put(n, medio);
        return medio;
    }

    /** Dibuja la U de cada fusion; las hojas bajan hasta la base del eje. */
    private static void dibujarNodo(Graphics2D g, NodoDendrograma n, Map<NodoDendrograma, Double> x, double base, double tope) {
        if (n.isEsHoja()) return;
        int yNodo = y(n.getAltura(), base, tope);
        int[] hijosY = new int[2];
        NodoDendrograma[] hijos = {n.getIzquierdo(), n.getDerecho()};
        g.setColor(new Color(0x2f6690));
        for (int i = 0; i < 2; i++) {
            NodoDendrograma h = hijos[i];
            hijosY[i] = h.isEsHoja() ? y(base, base, tope) : y(h.getAltura(), base, tope);
            int xx = (int) Math.round(x.get(h));
            g.drawLine(xx, hijosY[i], xx, yNodo);
        }
        g.drawLine((int) Math.round(x.get(hijos[0])), yNodo, (int) Math.round(x.get(hijos[1])), yNodo);
        dibujarNodo(g, hijos[0], x, base, tope);
        dibujarNodo(g, hijos[1], x, base, tope);
    }
}
