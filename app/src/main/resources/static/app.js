// ============================================================
// Frontend de la aplicacion (vanilla JS, sin frameworks/CDN).
// Consume la API REST implementada en co.edu.uniquindio.bibliometria.web.
// ============================================================

// Estado global de la pagina:
let ARTICULOS = [];               // los 20 articulos recibidos de /api/articulos
let CLUSTERING_RESPUESTA = null;  // respuesta de /api/clustering (null hasta que se pide)
let CLUSTER_ACTUAL = 0;           // dendrograma visible: 0 Single, 1 Complete, 2 Average
let RANKING_CARGADO = false;      // si ya se midio el ranking al menos una vez

// Al terminar de cargar el HTML: activar pestanas y botones, y traer el corpus.
document.addEventListener("DOMContentLoaded", () => {
    configurarNavegacion();
    configurarTabsClustering();
    configurarDocumentos();
    ejecutarConAviso(cargarArticulos, "tabla-articulos");
    document.getElementById("btn-comparar").addEventListener("click", () => ejecutarConAviso(ejecutarComparacion, "resultado-similitud"));
    document.getElementById("btn-medir").addEventListener("click", () => ejecutarConAviso(cargarRanking, "resumen-ranking"));
    document.getElementById("btn-buscar").addEventListener("click", () => ejecutarConAviso(ejecutarBusqueda, "resumen-busqueda"));
    document.getElementById("select-corte").addEventListener("change", () => { if (CLUSTERING_RESPUESTA) dibujarDendrogramaActual(); });
    document.getElementById("input-patron").addEventListener("keydown", e => {
        if (e.key === "Enter") ejecutarConAviso(ejecutarBusqueda, "resumen-busqueda");
    });
});

// Ejecuta una funcion asincrona (una peticion al servidor) y, si falla, por ejemplo porque el servidor
// no responde, muestra el error en el contenedor indicado en lugar de perderlo en silencio.
function ejecutarConAviso(funcionAsincrona, idContenedor) {
    funcionAsincrona().catch(error => mostrarError(idContenedor, error));
}

// Muestra un mensaje de error dentro del contenedor indicado.
function mostrarError(idContenedor, error) {
    const contenedor = document.getElementById(idContenedor);
    if (!contenedor) return;
    const detalle = error && error.message ? error.message : String(error);
    contenedor.innerHTML = `<p style="color:#b3552b">No se pudo completar la operación (${escaparHtml(detalle)}).
        Verifica que el servidor esté en ejecución y vuelve a intentarlo.</p>`;
}

// ------------------------------------------------------------
// Navegacion entre vistas
// ------------------------------------------------------------
// Cada vista tiene su direccion (#inicio, #requerimientos, #clustering, ...): asi funcionan el boton "atras"
// del navegador y la recarga de la pagina. Sin direccion se abre la vista de Inicio.
const VISTA_INICIAL = "inicio";

function configurarNavegacion() {
    // Menu superior, tarjetas de Inicio, tarjetas de los submenus, botones "volver" y R1/R2/R3.
    document.querySelectorAll("[data-vista], [data-ir]").forEach(boton => {
        boton.addEventListener("click", () => irA(boton.dataset.vista || boton.dataset.ir));
    });
    window.addEventListener("hashchange", () => mostrarVista(vistaDeLaDireccion()));
    mostrarVista(vistaDeLaDireccion());
}

function irA(vista) {
    if (location.hash === "#" + vista) {
        mostrarVista(vista);
    } else {
        location.hash = vista;   // dispara "hashchange", que muestra la vista
    }
}

function vistaDeLaDireccion() {
    const nombre = location.hash.replace("#", "");
    return document.getElementById("vista-" + nombre) ? nombre : VISTA_INICIAL;
}

// Muestra una vista y marca en el menu su opcion (o la de su seccion padre: R1, R2 y R3 estan dentro de
// Requerimientos y el ranking dentro de Seguimiento). La primera vez que se abre el clustering o el ranking
// se piden sus datos al servidor (carga perezosa).
function mostrarVista(nombre) {
    const vista = document.getElementById("vista-" + nombre);
    document.querySelectorAll(".vista").forEach(v => v.classList.toggle("activo", v === vista));
    const opcionMenu = vista.dataset.padre || nombre;
    document.querySelectorAll(".menu-btn").forEach(b => b.classList.toggle("activo", b.dataset.vista === opcionMenu));
    window.scrollTo({ top: 0 });
    if (nombre === "clustering" && !CLUSTERING_RESPUESTA) {
        ejecutarConAviso(cargarClustering, "resumen-clustering");
    }
    if (nombre === "ranking" && !RANKING_CARGADO) {
        ejecutarConAviso(cargarRanking, "resumen-ranking");
    }
}

