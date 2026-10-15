// ============================================================
// Frontend de la aplicacion (vanilla JS, sin frameworks/CDN).
// Consume la API REST implementada en co.edu.uniquindio.bibliometria.web.
// ============================================================

let ARTICULOS = [];
let CLUSTERING_RESPUESTA = null;
let CLUSTER_ACTUAL = 0;
let RANKING_CARGADO = false;

document.addEventListener("DOMContentLoaded", () => {
    configurarTabs();
    configurarTabsClustering();
    cargarArticulos();
    document.getElementById("btn-comparar").addEventListener("click", ejecutarComparacion);
    document.getElementById("btn-medir").addEventListener("click", cargarRanking);
});

// ------------------------------------------------------------
// Navegacion por pestanas
// ------------------------------------------------------------
function configurarTabs() {
    document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("activo"));
            document.querySelectorAll(".tab-contenido").forEach(s => s.classList.remove("activo"));
            btn.classList.add("activo");
            document.getElementById("tab-" + btn.dataset.tab).classList.add("activo");
            if (btn.dataset.tab === "clustering" && !CLUSTERING_RESPUESTA) {
                cargarClustering();
            }
            if (btn.dataset.tab === "ranking" && !RANKING_CARGADO) {
                cargarRanking();
            }
        });
    });
}

function configurarTabsClustering() {
    document.querySelectorAll(".cluster-tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".cluster-tab-btn").forEach(b => b.classList.remove("activo"));
            btn.classList.add("activo");
            CLUSTER_ACTUAL = parseInt(btn.dataset.cluster, 10);
            if (CLUSTERING_RESPUESTA) dibujarDendrogramaActual();
        });
    });
}

// ------------------------------------------------------------
// Corpus de articulos
// ------------------------------------------------------------
async function cargarArticulos() {
    const respuesta = await fetch("/api/articulos");
    ARTICULOS = await respuesta.json();
    renderizarTablaArticulos();
    poblarSelectores();
}

function renderizarTablaArticulos() {
    const contenedor = document.getElementById("tabla-articulos");
    let html = "<table><thead><tr><th>#</th><th>Título</th><th>Autores</th><th></th></tr></thead><tbody>";
    for (const art of ARTICULOS) {
        html += `<tr>
            <td data-etiqueta="ID">${art.id}</td>
            <td data-etiqueta="Título">${escaparHtml(art.titulo)}</td>
            <td data-etiqueta="Autores">${escaparHtml(art.autores)}</td>
            <td><button class="btn-ver" onclick="alternarAbstract(${art.id})">Ver abstract</button></td>
        </tr>
        <tr id="abstract-${art.id}" class="fila-abstract" style="display:none">
            <td colspan="4">${escaparHtml(art.resumen)}</td>
        </tr>`;
    }
    html += "</tbody></table>";
    contenedor.innerHTML = html;
}

function alternarAbstract(id) {
    const fila = document.getElementById("abstract-" + id);
    fila.style.display = fila.style.display === "none" ? "table-row" : "none";
}

function poblarSelectores() {
    const selectA = document.getElementById("select-articulo-a");
    const selectB = document.getElementById("select-articulo-b");
    const opciones = ARTICULOS.map(a => `<option value="${a.id}">[${a.id}] ${escaparHtml(acortar(a.titulo, 60))}</option>`).join("");
    selectA.innerHTML = opciones;
    selectB.innerHTML = opciones;
    if (ARTICULOS.length > 1) selectB.selectedIndex = 1;
}

// ------------------------------------------------------------
// Requerimiento 1: Similitud
// ------------------------------------------------------------
async function ejecutarComparacion() {
    const idA = document.getElementById("select-articulo-a").value;
    const idB = document.getElementById("select-articulo-b").value;
    const contenedor = document.getElementById("resultado-similitud");

    if (idA === idB) {
        contenedor.innerHTML = `<p style="color:#b3552b">Selecciona dos artículos distintos.</p>`;
        return;
    }
    contenedor.innerHTML = `<p class="cargando">Calculando los 6 algoritmos...</p>`;

    const respuesta = await fetch(`/api/similitud?idA=${idA}&idB=${idB}`);
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        contenedor.innerHTML = `<p style="color:#b3552b">${escaparHtml(datos.error || "Error")}</p>`;
        return;
    }
    renderizarResultadosSimilitud(datos);
}

