/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : inventario.js
 * Descripcion   : Logica de la pantalla de control de inventario: registro
 *                 de entradas, salidas y ajustes, consulta de existencias
 *                 y alertas de stock bajo (RF-09 a RF-12).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

let productosCargados = [];
let sesionActual = null;

document.addEventListener("DOMContentLoaded", async () => {
    sesionActual = await App.iniciar("inventario.html", ["ADMINISTRADOR", "ALMACENERO", "CAJERO"]);
    if (!sesionActual) {
        return;
    }

    /* El cajero solo consulta las existencias (RF-11). */
    if (sesionActual.rol === "CAJERO") {
        document.getElementById("tarjetaMovimiento").style.display = "none";
    }

    document.getElementById("desde").value = Formato.haceDias(7);
    document.getElementById("hasta").value = Formato.hoy();

    await cargarProductos();
    await cargarMovimientos();

    const formulario = document.getElementById("formMovimiento");
    if (formulario) {
        formulario.addEventListener("submit", registrar);
        document.getElementById("btnLimpiar").addEventListener("click", () => {
            formulario.reset();
            document.getElementById("stockActual").value = "-";
        });
        document.getElementById("idProducto").addEventListener("change", mostrarStockActual);
        document.getElementById("tipo").addEventListener("change", ajustarEtiquetaCantidad);
    }

    document.getElementById("btnFiltrar").addEventListener("click",
        () => cargarMovimientos(document.getElementById("desde").value,
                                document.getElementById("hasta").value));
    document.getElementById("btnUltimos").addEventListener("click", () => cargarMovimientos());
});

async function cargarProductos() {
    try {
        productosCargados = (await Api.get("/api/productos")).data || [];

        const combo = document.getElementById("idProducto");
        if (combo) {
            combo.innerHTML = '<option value="">Seleccione un producto</option>'
                + productosCargados.map(p =>
                    `<option value="${p.id}">${Formato.texto(p.codigo + " - " + p.nombre)}</option>`).join("");
        }

        dibujarIndicadores();
        dibujarExistencias();
    } catch (error) {
        Mensaje.error(error.message);
    }
}

function dibujarIndicadores() {
    const bajos = productosCargados.filter(p => p.stockActual <= p.stockMinimo);
    const sinStock = productosCargados.filter(p => p.stockActual === 0);
    const unidades = productosCargados.reduce((suma, p) => suma + p.stockActual, 0);

    const tarjetas = [
        { etiqueta: "Productos activos", valor: productosCargados.length, clase: "" },
        { etiqueta: "Unidades en almacen", valor: unidades, clase: "" },
        { etiqueta: "Con stock bajo", valor: bajos.length, clase: bajos.length ? "alerta" : "exito" },
        { etiqueta: "Sin existencias", valor: sinStock.length, clase: sinStock.length ? "alerta" : "exito" }
    ];

    document.getElementById("indicadores").innerHTML = tarjetas.map(t => `
        <div class="indicador ${t.clase}">
            <div class="etiqueta">${t.etiqueta}</div>
            <div class="valor">${t.valor}</div>
        </div>`).join("");
}

function dibujarExistencias() {
    const cuerpo = document.getElementById("tablaExistencias");
    if (productosCargados.length === 0) {
        cuerpo.innerHTML = App.filaVacia(7, "No hay productos registrados.");
        return;
    }
    cuerpo.innerHTML = productosCargados.map(p => `
        <tr>
            <td>${Formato.texto(p.codigo)}</td>
            <td>${Formato.texto(p.nombre)}</td>
            <td>${Formato.texto(p.categoria ? p.categoria.nombre : "-")}</td>
            <td>${Formato.texto(p.unidadMedida)}</td>
            <td class="numero">${p.stockActual}</td>
            <td class="numero">${p.stockMinimo}</td>
            <td class="centro">
                <span class="etiqueta ${p.stockActual === 0 ? "roja" : (p.stockActual <= p.stockMinimo ? "ambar" : "verde")}">
                    ${p.stockActual === 0 ? "SIN STOCK" : (p.stockActual <= p.stockMinimo ? "STOCK BAJO" : "NORMAL")}
                </span>
            </td>
        </tr>`).join("");
}

function mostrarStockActual() {
    const id = Number(document.getElementById("idProducto").value);
    const producto = productosCargados.find(p => p.id === id);
    document.getElementById("stockActual").value = producto
        ? producto.stockActual + " " + producto.unidadMedida.toLowerCase()
        : "-";
}

function ajustarEtiquetaCantidad() {
    const tipo = document.getElementById("tipo").value;
    document.getElementById("etiquetaCantidad").textContent = tipo === "AJUSTE"
        ? "Stock real contado *"
        : "Cantidad *";
}

async function registrar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    const peticion = {
        idProducto: Number(document.getElementById("idProducto").value) || null,
        tipo: document.getElementById("tipo").value,
        cantidad: Number(document.getElementById("cantidad").value || 0),
        motivo: document.getElementById("motivo").value.trim(),
        referencia: document.getElementById("referencia").value.trim() || null
    };

    if (!peticion.idProducto) {
        Mensaje.error("Debe seleccionar el producto del movimiento.");
        return;
    }

    const boton = document.getElementById("btnRegistrar");
    boton.disabled = true;

    try {
        const respuesta = await Api.post("/api/inventario/movimientos", peticion);
        Mensaje.exito(respuesta.mensaje);
        document.getElementById("formMovimiento").reset();
        document.getElementById("stockActual").value = "-";
        await cargarProductos();
        await cargarMovimientos();
    } catch (error) {
        Mensaje.error(error.message);
    } finally {
        boton.disabled = false;
    }
}

async function cargarMovimientos(desde, hasta) {
    const cuerpo = document.getElementById("tablaMovimientos");
    cuerpo.innerHTML = App.filaVacia(8, "Cargando movimientos...");

    let ruta = "/api/inventario/movimientos";
    if (desde && hasta) {
        ruta += `?desde=${desde}&hasta=${hasta}`;
    }

    try {
        const movimientos = (await Api.get(ruta)).data || [];
        if (movimientos.length === 0) {
            cuerpo.innerHTML = App.filaVacia(8, "No hay movimientos en el periodo indicado.");
            return;
        }
        cuerpo.innerHTML = movimientos.map(m => `
            <tr>
                <td>${Formato.fecha(m.fecha)}</td>
                <td>${Formato.texto(m.producto ? m.producto.codigo + " - " + m.producto.nombre : "-")}</td>
                <td class="centro"><span class="etiqueta ${colorTipo(m.tipo)}">${Formato.texto(m.tipo)}</span></td>
                <td class="numero">${m.cantidad}</td>
                <td class="numero">${m.stockAnterior}</td>
                <td class="numero">${m.stockResultante}</td>
                <td>${Formato.texto(m.motivo)}</td>
                <td>${Formato.texto(m.usuario ? m.usuario.username : "-")}</td>
            </tr>`).join("");
    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(8, error.message);
    }
}

function colorTipo(tipo) {
    if (tipo === "ENTRADA") return "verde";
    if (tipo === "SALIDA") return "roja";
    return "azul";
}