// ------------------------------------------------------------
// Documentos (PDF)
// ------------------------------------------------------------
// "Ver" abre el PDF en un visor dentro de la pagina; si el archivo no esta disponible en esta instalacion
// (los PDF no forman parte del repositorio), se avisa en lugar de mostrar un visor vacio.
function configurarDocumentos() {
    document.querySelectorAll(".documento").forEach(doc => {
        const archivo = doc.dataset.archivo;
        doc.querySelector('[data-accion="ver"]').addEventListener("click", () => ejecutarConAviso(() => verDocumento(doc), "visor-documento"));
        fetch(archivo, { method: "HEAD" })
            .then(r => { if (!r.ok) marcarDocumentoNoDisponible(doc); })
            .catch(() => marcarDocumentoNoDisponible(doc));
    });
}

async function verDocumento(doc) {
    const visor = document.getElementById("visor-documento");
    const titulo = doc.querySelector("strong").textContent;
    document.querySelectorAll(".documento").forEach(d => d.classList.toggle("abierto", d === doc));
    const respuesta = await fetch(doc.dataset.archivo, { method: "HEAD" });
    if (!respuesta.ok) {
        marcarDocumentoNoDisponible(doc);
        visor.innerHTML = `<p class="nota">El documento "${escaparHtml(titulo)}" no está disponible en esta instalación.</p>`;
        return;
    }
    visor.innerHTML = `<div class="visor-cabecera"><strong>${escaparHtml(titulo)}</strong>
        <button class="btn-ver" id="btn-cerrar-visor">Cerrar</button></div>
        <iframe class="visor-pdf" src="${doc.dataset.archivo}" title="${escaparHtml(titulo)}"></iframe>`;
    document.getElementById("btn-cerrar-visor").addEventListener("click", () => {
        visor.innerHTML = "";
        doc.classList.remove("abierto");
    });
    visor.scrollIntoView({ behavior: "smooth", block: "start" });
}

function marcarDocumentoNoDisponible(doc) {
    doc.classList.add("no-disponible");
    doc.querySelector(".documento-texto span").textContent += " — no disponible en esta instalación";
}

// Botones Single / Complete / Average: cambian el dendrograma que se dibuja sin volver a pedir datos.
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
// Pide los 20 articulos a /api/articulos, arma la tabla del corpus y llena los selectores de similitud.
async function cargarArticulos() {
    const respuesta = await fetch("/api/articulos");
    ARTICULOS = await respuesta.json();
    renderizarTablaArticulos();
    poblarSelectores();
}

// Dibuja la tabla del corpus; cada articulo tiene una fila oculta con su abstract.
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

// Muestra u oculta el abstract de un articulo en la tabla del corpus.
function alternarAbstract(id) {
    const fila = document.getElementById("abstract-" + id);
    fila.style.display = fila.style.display === "none" ? "table-row" : "none";
}

// Llena la lista de casillas para elegir dos o mas articulos (arrancan marcados el 1 y el 2, el caso de estudio del documento)
// y el selector de la traza del Requerimiento 3.
function poblarSelectores() {
    const lista = document.getElementById("lista-seleccion-articulos");
    lista.innerHTML = ARTICULOS.map((a, i) => `<label>
        <input type="checkbox" class="check-articulo" value="${a.id}" ${i < 2 ? "checked" : ""}>
        <span>[${a.id}] ${escaparHtml(acortar(a.titulo, 80))}</span>
    </label>`).join("");
    lista.addEventListener("change", actualizarContadorSeleccion);
    document.getElementById("btn-seleccionar-todos").addEventListener("click", () => marcarTodos(true));
    document.getElementById("btn-limpiar-seleccion").addEventListener("click", () => marcarTodos(false));
    actualizarContadorSeleccion();

    const opciones = ARTICULOS.map(a => `<option value="${a.id}">[${a.id}] ${escaparHtml(acortar(a.titulo, 60))}</option>`).join("");
    // La traza arranca en el abstract 16, el del caso de estudio del documento tecnico (seccion 6.5).
    const selectTraza = document.getElementById("select-articulo-traza");
    selectTraza.innerHTML = opciones;
    if (ARTICULOS.some(a => a.id === 16)) selectTraza.value = "16";
}

// ------------------------------------------------------------
// Requerimiento 1: Similitud
// ------------------------------------------------------------
let SIMILITUD_RESPUESTA = null;  // ultima respuesta de /api/similitud?ids=...
let MATRIZ_ACTUAL = 0;            // algoritmo cuya matriz se muestra

// Ids de los articulos marcados, en el orden del corpus.
function idsSeleccionados() {
    return [...document.querySelectorAll(".check-articulo:checked")].map(c => c.value);
}

