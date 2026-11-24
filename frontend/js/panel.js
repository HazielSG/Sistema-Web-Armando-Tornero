/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : panel.js
 * Descripcion   : Logica de la pantalla del panel principal: indicadores,
 *                 alertas de stock bajo (RF-12) y ultimas operaciones de
 *                 la bitacora de auditoria.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("panel.html", null);
    if (!sesion) {
        return;
    }

    await cargarIndicadores(sesion);
    await cargarStockBajo();

    if (sesion.rol === "ADMINISTRADOR") {
        document.getElementById("tarjetaAuditoria").style.display = "";
        await cargarAuditoria();
    }
});

async function cargarIndicadores(sesion) {
    try {
        const resumen = (await Api.get("/api/dashboard/resumen")).data;

        const tarjetas = [
            { etiqueta: "Productos activos", valor: resumen.productosRegistrados, clase: "" },
            { etiqueta: "Stock bajo", valor: resumen.productosStockBajo,
              clase: resumen.productosStockBajo > 0 ? "alerta" : "exito" },
            { etiqueta: "Ventas de hoy", valor: resumen.ventasDelDia, clase: "" },
            { etiqueta: "Vendido hoy", valor: Formato.soles(resumen.totalVendidoDia), clase: "exito" },
            { etiqueta: "Vendido en el mes", valor: Formato.soles(resumen.totalVendidoMes), clase: "" }
        ];

        if (sesion.rol === "ADMINISTRADOR") {
            tarjetas.push({ etiqueta: "Usuarios activos", valor: resumen.usuariosActivos, clase: "" });
            tarjetas.push({ etiqueta: "Operaciones auditadas hoy",
                            valor: resumen.operacionesAuditadasHoy, clase: "" });
        }

        document.getElementById("indicadores").innerHTML = tarjetas.map(t => `
            <div class="indicador ${t.clase}">
                <div class="etiqueta">${t.etiqueta}</div>
                <div class="valor">${Formato.texto(t.valor)}</div>
            </div>`).join("");

    } catch (error) {
        Mensaje.error(error.message);
    }
}

async function cargarStockBajo() {
    const cuerpo = document.getElementById("tablaStockBajo");
    try {
        const productos = (await Api.get("/api/productos/stock-bajo")).data || [];
        if (productos.length === 0) {
            cuerpo.innerHTML = App.filaVacia(6, "Todos los productos tienen existencias por encima del minimo.");
            return;
        }
        cuerpo.innerHTML = productos.map(p => `
            <tr>
                <td>${Formato.texto(p.codigo)}</td>
                <td>${Formato.texto(p.nombre)}</td>
                <td>${Formato.texto(p.categoria ? p.categoria.nombre : "-")}</td>
                <td class="numero">${p.stockActual}</td>
                <td class="numero">${p.stockMinimo}</td>
                <td class="centro"><span class="etiqueta ${p.stockActual === 0 ? "roja" : "ambar"}">
                    ${p.stockActual === 0 ? "SIN STOCK" : "STOCK BAJO"}</span></td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(6, error.message);
    }
}

async function cargarAuditoria() {
    const cuerpo = document.getElementById("tablaAuditoria");
    try {
        const registros = (await Api.get("/api/auditoria/ultimas")).data || [];
        if (registros.length === 0) {
            cuerpo.innerHTML = App.filaVacia(5, "Todavia no hay operaciones registradas.");
            return;
        }
        cuerpo.innerHTML = registros.map(r => `
            <tr>
                <td>${Formato.fecha(r.fechaHora)}</td>
                <td>${Formato.texto(r.username)}</td>
                <td><span class="etiqueta ${colorAccion(r.accion)}">${Formato.texto(r.accion)}</span></td>
                <td>${Formato.texto(r.entidad)}</td>
                <td>${Formato.texto(r.descripcion)}</td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(5, error.message);
    }
}

function colorAccion(accion) {
    if (accion === "LOGIN" || accion === "CREAR") return "verde";
    if (accion === "LOGIN_FALLIDO" || accion === "ACCESO_DENEGADO" || accion === "ANULAR") return "roja";
    if (accion === "ELIMINAR" || accion === "DESACTIVAR") return "ambar";
    if (accion === "ACTUALIZAR") return "azul";
    return "gris";
}
