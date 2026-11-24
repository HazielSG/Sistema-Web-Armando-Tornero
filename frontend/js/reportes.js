/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : reportes.js
 * Descripcion   : Logica de los reportes de ventas, productos mas
 *                 vendidos e inventario, con exportacion a CSV
 *                 (RF-21, RF-22, CU-08, CU-09).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("reportes.html", ["ADMINISTRADOR"]);
    if (!sesion) {
        return;
    }

    document.getElementById("desde").value = Formato.haceDias(30);
    document.getElementById("hasta").value = Formato.hoy();

    await generar();

    document.getElementById("btnGenerar").addEventListener("click", generar);

    document.getElementById("btnCsvVentas").addEventListener("click", () => {
        const desde = document.getElementById("desde").value;
        const hasta = document.getElementById("hasta").value;
        Api.descargar(`/api/reportes/ventas/csv?desde=${desde}&hasta=${hasta}`, "reporte_ventas.csv");
        Mensaje.exito("La descarga del reporte de ventas se inicio. La exportacion queda registrada en la auditoria.");
    });

    document.getElementById("btnCsvInventario").addEventListener("click", () => {
        Api.descargar("/api/reportes/inventario/csv", "reporte_inventario.csv");
        Mensaje.exito("La descarga del reporte de inventario se inicio.");
    });
});

async function generar() {
    const desde = document.getElementById("desde").value;
    const hasta = document.getElementById("hasta").value;

    await Promise.all([
        cargarVentas(desde, hasta),
        cargarMasVendidos(desde, hasta),
        cargarInventario()
    ]);
}

async function cargarVentas(desde, hasta) {
    const cuerpo = document.getElementById("tablaVentas");
    try {
        const filas = (await Api.get(`/api/reportes/ventas?desde=${desde}&hasta=${hasta}`)).data || [];
        dibujarIndicadores(filas);

        if (filas.length === 0) {
            cuerpo.innerHTML = App.filaVacia(10, "No hay ventas registradas en el periodo.");
            return;
        }

        cuerpo.innerHTML = filas.map(f => `
            <tr>
                <td>${Formato.texto(f.comprobante)}</td>
                <td>${Formato.texto(f.fecha)}</td>
                <td>${Formato.texto(f.cajero)}</td>
                <td>${Formato.texto(f.cliente)}</td>
                <td class="numero">${f.productos}</td>
                <td class="numero">${Formato.soles(f.subtotal)}</td>
                <td class="numero">${Formato.soles(f.igv)}</td>
                <td class="numero">${Formato.soles(f.total)}</td>
                <td class="centro">${Formato.texto(f.metodoPago)}</td>
                <td class="centro">
                    <span class="etiqueta ${f.estado === "ANULADA" ? "roja" : "verde"}">${Formato.texto(f.estado)}</span>
                </td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(10, error.message);
    }
}

function dibujarIndicadores(filas) {
    const emitidas = filas.filter(f => f.estado === "EMITIDA");
    const total = emitidas.reduce((suma, f) => suma + Number(f.total), 0);
    const igv = emitidas.reduce((suma, f) => suma + Number(f.igv), 0);

    const tarjetas = [
        { etiqueta: "Ventas del periodo", valor: emitidas.length, clase: "" },
        { etiqueta: "Total vendido", valor: Formato.soles(total), clase: "exito" },
        { etiqueta: "IGV del periodo", valor: Formato.soles(igv), clase: "" },
        { etiqueta: "Ticket promedio",
          valor: Formato.soles(emitidas.length ? total / emitidas.length : 0), clase: "" },
        { etiqueta: "Ventas anuladas", valor: filas.length - emitidas.length,
          clase: filas.length - emitidas.length ? "alerta" : "" }
    ];

    document.getElementById("indicadores").innerHTML = tarjetas.map(t => `
        <div class="indicador ${t.clase}">
            <div class="etiqueta">${t.etiqueta}</div>
            <div class="valor" style="font-size:${String(t.valor).length > 9 ? "19px" : "24px"};">${t.valor}</div>
        </div>`).join("");
}

async function cargarMasVendidos(desde, hasta) {
    const cuerpo = document.getElementById("tablaMasVendidos");
    try {
        const filas = (await Api.get(
            `/api/reportes/productos-mas-vendidos?desde=${desde}&hasta=${hasta}`)).data || [];

        if (filas.length === 0) {
            cuerpo.innerHTML = App.filaVacia(4, "No hay productos vendidos en el periodo.");
            return;
        }

        cuerpo.innerHTML = filas.slice(0, 15).map((f, indice) => `
            <tr>
                <td class="centro">${indice + 1}</td>
                <td>${Formato.texto(f.producto)}</td>
                <td class="numero">${f.unidades}</td>
                <td class="numero">${Formato.soles(f.importe)}</td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(4, error.message);
    }
}

async function cargarInventario() {
    const cuerpo = document.getElementById("tablaInventario");
    try {
        const filas = (await Api.get("/api/reportes/inventario")).data || [];

        if (filas.length === 0) {
            cuerpo.innerHTML = App.filaVacia(7, "No hay productos registrados.");
            return;
        }

        cuerpo.innerHTML = filas.map(f => `
            <tr>
                <td>${Formato.texto(f.codigo)}</td>
                <td>${Formato.texto(f.producto)}</td>
                <td>${Formato.texto(f.categoria)}</td>
                <td class="numero">${Formato.soles(f.precioVenta)}</td>
                <td class="numero">${f.stockActual}</td>
                <td class="numero">${f.stockMinimo}</td>
                <td class="centro">
                    <span class="etiqueta ${f.alerta === "STOCK BAJO" ? "ambar" : "verde"}">${Formato.texto(f.alerta)}</span>
                </td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(7, error.message);
    }
}