function actualizarContadorSeleccion() {
    const k = idsSeleccionados().length;
    const pares = k * (k - 1) / 2;
    document.getElementById("contador-seleccion").textContent =
        `${k} artículo${k === 1 ? "" : "s"} seleccionado${k === 1 ? "" : "s"}` + (k >= 2 ? ` · ${pares} par${pares === 1 ? "" : "es"} a comparar` : "");
}

function marcarTodos(marcar) {
    document.querySelectorAll(".check-articulo").forEach(c => c.checked = marcar);
    actualizarContadorSeleccion();
}

// Lee los articulos marcados (dos o mas) y pide a /api/similitud los 6 algoritmos sobre cada par.
async function ejecutarComparacion() {
    const ids = idsSeleccionados();
    const contenedor = document.getElementById("resultado-similitud");

    if (ids.length < 2) {
        contenedor.innerHTML = `<p style="color:#b3552b">Selecciona al menos dos artículos.</p>`;
        return;
    }
    const pares = ids.length * (ids.length - 1) / 2;
    contenedor.innerHTML = `<p class="cargando">Calculando los 6 algoritmos sobre ${pares} par(es)...</p>`;

    const respuesta = await fetch(`/api/similitud?ids=${ids.join(",")}`);
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        contenedor.innerHTML = `<p style="color:#b3552b">${escaparHtml(datos.error || "Error")}</p>`;
        return;
    }
    SIMILITUD_RESPUESTA = datos;
    renderizarComparacion(datos);
}

// Muestra los abstracts seleccionados y, segun cuantos sean, el detalle del unico par o las matrices de similitud.
function renderizarComparacion(datos) {
    const contenedor = document.getElementById("resultado-similitud");
    let html = `<h3>Abstracts seleccionados</h3><div class="abstracts-seleccionados">`;
    for (const a of datos.articulos) {
        html += `<details><summary><strong>[${a.id}]</strong> ${escaparHtml(a.titulo)}</summary><p>${escaparHtml(a.resumen)}</p></details>`;
    }
    html += `</div>`;

    if (datos.articulos.length > 2) {
        html += `<h3>Matriz de similitud por algoritmo</h3>
            <p class="nota">Cada celda es la similitud entre dos abstracts (diagonal = 1). El color se escala entre el mínimo y el máximo
            de cada algoritmo, para que sus rangos (muy distintos entre sí) se puedan comparar. Haz clic en una celda para ver el detalle de ese par.</p>
            <div class="traza-tabs">${datos.matrices.map((m, i) =>
                `<button class="cluster-tab-btn ${i === MATRIZ_ACTUAL ? "activo" : ""}" data-matriz="${i}">${escaparHtml(m.algoritmo)}</button>`).join("")}</div>
            <div id="contenedor-matriz" class="contenedor-matriz"></div>
            <h3>Detalle de un par</h3>
            <div class="selector-articulos"><label>Par a detallar:
                <select id="select-par">${datos.pares.map((p, i) =>
                    `<option value="${i}">[${p.idA}] vs [${p.idB}]</option>`).join("")}</select></label></div>`;
    }
    html += `<div id="detalle-par"></div>`;
    contenedor.innerHTML = html;

    document.querySelectorAll("[data-matriz]").forEach(btn => btn.addEventListener("click", () => {
        MATRIZ_ACTUAL = parseInt(btn.dataset.matriz, 10);
        document.querySelectorAll("[data-matriz]").forEach(b => b.classList.toggle("activo", b === btn));
        renderizarMatriz();
    }));
    const selectPar = document.getElementById("select-par");
    if (selectPar) selectPar.addEventListener("change", () => renderizarResultadosSimilitud(datos.pares[selectPar.value]));
    renderizarMatriz();
    renderizarResultadosSimilitud(datos.pares[0]);
}

