/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : auditoria.js
 * Descripcion   : Logica de la bitacora de auditoria: consulta filtrada
 *                 del historial de operaciones, resumen por accion,
 *                 paginacion y exportacion a CSV (RF-20, RF-22).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

let paginaActual = 0;
let totalPaginas = 1;

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("auditoria.html", ["ADMINISTRADOR"]);
    if (!sesion) {
        return;
    }

    document.getElementById("desde").value = Formato.haceDias(30);
    document.getElementById("hasta").value = Formato.hoy();

    await cargarFiltros();
    await cargar(0);

    document.getElementById("btnFiltrar").addEventListener("click", () => cargar(0));
    document.getElementById("btnLimpiar").addEventListener("click", () => {
        document.getElementById("desde").value = Formato.haceDias(30);
        document.getElementById("hasta").value = Formato.hoy();
        document.getElementById("usuario").value = "";
        document.getElementById("accion").value = "";
        document.getElementById("entidad").value = "";
        cargar(0);
    });

    document.getElementById("btnCsv").addEventListener("click", () => {
        const desde = document.getElementById("desde").value;
        const hasta = document.getElementById("hasta").value;
        Api.descargar(`/api/reportes/auditoria/csv?desde=${desde}&hasta=${hasta}`,
                      "bitacora_auditoria.csv");
        Mensaje.exito("La descarga de la bitacora se inicio. La exportacion tambien queda auditada.");
    });

    document.getElementById("btnAnterior").addEventListener("click", () => {
        if (paginaActual > 0) {
            cargar(paginaActual - 1);
        }
    });
    document.getElementById("btnSiguiente").addEventListener("click", () => {
        if (paginaActual + 1 < totalPaginas) {
            cargar(paginaActual + 1);
        }
    });
});

async function cargarFiltros() {
    try {
        const filtros = (await Api.get("/api/auditoria/filtros")).data;

        document.getElementById("accion").innerHTML = '<option value="">Todas</option>'
            + (filtros.acciones || []).map(a =>
                `<option value="${a}">${Formato.texto(a)}</option>`).join("");

        document.getElementById("entidad").innerHTML = '<option value="">Todos</option>'
            + (filtros.entidades || []).map(e =>
                `<option value="${Formato.texto(e)}">${Formato.texto(e)}</option>`).join("");

        document.getElementById("usuario").innerHTML = '<option value="">Todos</option>'
            + (filtros.usuarios || []).map(u =>
                `<option value="${Formato.texto(u)}">${Formato.texto(u)}</option>`).join("");

    } catch (error) {
        Mensaje.error("No se pudieron cargar los filtros: " + error.message);
    }
}

async function cargar(pagina) {
    const cuerpo = document.getElementById("tablaAuditoria");
    cuerpo.innerHTML = App.filaVacia(12, "Consultando la bitacora...");

    const parametros = new URLSearchParams({
        desde: document.getElementById("desde").value,
        hasta: document.getElementById("hasta").value,
        pagina: pagina,
        tamano: 25
    });

    const usuario = document.getElementById("usuario").value;
    const accion = document.getElementById("accion").value;
    const entidad = document.getElementById("entidad").value;

    if (usuario) { parametros.append("usuario", usuario); }
    if (accion) { parametros.append("accion", accion); }
    if (entidad) { parametros.append("entidad", entidad); }

    try {
        const respuesta = await Api.get("/api/auditoria?" + parametros.toString());
        const datos = respuesta.data;

        paginaActual = datos.pagina;
        totalPaginas = Math.max(datos.totalPaginas, 1);

        document.getElementById("infoPaginas").textContent =
            `${datos.totalRegistros} operacion(es) | pagina ${paginaActual + 1} de ${totalPaginas}`;
        document.getElementById("btnAnterior").disabled = paginaActual === 0;
        document.getElementById("btnSiguiente").disabled = paginaActual + 1 >= totalPaginas;

        const registros = datos.registros || [];
        if (registros.length === 0) {
            cuerpo.innerHTML = App.filaVacia(12, "No hay operaciones que cumplan los filtros indicados.");
            return;
        }

        cuerpo.innerHTML = registros.map(r => `
            <tr>
                <td class="centro">${r.id}</td>
                <td style="white-space:nowrap;">${Formato.fecha(r.fechaHora)}</td>
                <td>${Formato.texto(r.username)}</td>
                <td>${Formato.texto(r.rol || "-")}</td>
                <td class="centro">
                    <span class="etiqueta ${colorAccion(r.accion)}">${Formato.texto(r.accion)}</span>
                </td>
                <td>${Formato.texto(r.entidad)}</td>
                <td class="centro">${Formato.texto(r.entidadId || "-")}</td>
                <td>${Formato.texto(r.descripcion)}</td>
                <td class="detalle-json">${Formato.texto(r.valorAnterior || "-")}</td>
                <td class="detalle-json">${Formato.texto(r.valorNuevo || "-")}</td>
                <td>${Formato.texto(r.direccionIp || "-")}</td>
                <td class="centro">
                    <span class="etiqueta ${r.resultado === "EXITO" ? "verde" : "roja"}">${Formato.texto(r.resultado)}</span>
                </td>
            </tr>`).join("");

        await cargarResumen();

    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(12, error.message);
    }
}

async function cargarResumen() {
    const desde = document.getElementById("desde").value;
    const hasta = document.getElementById("hasta").value;

    try {
        const filas = (await Api.get(`/api/auditoria/resumen?desde=${desde}&hasta=${hasta}`)).data || [];

        if (filas.length === 0) {
            document.getElementById("resumenAcciones").innerHTML =
                '<p style="color:var(--texto-suave);margin:0;">Sin operaciones en el periodo.</p>';
            return;
        }

        document.getElementById("resumenAcciones").innerHTML = filas.map(f => `
            <div class="indicador">
                <div class="etiqueta">${Formato.texto(f.accion)}</div>
                <div class="valor">${f.cantidad}</div>
            </div>`).join("");

    } catch (error) {
        /* El resumen es complementario: si falla, la tabla igual se muestra. */
    }
}

function colorAccion(accion) {
    if (accion === "LOGIN" || accion === "CREAR") return "verde";
    if (accion === "LOGIN_FALLIDO" || accion === "ACCESO_DENEGADO" || accion === "ANULAR") return "roja";
    if (accion === "ELIMINAR" || accion === "DESACTIVAR") return "ambar";
    if (accion === "ACTUALIZAR") return "azul";
    return "gris";
}
