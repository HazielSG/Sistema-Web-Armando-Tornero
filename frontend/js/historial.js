/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : historial.js
 * Descripcion   : Logica de la consulta y el filtrado del historial de
 *                 ventas, con acceso al comprobante y a la anulacion
 *                 (RF-20, CU-06, CU-07).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

let sesionActual = null;

document.addEventListener("DOMContentLoaded", async () => {
    sesionActual = await App.iniciar("historial.html", ["CAJERO", "ADMINISTRADOR"]);
    if (!sesionActual) {
        return;
    }

    document.getElementById("desde").value = Formato.haceDias(7);
    document.getElementById("hasta").value = Formato.hoy();

    await cargar(document.getElementById("desde").value, document.getElementById("hasta").value);

    document.getElementById("btnFiltrar").addEventListener("click", () =>
        cargar(document.getElementById("desde").value, document.getElementById("hasta").value));

    document.getElementById("btnHoy").addEventListener("click", () => {
        document.getElementById("desde").value = Formato.hoy();
        document.getElementById("hasta").value = Formato.hoy();
        cargar(Formato.hoy(), Formato.hoy());
    });
});

async function cargar(desde, hasta) {
    const cuerpo = document.getElementById("tablaVentas");
    cuerpo.innerHTML = App.filaVacia(8, "Cargando ventas...");

    try {
        const ventas = (await Api.get(`/api/ventas?desde=${desde}&hasta=${hasta}`)).data || [];
        dibujarIndicadores(ventas);

        if (ventas.length === 0) {
            cuerpo.innerHTML = App.filaVacia(8, "No hay ventas registradas en el periodo indicado.");
            return;
        }

        cuerpo.innerHTML = ventas.map(v => `
            <tr>
                <td>${Formato.texto(v.numeroComprobante)}</td>
                <td>${Formato.fecha(v.fecha)}</td>
                <td>${Formato.texto(v.clienteNombre)}</td>
                <td>${Formato.texto(v.usuario ? v.usuario.username : "-")}</td>
                <td class="centro">${Formato.texto(v.metodoPago)}</td>
                <td class="numero">${Formato.soles(v.total)}</td>
                <td class="centro">
                    <span class="etiqueta ${v.estado === "ANULADA" ? "roja" : "verde"}">${Formato.texto(v.estado)}</span>
                </td>
                <td class="centro" style="white-space:nowrap;">
                    <a href="comprobante.html?id=${v.id}"><button class="secundario pequeno">Comprobante</button></a>
                    ${(sesionActual.rol === "ADMINISTRADOR" && v.estado !== "ANULADA")
                        ? `<button class="peligro pequeno" data-anular="${v.id}">Anular</button>` : ""}
                </td>
            </tr>`).join("");

        cuerpo.querySelectorAll("[data-anular]").forEach(boton =>
            boton.addEventListener("click", () => anular(boton.dataset.anular, desde, hasta)));

    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(8, error.message);
    }
}

function dibujarIndicadores(ventas) {
    const emitidas = ventas.filter(v => v.estado === "EMITIDA");
    const total = emitidas.reduce((suma, v) => suma + Number(v.total), 0);
    const promedio = emitidas.length ? total / emitidas.length : 0;

    const tarjetas = [
        { etiqueta: "Ventas emitidas", valor: emitidas.length, clase: "" },
        { etiqueta: "Ventas anuladas", valor: ventas.length - emitidas.length,
          clase: ventas.length - emitidas.length ? "alerta" : "" },
        { etiqueta: "Total vendido", valor: Formato.soles(total), clase: "exito" },
        { etiqueta: "Ticket promedio", valor: Formato.soles(promedio), clase: "" }
    ];

    document.getElementById("indicadores").innerHTML = tarjetas.map(t => `
        <div class="indicador ${t.clase}">
            <div class="etiqueta">${t.etiqueta}</div>
            <div class="valor">${t.valor}</div>
        </div>`).join("");
}

async function anular(id, desde, hasta) {
    const motivo = prompt("Indique el motivo de la anulacion (queda registrado en la auditoria):");
    if (motivo === null) {
        return;
    }
    if (!motivo.trim()) {
        Mensaje.error("Debe indicar el motivo de la anulacion.");
        return;
    }

    try {
        const respuesta = await Api.put("/api/ventas/" + id + "/anular", { motivo: motivo.trim() });
        Mensaje.aviso(respuesta.mensaje);
        await cargar(desde, hasta);
    } catch (error) {
        Mensaje.error(error.message);
    }
}