function renderizarResultadosSimilitud(datos) {
    const contenedor = document.getElementById("resultado-similitud");
    let html = `<p><strong>A:</strong> ${escaparHtml(datos.articuloA.titulo)} &nbsp;&nbsp;
                 <strong>B:</strong> ${escaparHtml(datos.articuloB.titulo)}</p>`;
    html += `<div class="tarjetas-resultado">`;
    for (const r of datos.resultados) {
        const esIa = r.categoria === "ia";
        const porcentaje = Math.max(0, Math.min(100, r.similitud * 100)).toFixed(1);
        html += `<div class="tarjeta ${esIa ? "ia" : ""}">
            <div class="categoria">${esIa ? "Algoritmo con IA" : "Algoritmo clásico"}</div>
            <h3>${escaparHtml(r.algoritmo)}</h3>
            <div class="valor-similitud">${r.similitud.toFixed(4)}</div>
            <div class="barra-fondo"><div class="barra-relleno" style="width:${porcentaje}%"></div></div>
            <details>
                <summary>Ver detalle del cálculo</summary>
                ${renderizarDetalle(r.detalle)}
            </details>
        </div>`;
    }
    html += `</div>`;
    contenedor.innerHTML = html;
}

function renderizarDetalle(detalle) {
    let filas = "";
    for (const [clave, valor] of Object.entries(detalle)) {
        filas += `<tr><td>${escaparHtml(clave)}</td><td>${formatearValorDetalle(valor)}</td></tr>`;
    }
    return `<table class="detalle-tabla">${filas}</table>`;
}

function formatearValorDetalle(valor) {
    if (valor === null || valor === undefined) return "-";
    if (typeof valor === "number") return Number.isInteger(valor) ? valor : valor.toFixed(5);
    if (Array.isArray(valor)) return escaparHtml(valor.join(", "));
    if (typeof valor === "object") {
        return Object.entries(valor).map(([k, v]) =>
            `${escaparHtml(k)}: ${typeof v === "number" ? v.toFixed(4) : escaparHtml(String(v))}`
        ).join("<br>");
    }
    return escaparHtml(String(valor));
}

// ------------------------------------------------------------
// Requerimiento 2: Clustering / Dendrograma
// ------------------------------------------------------------
async function cargarClustering() {
    const resumen = document.getElementById("resumen-clustering");
    resumen.innerHTML = `<p class="cargando">Calculando los 3 algoritmos de clustering...</p>`;
    const respuesta = await fetch("/api/clustering");
    CLUSTERING_RESPUESTA = await respuesta.json();
    renderizarResumenClustering();
    dibujarDendrogramaActual();
}

function renderizarResumenClustering() {
    const resumen = document.getElementById("resumen-clustering");
    let filas = "";
    for (const r of CLUSTERING_RESPUESTA.resultados) {
        const esMejor = r.nombreEnlace === CLUSTERING_RESPUESTA.algoritmoMasCoherente;
        filas += `<tr>
            <td>${escaparHtml(r.nombreEnlace)} ${esMejor ? '<span class="badge-mejor">Más coherente</span>' : ""}</td>
            <td>${r.correlacionCofenetica.toFixed(4)}</td>
        </tr>`;
    }
    resumen.innerHTML = `
        <p>Métrica de evaluación interna: <strong>Coeficiente de Correlación Cofenética</strong>
        (Pearson entre las distancias originales y las distancias cofenéticas del dendrograma; 1 = ajuste perfecto).</p>
        <table>
            <thead><tr><th>Algoritmo de enlace</th><th>Correlación cofenética</th></tr></thead>
            <tbody>${filas}</tbody>
        </table>`;
}

