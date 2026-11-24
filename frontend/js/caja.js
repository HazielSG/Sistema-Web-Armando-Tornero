/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : caja.js
 * Descripcion   : Logica de la apertura y el cierre de caja del turno:
 *                 resumen de las operaciones y diferencia de efectivo
 *                 (RF-19, CU-18).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

let turnoActual = null;

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("caja.html", ["CAJERO", "ADMINISTRADOR"]);
    if (!sesion) {
        return;
    }

    document.getElementById("formApertura").addEventListener("submit", aperturar);
    document.getElementById("formCierre").addEventListener("submit", cerrar);
    document.getElementById("montoDeclarado").addEventListener("input", calcularDiferencia);

    await cargarEstado();
    await cargarHistorial();
});

async function cargarEstado() {
    try {
        const estado = (await Api.get("/api/caja/estado")).data;

        if (!estado) {
            document.getElementById("tarjetaApertura").style.display = "";
            document.getElementById("bloqueTurno").style.display = "none";
            turnoActual = null;
            return;
        }

        document.getElementById("tarjetaApertura").style.display = "none";
        document.getElementById("bloqueTurno").style.display = "";

        turnoActual = (await Api.get("/api/caja/resumen")).data;
        dibujarResumen(turnoActual);
        await cargarVentasTurno(turnoActual.id);

    } catch (error) {
        Mensaje.error(error.message);
    }
}

function dibujarResumen(turno) {
    const esperado = Number(turno.montoInicial) + Number(turno.totalEfectivo);

    const tarjetas = [
        { etiqueta: "Turno abierto desde", valor: Formato.fecha(turno.fechaApertura), clase: "" },
        { etiqueta: "Ventas del turno", valor: turno.cantidadVentas, clase: "" },
        { etiqueta: "Total vendido", valor: Formato.soles(turno.totalVentas), clase: "exito" },
        { etiqueta: "Efectivo", valor: Formato.soles(turno.totalEfectivo), clase: "" },
        { etiqueta: "Tarjeta", valor: Formato.soles(turno.totalTarjeta), clase: "" },
        { etiqueta: "Yape / Plin / Transf.", valor: Formato.soles(turno.totalDigital), clase: "" }
    ];

    document.getElementById("indicadores").innerHTML = tarjetas.map(t => `
        <div class="indicador ${t.clase}">
            <div class="etiqueta">${t.etiqueta}</div>
            <div class="valor" style="font-size:${String(t.valor).length > 9 ? "18px" : "24px"};">${Formato.texto(t.valor)}</div>
        </div>`).join("");

    document.getElementById("efectivoEsperado").value = esperado.toFixed(2);
    calcularDiferencia();
}

async function cargarVentasTurno(idTurno) {
    const cuerpo = document.getElementById("tablaVentasTurno");
    try {
        const ventas = (await Api.get("/api/caja/" + idTurno + "/ventas")).data || [];
        if (ventas.length === 0) {
            cuerpo.innerHTML = App.filaVacia(6, "Todavia no hay ventas registradas en este turno.");
            return;
        }
        cuerpo.innerHTML = ventas.map(v => `
            <tr>
                <td>${Formato.texto(v.numeroComprobante)}</td>
                <td>${Formato.fecha(v.fecha)}</td>
                <td>${Formato.texto(v.clienteNombre)}</td>
                <td class="centro">${Formato.texto(v.metodoPago)}</td>
                <td class="numero">${Formato.soles(v.total)}</td>
                <td class="centro">
                    <span class="etiqueta ${v.estado === "ANULADA" ? "roja" : "verde"}">${Formato.texto(v.estado)}</span>
                </td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(6, error.message);
    }
}

function calcularDiferencia() {
    const esperado = Number(document.getElementById("efectivoEsperado").value || 0);
    const contado = Number(document.getElementById("montoDeclarado").value || 0);
    const diferencia = contado - esperado;

    const campo = document.getElementById("diferencia");
    campo.value = diferencia.toFixed(2);
    campo.style.color = Math.abs(diferencia) < 0.005
        ? "var(--verde)"
        : (diferencia < 0 ? "var(--rojo)" : "var(--ambar)");
}

async function aperturar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    try {
        const respuesta = await Api.post("/api/caja/apertura", {
            montoInicial: Number(document.getElementById("montoInicial").value || 0)
        });
        Mensaje.exito(respuesta.mensaje);
        await cargarEstado();
        await cargarHistorial();
    } catch (error) {
        Mensaje.error(error.message);
    }
}

async function cerrar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    if (!confirm("El cierre de caja no se puede modificar despues de registrarse. Desea continuar?")) {
        return;
    }

    try {
        const respuesta = await Api.post("/api/caja/cierre", {
            montoDeclarado: Number(document.getElementById("montoDeclarado").value || 0),
            observaciones: document.getElementById("observaciones").value.trim() || null
        });
        Mensaje.exito(respuesta.mensaje);
        document.getElementById("formCierre").reset();
        await cargarEstado();
        await cargarHistorial();
    } catch (error) {
        Mensaje.error(error.message);
    }
}

async function cargarHistorial() {
    const cuerpo = document.getElementById("tablaHistorialCaja");
    try {
        const cierres = (await Api.get("/api/caja/historial")).data || [];
        if (cierres.length === 0) {
            cuerpo.innerHTML = App.filaVacia(7, "No hay cajas registradas.");
            return;
        }
        cuerpo.innerHTML = cierres.map(c => `
            <tr>
                <td>${Formato.fecha(c.fechaApertura)}</td>
                <td>${Formato.fecha(c.fechaCierre)}</td>
                <td>${Formato.texto(c.usuario ? c.usuario.username : "-")}</td>
                <td class="numero">${c.cantidadVentas}</td>
                <td class="numero">${Formato.soles(c.totalVentas)}</td>
                <td class="numero">${c.diferencia === null ? "-" : Formato.soles(c.diferencia)}</td>
                <td class="centro">
                    <span class="etiqueta ${c.estado === "ABIERTA" ? "ambar" : "gris"}">${Formato.texto(c.estado)}</span>
                </td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(7, error.message);
    }
}