// Dibuja la matriz k x k del algoritmo elegido, coloreada segun el valor.
function renderizarMatriz() {
    const contenedor = document.getElementById("contenedor-matriz");
    if (!contenedor || !SIMILITUD_RESPUESTA) return;
    const datos = SIMILITUD_RESPUESTA;
    if (MATRIZ_ACTUAL >= datos.matrices.length) MATRIZ_ACTUAL = 0;
    const matriz = datos.matrices[MATRIZ_ACTUAL];
    const ids = datos.articulos.map(a => a.id);
    const k = ids.length;
    // Minimo y maximo fuera de la diagonal, para escalar el color.
    let min = Infinity, max = -Infinity;
    for (let i = 0; i < k; i++) for (let j = 0; j < k; j++) if (i !== j) {
        min = Math.min(min, matriz.valores[i][j]);
        max = Math.max(max, matriz.valores[i][j]);
    }
    const rgb = matriz.categoria === "ia" ? "179,85,43" : "47,102,144";
    let html = `<table class="matriz-similitud"><tr><th></th>${ids.map(id => `<th>[${id}]</th>`).join("")}</tr>`;
    for (let i = 0; i < k; i++) {
        html += `<tr><th>[${ids[i]}]</th>`;
        for (let j = 0; j < k; j++) {
            const v = matriz.valores[i][j];
            if (i === j) {
                html += `<td style="color:#999">1</td>`;
            } else {
                const t = max > min ? (v - min) / (max - min) : 1;
                html += `<td class="par" data-i="${Math.min(i, j)}" data-j="${Math.max(i, j)}" title="[${ids[i]}] vs [${ids[j]}]"
                    style="background:rgba(${rgb},${(0.08 + 0.72 * t).toFixed(2)});color:${t > 0.6 ? "white" : "inherit"}">${v.toFixed(3)}</td>`;
            }
        }
        html += `</tr>`;
    }
    html += `</table><p class="nota">${escaparHtml(matriz.algoritmo)}: mínimo ${min.toFixed(4)}, máximo ${max.toFixed(4)} entre pares distintos.</p>`;
    contenedor.innerHTML = html;
    contenedor.querySelectorAll("td.par").forEach(td => td.addEventListener("click", () => {
        const idA = ids[td.dataset.i], idB = ids[td.dataset.j];
        const indice = datos.pares.findIndex(p => p.idA === idA && p.idB === idB);
        document.getElementById("select-par").value = indice;
        renderizarResultadosSimilitud(datos.pares[indice]);
    }));
}