function dibujarDendrogramaActual() {
    const contenedor = document.getElementById("contenedor-dendrograma");
    if (!CLUSTERING_RESPUESTA) return;
    const resultado = CLUSTERING_RESPUESTA.resultados[CLUSTER_ACTUAL];
    contenedor.innerHTML = "";
    contenedor.appendChild(construirSvgDendrograma(resultado.dendrograma));
}

function construirSvgDendrograma(raiz) {
    const espacioX = 55;
    const margenIzq = 30;
    const margenDer = 30;
    const margenSup = 20;
    const alturaPlot = 320;
    const espacioEtiquetas = 130;

    let siguienteIndice = 0;
    function asignarPosicionesHoja(nodo) {
        if (nodo.esHoja) {
            nodo._x = margenIzq + siguienteIndice * espacioX;
            siguienteIndice++;
        } else {
            asignarPosicionesHoja(nodo.izquierdo);
            asignarPosicionesHoja(nodo.derecho);
            nodo._x = (nodo.izquierdo._x + nodo.derecho._x) / 2;
        }
    }
    asignarPosicionesHoja(raiz);

    const alturaMaxima = raiz.altura || 1;
    function y(altura) {
        return margenSup + alturaPlot * (1 - altura / alturaMaxima);
    }

    const numHojas = siguienteIndice;
    const ancho = margenIzq + (numHojas - 1) * espacioX + margenDer;
    const alto = margenSup + alturaPlot + espacioEtiquetas;

    const NS = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(NS, "svg");
    svg.setAttribute("width", ancho);
    svg.setAttribute("height", alto);
    svg.setAttribute("viewBox", `0 0 ${ancho} ${alto}`);

    function linea(x1, y1, x2, y2) {
        const l = document.createElementNS(NS, "line");
        l.setAttribute("x1", x1); l.setAttribute("y1", y1);
        l.setAttribute("x2", x2); l.setAttribute("y2", y2);
        l.setAttribute("class", "linea-dendro");
        svg.appendChild(l);
    }

    function dibujar(nodo) {
        if (nodo.esHoja) {
            const texto = document.createElementNS(NS, "text");
            texto.setAttribute("x", 0);
            texto.setAttribute("y", 0);
            texto.setAttribute("class", "etiqueta-hoja");
            texto.setAttribute("transform", `translate(${nodo._x + 3},${y(0) + 5}) rotate(65)`);
            texto.textContent = nodo.etiqueta || ("Art. " + nodo.articuloId);
            svg.appendChild(texto);

            const punto = document.createElementNS(NS, "circle");
            punto.setAttribute("cx", nodo._x);
            punto.setAttribute("cy", y(0));
            punto.setAttribute("r", 2.5);
            punto.setAttribute("fill", "#1f3a5f");
            svg.appendChild(punto);
            return;
        }
        const yNodo = y(nodo.altura);
        const yIzq = y(nodo.izquierdo.altura);
        const yDer = y(nodo.derecho.altura);

        linea(nodo.izquierdo._x, yIzq, nodo.izquierdo._x, yNodo);
        linea(nodo.derecho._x, yDer, nodo.derecho._x, yNodo);
        linea(nodo.izquierdo._x, yNodo, nodo.derecho._x, yNodo);

        dibujar(nodo.izquierdo);
        dibujar(nodo.derecho);
    }
    dibujar(raiz);

    return svg;
}

// ------------------------------------------------------------
// Seguimiento: Ranking de terminos (Counting, Radix y Bucket Sort)
// ------------------------------------------------------------
async function cargarRanking() {
    const resumen = document.getElementById("resumen-ranking");
    const repeticiones = document.getElementById("select-repeticiones").value;
    resumen.innerHTML = `<p class="cargando">Ordenando y midiendo los 3 algoritmos (${repeticiones} repeticiones)...</p>`;
    const respuesta = await fetch(`/api/ranking?top=15&repeticiones=${repeticiones}`);
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        resumen.innerHTML = `<p style="color:#b3552b">${escaparHtml(datos.error || "Error")}</p>`;
        return;
    }
    RANKING_CARGADO = true;
    renderizarRanking(datos);
}

