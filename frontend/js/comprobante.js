/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : comprobante.js
 * Descripcion   : Logica del comprobante interno de la venta registrada
 *                 (RF-18, CU-17).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("comprobante.html", ["CAJERO", "ADMINISTRADOR"]);
    if (!sesion) {
        return;
    }

    document.getElementById("btnImprimir").addEventListener("click", () => window.print());

    const id = new URLSearchParams(window.location.search).get("id");
    if (!id) {
        Mensaje.error("No se indico la venta que se desea consultar.");
        return;
    }

    try {
        const venta = (await Api.get("/api/ventas/" + id + "/comprobante")).data;
        dibujar(venta);
    } catch (error) {
        Mensaje.error(error.message);
    }
});

function dibujar(venta) {
    const lineas = (venta.detalles || []).map(d => `
        <tr>
            <td>${Formato.texto(d.producto ? d.producto.nombre : "-")}</td>
            <td class="numero">${d.cantidad}</td>
            <td class="numero">${Formato.soles(d.precioUnitario)}</td>
            <td class="numero">${Formato.soles(d.subtotal)}</td>
        </tr>`).join("");

    document.getElementById("comprobante").innerHTML = `
        <div class="centro-texto">
            <h2>Bodega Armando Tornero</h2>
            <p style="margin:0;color:var(--texto-suave);font-size:12px;">
                Comas - Lima, Peru<br>Comprobante interno de venta</p>
        </div>

        <div class="separador"></div>

        <div class="dato"><span>Comprobante</span><strong>${Formato.texto(venta.numeroComprobante)}</strong></div>
        <div class="dato"><span>Fecha</span><span>${Formato.fecha(venta.fecha)}</span></div>
        <div class="dato"><span>Cajero</span><span>${Formato.texto(venta.usuario ? venta.usuario.nombres + " " + venta.usuario.apellidos : "-")}</span></div>
        <div class="dato"><span>Cliente</span><span>${Formato.texto(venta.clienteNombre || "Cliente varios")}</span></div>
        ${venta.clienteDocumento
            ? `<div class="dato"><span>DNI / RUC</span><span>${Formato.texto(venta.clienteDocumento)}</span></div>` : ""}

        <table>
            <thead>
                <tr><th>Producto</th><th class="numero">Cant.</th><th class="numero">P. unit.</th><th class="numero">Importe</th></tr>
            </thead>
            <tbody>${lineas}</tbody>
        </table>

        <div class="dato"><span>Valor de venta</span><span>${Formato.soles(venta.subtotal)}</span></div>
        <div class="dato"><span>IGV (18 %)</span><span>${Formato.soles(venta.igv)}</span></div>
        <div class="dato" style="font-size:16px;font-weight:700;">
            <span>Total</span><span>${Formato.soles(venta.total)}</span></div>

        <div class="separador"></div>

        <div class="dato"><span>Metodo de pago</span><span>${Formato.texto(venta.metodoPago)}</span></div>
        ${venta.montoRecibido
            ? `<div class="dato"><span>Recibido</span><span>${Formato.soles(venta.montoRecibido)}</span></div>
               <div class="dato"><span>Vuelto</span><span>${Formato.soles(venta.vuelto)}</span></div>` : ""}
        <div class="dato"><span>Estado</span>
            <span class="etiqueta ${venta.estado === "ANULADA" ? "roja" : "verde"}">${Formato.texto(venta.estado)}</span></div>

        <div class="separador"></div>
        <p class="centro-texto" style="font-size:11px;color:var(--texto-suave);margin:0;">
            Documento de control interno. No constituye comprobante de pago electronico.</p>`;
}