// Dibuja una tarjeta por algoritmo con su puntaje, una barra proporcional y el detalle del calculo de un par.
function renderizarResultadosSimilitud(par) {
    const contenedor = document.getElementById("detalle-par");
    const artA = ARTICULOS.find(a => a.id === par.idA);
    const artB = ARTICULOS.find(a => a.id === par.idB);
    let html = `<p><strong>A [${par.idA}]:</strong> ${escaparHtml(artA.titulo)} &nbsp;&nbsp;
                 <strong>B [${par.idB}]:</strong> ${escaparHtml(artB.titulo)}</p>`;
    html += `<div class="tarjetas-resultado">`;
    for (const r of par.resultados) {
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

// Convierte el objeto "detalle" de un resultado en una tabla clave / valor.
function renderizarDetalle(detalle) {
    let filas = "";
    for (const [clave, valor] of Object.entries(detalle)) {
        filas += `<tr><td>${escaparHtml(clave)}</td><td>${formatearValorDetalle(valor)}</td></tr>`;
    }
    return `<table class="detalle-tabla">${filas}</table>`;
}

// Da formato legible a un valor del detalle: decimales a 5 cifras, listas separadas por comas y objetos como lineas clave: valor.
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
// Pide los 3 dendrogramas a /api/clustering y muestra la tabla de correlacion y el dendrograma elegido.
async function cargarClustering() {
    const resumen = document.getElementById("resumen-clustering");
    resumen.innerHTML = `<p class="cargando">Calculando los 3 algoritmos de clustering...</p>`;
    const respuesta = await fetch("/api/clustering");
    CLUSTERING_RESPUESTA = await respuesta.json();
    renderizarResumenClustering();
    dibujarDendrogramaActual();
}

// Tabla con la correlacion cofenetica de cada criterio, marcando el mas coherente.
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
    // Segunda metrica: silueta de los cortes del dendrograma en k grupos.
    const ks = CLUSTERING_RESPUESTA.resultados[0].cortes.map(c => c.k);
    let filasCortes = "";
    for (const r of CLUSTERING_RESPUESTA.resultados) {
        filasCortes += `<tr><td>${escaparHtml(r.nombreEnlace)}</td>${r.cortes.map(c =>
            `<td>${c.silueta.toFixed(3)}<br><span class="nota">[${c.tamanos.join(", ")}]</span></td>`).join("")}</tr>`;
    }
    resumen.innerHTML = `
        <p>Métrica de evaluación interna 1: <strong>Coeficiente de Correlación Cofenética</strong>
        (Pearson entre las distancias originales y las distancias cofenéticas del dendrograma; 1 = el dendrograma conserva
        perfectamente las distancias).</p>
        <table>
            <thead><tr><th>Algoritmo de enlace</th><th>Correlación cofenética</th></tr></thead>
            <tbody>${filas}</tbody>
        </table>
        <p style="margin-top:18px">Métrica de evaluación interna 2: <strong>Coeficiente de silueta</strong> de los grupos que se obtienen al
        cortar cada dendrograma en k grupos (1 = grupos compactos y bien separados; 0 = sin separación; por debajo de 0.25 no
        hay una estructura de grupos sustancial). Debajo de cada valor, el tamaño de los grupos.</p>
        <table class="tabla-cortes">
            <thead><tr><th>Algoritmo de enlace</th>${ks.map(k => `<th>k = ${k}</th>`).join("")}</tr></thead>
            <tbody>${filasCortes}</tbody>
        </table>`;
}

// Dibuja el dendrograma del criterio seleccionado (Single, Complete o Average).
function dibujarDendrogramaActual() {
    const contenedor = document.getElementById("contenedor-dendrograma");
    if (!CLUSTERING_RESPUESTA) return;
    const resultado = CLUSTERING_RESPUESTA.resultados[CLUSTER_ACTUAL];
    const k = parseInt(document.getElementById("select-corte").value, 10);
    const corte = resultado.cortes.find(c => c.k === k) || null;
    // La linea de corte va entre la fusion n-k (la mas alta que se conserva) y la n-k+1 (la mas baja que se deshace).
    let alturaCorte = null;
    if (corte) {
        const pasos = resultado.pasos;
        const conservada = pasos[pasos.length - k].altura;
        const deshecha = pasos[pasos.length - k + 1].altura;
        alturaCorte = (conservada + deshecha) / 2;
    }
    contenedor.innerHTML = "";
    contenedor.appendChild(construirSvgDendrograma(resultado.dendrograma, corte, alturaCorte));
    renderizarFusiones(resultado, corte);
}

// Debajo del dendrograma: los grupos del corte elegido y la lista de las 19 fusiones en orden.
function renderizarFusiones(resultado, corte) {
    const titulo = id => { const a = ARTICULOS.find(x => x.id === id); return a ? `[${id}] ${acortar(a.titulo, 50)}` : `[${id}]`; };
    let html = "";
    if (corte) {
        html += `<details open><summary><strong>${corte.k} grupos</strong> (silueta = ${corte.silueta.toFixed(3)})</summary><ul>`;
        corte.grupos.forEach((g, i) => {
            html += `<li><span style="color:${COLORES_GRUPO[i % COLORES_GRUPO.length]};font-weight:700">Grupo ${i + 1}</span>
                (${g.length}): ${g.map(id => escaparHtml(titulo(id))).join("; ")}</li>`;
        });
        html += `</ul></details>`;
    }
    html += `<details><summary>Ver las ${resultado.pasos.length} fusiones en el orden en que ocurrieron</summary>
        <table><thead><tr><th>#</th><th>Altura (distancia)</th><th>Cluster A</th><th>Cluster B</th><th>Tamaño</th></tr></thead><tbody>
        ${resultado.pasos.map((p, i) => `<tr><td>${i + 1}</td><td>${p.altura.toFixed(4)}</td>
            <td>${p.miembrosA.map(id => `[${id}]`).join(" ")}</td><td>${p.miembrosB.map(id => `[${id}]`).join(" ")}</td>
            <td>${p.tamanoResultante}</td></tr>`).join("")}
        </tbody></table></details>`;
    document.getElementById("detalle-dendrograma").innerHTML = html;
}

const COLORES_GRUPO = ["#2f6690", "#b3552b", "#1e7d4f", "#8a4fbf", "#c48a00", "#d0417e"];

// Construye el dibujo SVG de un dendrograma: las hojas se reparten de izquierda a derecha, cada fusion se dibuja a una altura proporcional a su distancia y se une con lineas en forma de U.
function construirSvgDendrograma(raiz, corte, alturaCorte) {
    // Color de cada hoja segun el grupo al que pertenece en el corte (si hay corte).
    const grupoDe = {};
    if (corte) corte.grupos.forEach((g, i) => g.forEach(id => grupoDe[id] = i));
    const espacioX = 55;
    const margenIzq = 30;
    const margenDer = 30;
    const margenSup = 20;
    const alturaPlot = 320;
    const espacioEtiquetas = 130;

    let siguienteIndice = 0;
    // Recorre el arbol en orden y asigna la coordenada x: las hojas en columnas consecutivas y cada nodo interno en el punto medio de sus hijos.
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
    // Convierte una altura (distancia) en coordenada vertical: altura 0 abajo, altura maxima arriba.
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

    // Agrega una linea recta al SVG.
    function linea(x1, y1, x2, y2) {
        const l = document.createElementNS(NS, "line");
        l.setAttribute("x1", x1); l.setAttribute("y1", y1);
        l.setAttribute("x2", x2); l.setAttribute("y2", y2);
        l.setAttribute("class", "linea-dendro");
        svg.appendChild(l);
    }

    // Dibuja recursivamente un nodo: las hojas con su etiqueta girada y un punto; los nodos internos con dos lineas verticales y una horizontal que une a sus hijos.
    function dibujar(nodo) {
        if (nodo.esHoja) {
            const texto = document.createElementNS(NS, "text");
            texto.setAttribute("x", 0);
            texto.setAttribute("y", 0);
            texto.setAttribute("class", "etiqueta-hoja");
            texto.setAttribute("transform", `translate(${nodo._x + 3},${y(0) + 5}) rotate(65)`);
            texto.textContent = nodo.etiqueta || ("Art. " + nodo.articuloId);
            const color = corte ? COLORES_GRUPO[grupoDe[nodo.articuloId] % COLORES_GRUPO.length] : "#1f3a5f";
            if (corte) texto.setAttribute("style", `fill:${color};font-weight:600`);
            svg.appendChild(texto);

            const punto = document.createElementNS(NS, "circle");
            punto.setAttribute("cx", nodo._x);
            punto.setAttribute("cy", y(0));
            punto.setAttribute("r", corte ? 4 : 2.5);
            punto.setAttribute("fill", color);
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

    // Linea horizontal del corte: todo lo que queda debajo de ella forma un grupo.
    if (alturaCorte !== null && alturaCorte !== undefined) {
        const yc = y(alturaCorte);
        const l = document.createElementNS(NS, "line");
        l.setAttribute("x1", 0); l.setAttribute("x2", ancho);
        l.setAttribute("y1", yc); l.setAttribute("y2", yc);
        l.setAttribute("class", "linea-corte");
        svg.appendChild(l);
        const t = document.createElementNS(NS, "text");
        t.setAttribute("x", 4); t.setAttribute("y", yc - 4);
        t.setAttribute("class", "texto-corte");
        t.textContent = `corte en ${corte.k} grupos (altura ${alturaCorte.toFixed(4)})`;
        svg.appendChild(t);
    }

    return svg;
}

// ------------------------------------------------------------
// Requerimiento 3: Busqueda de patrones (KMP, Boyer-Moore, Rabin-Karp)
// ------------------------------------------------------------
let BUSQUEDA_RESPUESTA = null;  // ultima respuesta de /api/busqueda
let TRAZA_ACTUAL = 0;           // algoritmo cuya traza se muestra: 0 KMP, 1 Boyer-Moore, 2 Rabin-Karp
const PASOS_VISIBLES = 60;      // pasos de traza que se dibujan

// Lee el patron y las opciones, y pide a /api/busqueda las ocurrencias, tiempos, estructuras y la traza del abstract elegido.
async function ejecutarBusqueda() {
    const patron = document.getElementById("input-patron").value;
    const resumen = document.getElementById("resumen-busqueda");
    if (!patron) {
        resumen.innerHTML = `<p style="color:#b3552b">Escribe un patrón de búsqueda.</p>`;
        return;
    }
    const parametros = new URLSearchParams({
        patron,
        mayusculas: document.getElementById("check-mayusculas").checked,
        modulo: document.getElementById("select-modulo").value,
        traza: document.getElementById("select-articulo-traza").value,
    });
    resumen.innerHTML = `<p class="cargando">Buscando con los 3 algoritmos y midiendo tiempos...</p>`;
    document.getElementById("resultado-busqueda").innerHTML = "";
    const respuesta = await fetch(`/api/busqueda?${parametros}`);
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        resumen.innerHTML = `<p style="color:#b3552b">${escaparHtml(datos.error || "Error")}</p>`;
        return;
    }
    BUSQUEDA_RESPUESTA = datos;
    renderizarBusqueda(datos);
}

// Dibuja el resumen, la tabla comparativa, las estructuras auxiliares, la traza y las ocurrencias.
function renderizarBusqueda(datos) {
    const total = datos.algoritmos[0].ocurrencias;
    document.getElementById("resumen-busqueda").innerHTML = `
        <p>Patrón <code>${escaparHtml(datos.patron)}</code> (m = ${datos.longitudPatron}) en ${datos.articulos} abstracts
        (n = ${datos.caracteresTotales} caracteres en total): <strong>${total}</strong> ocurrencias en
        <strong>${datos.ocurrencias.length}</strong> artículos.</p>
        <p>${datos.losTresCoinciden
            ? '<span class="badge-mejor">Validado</span> Los tres algoritmos encontraron exactamente las mismas posiciones.'
            : '<span style="color:#b3552b">Los algoritmos NO encontraron las mismas posiciones.</span>'}</p>`;

    let html = `<h3>Comparación: preprocesamiento frente a búsqueda</h3>
        <table>
            <thead><tr><th>Algoritmo</th><th>Complejidad</th><th>Preproc. (ms)</th><th>Búsqueda (ms)</th>
            <th>Comparaciones</th><th>Comp. / carácter</th><th>Alineamientos</th><th>Colisiones</th></tr></thead><tbody>`;
    const maxBusqueda = Math.max(...datos.algoritmos.map(a => a.busquedaMedianaMs));
    for (const a of datos.algoritmos) {
        html += `<tr>
            <td data-etiqueta="Algoritmo">${escaparHtml(a.algoritmo)}
                <div class="barra-fondo"><div class="barra-relleno" style="width:${(a.busquedaMedianaMs / maxBusqueda * 100).toFixed(1)}%"></div></div></td>
            <td data-etiqueta="Complejidad">Pre: ${escaparHtml(a.complejidadPreprocesamiento)}<br>Búsq.: ${escaparHtml(a.complejidadBusqueda)}</td>
            <td data-etiqueta="Preproc. (ms)">${a.preprocesamientoMedianaMs.toFixed(4)}</td>
            <td data-etiqueta="Búsqueda (ms)">${a.busquedaMedianaMs.toFixed(4)}</td>
            <td data-etiqueta="Comparaciones">${a.comparaciones}</td>
            <td data-etiqueta="Comp. / carácter">${a.comparacionesPorCaracter.toFixed(3)}</td>
            <td data-etiqueta="Alineamientos">${a.alineamientos}</td>
            <td data-etiqueta="Colisiones">${a.colisiones === undefined ? "-" : `${a.colisiones} de ${a.coincidenciasHash} coincidencias de hash`}</td>
        </tr>`;
    }
    html += `</tbody></table>
        <p class="nota">Mediana de ${datos.repeticiones} repeticiones intercaladas, después de un calentamiento de la JVM. El preprocesamiento se hace
        una vez por patrón; la búsqueda recorre los 20 abstracts.</p>`;

    html += `<h3>Estructuras auxiliares del patrón</h3><div class="estructuras">`;
    for (const a of datos.algoritmos) html += renderizarEstructura(a.estructura);
    html += `</div>`;

    if (datos.traza && datos.traza.algoritmos) {
        const art = ARTICULOS.find(x => x.id === datos.traza.articuloId);
        html += `<h3>Traza paso a paso sobre el abstract [${datos.traza.articuloId}]</h3>
            <p class="nota">${art ? escaparHtml(art.titulo) : ""} (n = ${datos.traza.longitudTexto} caracteres). Cada paso coloca el patrón en la
            posición s del texto. El color compara toda la ventana (verde = coincide, rojo = no coincide); la columna
            Comp. indica cuántos de esos caracteres comparó realmente el algoritmo.</p>
            <div class="traza-tabs">${datos.traza.algoritmos.map((a, i) =>
                `<button class="cluster-tab-btn ${i === TRAZA_ACTUAL ? "activo" : ""}" data-traza="${i}">${escaparHtml(a.algoritmo)} (${a.totalPasos} pasos)</button>`
            ).join("")}</div>
            <div id="contenedor-traza" class="contenedor-traza"></div>`;
    }

    html += `<h3>Ocurrencias por artículo</h3>`;
    if (datos.ocurrencias.length === 0) html += `<p>No se encontraron ocurrencias.</p>`;
    for (const o of datos.ocurrencias) {
        html += `<details><summary><strong>[${o.articuloId}]</strong> ${escaparHtml(acortar(o.titulo, 90))} &mdash; ${o.total} ocurrencia(s)</summary>
            ${o.fragmentos.map(f => `<p class="fragmento">pos. ${f.posicion}: …${escaparHtml(f.antes)}<span class="resaltado">${escaparHtml(f.coincidencia)}</span>${escaparHtml(f.despues)}…</p>`).join("")}
        </details>`;
    }
    document.getElementById("resultado-busqueda").innerHTML = html;

    document.querySelectorAll("[data-traza]").forEach(btn => {
        btn.addEventListener("click", () => {
            TRAZA_ACTUAL = parseInt(btn.dataset.traza, 10);
            document.querySelectorAll("[data-traza]").forEach(b => b.classList.toggle("activo", b === btn));
            renderizarTraza();
        });
    });
    renderizarTraza();
}

// Muestra la estructura auxiliar de un algoritmo: tabla pi de KMP, tablas de Boyer-Moore o el calculo del hash de Rabin-Karp.
function renderizarEstructura(e) {
    const patron = [...e.patron];
    const filaIndices = arr => arr.map((_, i) => `<th>${i}</th>`).join("");
    let cuerpo = "";
    if (e.pi) {
        cuerpo = `<table class="tabla-indices"><tr><th>q</th>${filaIndices(patron)}</tr>
            <tr><th>P[q]</th>${patron.map(c => `<td>${escaparHtml(c)}</td>`).join("")}</tr>
            <tr><th>pi[q]</th>${e.pi.map(v => `<td>${v}</td>`).join("")}</tr></table>`;
    } else if (e.sufijoBueno) {
        cuerpo = `<p class="nota">Carácter malo (última posición en el patrón):</p>
            <table class="tabla-indices"><tr>${Object.keys(e.caracterMalo).map(k => `<th>${escaparHtml(k)}</th>`).join("")}</tr>
            <tr>${Object.values(e.caracterMalo).map(v => `<td>${v}</td>`).join("")}</tr></table>
            <p class="nota">Sufijo bueno (desplazamiento si falla en j = índice − 1; índice 0 = tras una ocurrencia):</p>
            <table class="tabla-indices"><tr><th>i</th>${filaIndices(e.sufijoBueno)}</tr>
            <tr><th>P[i]</th>${patron.map(c => `<td>${escaparHtml(c)}</td>`).join("")}<td>-</td></tr>
            <tr><th>borde[i]</th>${e.borde.map(v => `<td>${v}</td>`).join("")}</tr>
            <tr><th>sufijoBueno[i]</th>${e.sufijoBueno.map(v => `<td>${v}</td>`).join("")}</tr></table>`;
    } else if (e.horner) {
        cuerpo = `<p class="nota">Base d = ${e.base}, módulo q = ${e.modulo}. ${escaparHtml(e.potencia)}.</p>
            <table class="tabla-indices"><tr><th>Carácter</th><th>Regla de Horner</th></tr>
            ${Object.entries(e.horner).map(([k, v]) => `<tr><td>${escaparHtml(k)}</td><td>${escaparHtml(v)}</td></tr>`).join("")}</table>
            <p class="nota">hash(P) = <strong>${e.hashPatron}</strong></p>`;
    }
    return `<div class="estructura"><h4>${escaparHtml(e.nombre)}</h4>${cuerpo}</div>`;
}

// Dibuja los primeros pasos de la traza del algoritmo elegido: el texto alrededor de la posicion s y, debajo, el patron alineado.
function renderizarTraza() {
    const contenedor = document.getElementById("contenedor-traza");
    if (!contenedor || !BUSQUEDA_RESPUESTA) return;
    const datos = BUSQUEDA_RESPUESTA;
    const traza = datos.traza.algoritmos[TRAZA_ACTUAL];
    const art = ARTICULOS.find(x => x.id === datos.traza.articuloId);
    let texto = art.resumen;
    let patron = datos.patron;
    if (!datos.distinguirMayusculas) {
        texto = texto.toLowerCase();
        patron = patron.toLowerCase();
    }
    const m = patron.length;
    const margen = 6;
    let filas = "";
    for (const p of traza.pasos.slice(0, PASOS_VISIBLES)) {
        const s = p.posicion;
        const inicio = Math.max(0, s - margen);
        let lineaTexto = "", lineaPatron = "";
        for (let i = inicio; i < Math.min(texto.length, s + m + margen); i++) {
            const c = texto[i];
            const j = i - s;
            if (j >= 0 && j < m) {
                const clase = c === patron[j] ? "ch-ok" : "ch-mal";
                lineaTexto += `<span class="${clase}">${escaparHtml(c)}</span>`;
                lineaPatron += `<span class="${clase}">${escaparHtml(patron[j])}</span>`;
            } else {
                lineaTexto += escaparHtml(c);
                lineaPatron += " ";
            }
        }
        filas += `<tr>
            <td>${p.paso}</td><td>s = ${s}</td>
            <td class="traza-paso">${lineaTexto}<br>${lineaPatron}</td>
            <td>${p.comparaciones}</td><td>${escaparHtml(p.evento)}</td><td>+${p.desplazamiento}</td>
        </tr>`;
    }
    contenedor.innerHTML = `
        <p class="nota">Posiciones encontradas: ${traza.posiciones.length ? traza.posiciones.join(", ") : "ninguna"} &middot;
        ${traza.comparaciones} comparaciones de caracteres &middot; se muestran ${Math.min(PASOS_VISIBLES, traza.pasos.length)} de ${traza.totalPasos} pasos.</p>
        <table><thead><tr><th>Paso</th><th>Posición</th><th>Texto / patrón alineado</th><th>Comp.</th><th>Evento</th><th>Desplaz.</th></tr></thead>
        <tbody>${filas}</tbody></table>`;
}

// ------------------------------------------------------------
// Seguimiento: Ranking de terminos (Counting, Radix y Bucket Sort)
// ------------------------------------------------------------
// Pide a /api/ranking el top 15 con el numero de repeticiones elegido y muestra los resultados.
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

// Dibuja el resumen del corpus (n, k, d, cubetas), la tabla de tiempos y los tres top 15 lado a lado.
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
// Reemplaza los caracteres especiales de HTML para mostrar texto sin que el navegador lo interprete como codigo.
function escaparHtml(texto) {
    if (texto === null || texto === undefined) return "";
    return String(texto)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

// Recorta un texto a n caracteres, agregando puntos suspensivos si era mas largo.
function acortar(texto, n) {
    return texto.length > n ? texto.substring(0, n - 1) + "…" : texto;
}