function renderizarRanking(datos) {
    const c = datos.corpus;
    const resumen = document.getElementById("resumen-ranking");
    resumen.innerHTML = `
        <p><strong>${c.terminosDistintos}</strong> términos distintos (n) &middot; ${c.totalTokens} tokens en total &middot;
        frecuencia máxima k = <strong>${c.frecuenciaMaxima}</strong> &middot; ${c.valoresDeFrecuenciaDistintos} valores de frecuencia distintos &middot;
        Radix: d = ${c.digitosRadix} dígitos &middot; Bucket: ${c.cubetasOcupadas} de ${c.cubetas} cubetas ocupadas (la mayor con ${c.cubetaMasGrande} términos)</p>
        <p>${datos.losTresCoinciden
            ? '<span class="badge-mejor">Validado</span> Los tres algoritmos producen exactamente el mismo orden para los ' + c.terminosDistintos + ' términos.'
            : '<span style="color:#b3552b">Los algoritmos NO produjeron el mismo orden.</span>'}</p>`;

    let html = `<h3>Medición empírica (tiempo real de ejecución, en milisegundos)</h3>
        <table>
            <thead><tr><th>Algoritmo</th><th>Complejidad</th><th>Promedio</th><th>Mediana</th><th>Mínimo</th><th>Máximo</th><th>Desv. est.</th></tr></thead>
            <tbody>`;
    const maxPromedio = Math.max(...datos.resultados.map(r => r.promedioMs));
    for (const r of datos.resultados) {
        html += `<tr>
            <td data-etiqueta="Algoritmo">${escaparHtml(r.algoritmo)} ${r.ordenCorrecto ? "" : '<span style="color:#b3552b">(orden incorrecto)</span>'}
                <div class="barra-fondo"><div class="barra-relleno" style="width:${(r.promedioMs / maxPromedio * 100).toFixed(1)}%"></div></div></td>
            <td data-etiqueta="Complejidad">${escaparHtml(r.complejidad)}</td>
            <td data-etiqueta="Promedio">${r.promedioMs.toFixed(4)}</td>
            <td data-etiqueta="Mediana">${r.medianaMs.toFixed(4)}</td>
            <td data-etiqueta="Mínimo">${r.minimoMs.toFixed(4)}</td>
            <td data-etiqueta="Máximo">${r.maximoMs.toFixed(4)}</td>
            <td data-etiqueta="Desv. est.">${r.desviacionMs.toFixed(4)}</td>
        </tr>`;
    }
    html += `</tbody></table>
        <p class="nota">Cada algoritmo se ejecuta ${datos.resultados[0].repeticiones} veces (después de un calentamiento de la JVM) sobre la misma lista de frecuencias; las repeticiones se intercalan entre algoritmos.</p>`;

    html += `<h3>Validación: 15 términos más frecuentes según cada algoritmo</h3><div class="tablas-ranking">`;
    for (const r of datos.resultados) {
        const maxFrecuencia = r.top.length ? r.top[0].frecuencia : 1;
        let filas = "";
        r.top.forEach((t, i) => {
            filas += `<tr>
                <td>${i + 1}</td>
                <td>${escaparHtml(t.termino)}</td>
                <td class="celda-frecuencia">
                    <div class="barra-fondo"><div class="barra-relleno" style="width:${(t.frecuencia / maxFrecuencia * 100).toFixed(1)}%"></div></div>
                    <span>${t.frecuencia}</span>
                </td>
            </tr>`;
        });
        html += `<div class="tabla-ranking">
            <h4>${escaparHtml(r.algoritmo)}</h4>
            <table><thead><tr><th>#</th><th>Término</th><th>Frecuencia</th></tr></thead><tbody>${filas}</tbody></table>
        </div>`;
    }
    html += `</div>`;
    document.getElementById("resultado-ranking").innerHTML = html;
}

// ------------------------------------------------------------
// Utilidades
// ------------------------------------------------------------
function escaparHtml(texto) {
    if (texto === null || texto === undefined) return "";
    return String(texto)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function acortar(texto, n) {
    return texto.length > n ? texto.substring(0, n - 1) + "…" : texto;
}
