/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ventas.js
 * Descripcion   : Logica del registro de ventas: seleccion de productos,
 *                 validacion del stock disponible, calculo del importe y
 *                 registro del pago (RF-13 a RF-18).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

const IGV = 0.18;

let catalogo = [];
let detalle = [];

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("ventas.html", ["CAJERO", "ADMINISTRADOR"]);
    if (!sesion) {
        return;
    }

    await verificarCaja();
    await cargarCatalogo();
    calcularTotales();

    document.getElementById("btnAgregar").addEventListener("click", agregarDesdeBusqueda);
    document.getElementById("btnVaciar").addEventListener("click", vaciar);
    document.getElementById("formVenta").addEventListener("submit", confirmar);
    document.getElementById("metodoPago").addEventListener("change", cambiarMetodoPago);
    document.getElementById("montoRecibido").addEventListener("input", calcularTotales);
    document.getElementById("buscar").addEventListener("input", dibujarCatalogo);
    document.getElementById("buscar").addEventListener("keydown", (evento) => {
        if (evento.key === "Enter") {
            evento.preventDefault();
            agregarDesdeBusqueda();
        }
    });
});

/** La venta se asocia al turno de caja en curso (RF-19). */
async function verificarCaja() {
    try {
        const respuesta = await Api.get("/api/caja/estado");
        if (!respuesta.data) {
            const aviso = document.getElementById("avisoCaja");
            aviso.style.display = "";
            aviso.innerHTML = '<h2>Caja sin aperturar</h2>'
                + '<p>Puede registrar ventas, pero no se incluiran en un cierre de caja hasta que '
                + 'aperture su turno.</p>'
                + '<div class="acciones"><a href="caja.html"><button>Aperturar caja</button></a></div>';
        }
    } catch (error) {
        /* La verificacion de caja no impide registrar la venta. */
    }
}

async function cargarCatalogo() {
    try {
        catalogo = ((await Api.get("/api/productos")).data || []).filter(p => p.stockActual > 0);
        dibujarCatalogo();
    } catch (error) {
        Mensaje.error(error.message);
    }
}

function dibujarCatalogo() {
    const texto = document.getElementById("buscar").value.trim().toLowerCase();
    const cuerpo = document.getElementById("tablaCatalogo");

    const filtrados = texto
        ? catalogo.filter(p => p.codigo.toLowerCase().includes(texto)
                            || p.nombre.toLowerCase().includes(texto))
        : catalogo.slice(0, 12);

    if (filtrados.length === 0) {
        cuerpo.innerHTML = App.filaVacia(5, "No hay productos con existencias para ese criterio.");
        return;
    }

    cuerpo.innerHTML = filtrados.map(p => `
        <tr>
            <td>${Formato.texto(p.codigo)}</td>
            <td>${Formato.texto(p.nombre)}</td>
            <td class="numero">${Formato.soles(p.precioVenta)}</td>
            <td class="numero">${p.stockActual}</td>
            <td class="centro"><button class="secundario pequeno" data-agregar="${p.id}">Agregar</button></td>
        </tr>`).join("");

    cuerpo.querySelectorAll("[data-agregar]").forEach(boton =>
        boton.addEventListener("click", () => agregar(Number(boton.dataset.agregar), 1)));
}

function agregarDesdeBusqueda() {
    const texto = document.getElementById("buscar").value.trim().toLowerCase();
    const cantidad = Number(document.getElementById("cantidad").value || 1);

    if (!texto) {
        Mensaje.aviso("Escriba el codigo o el nombre del producto.");
        return;
    }

    const encontrado = catalogo.find(p => p.codigo.toLowerCase() === texto)
        || catalogo.find(p => p.codigo.toLowerCase().includes(texto)
                           || p.nombre.toLowerCase().includes(texto));

    if (!encontrado) {
        Mensaje.error("No se encontro un producto con existencias para ese criterio.");
        return;
    }

    agregar(encontrado.id, cantidad);
}

function agregar(idProducto, cantidad) {
    Mensaje.ocultar();

    const producto = catalogo.find(p => p.id === idProducto);
    if (!producto) {
        return;
    }

    const existente = detalle.find(d => d.idProducto === idProducto);
    const cantidadFinal = (existente ? existente.cantidad : 0) + cantidad;

    /* RF-14: no se permite superar el stock disponible. */
    if (cantidadFinal > producto.stockActual) {
        Mensaje.error(`Solo hay ${producto.stockActual} unidad(es) de ${producto.nombre}.`);
        return;
    }

    if (existente) {
        existente.cantidad = cantidadFinal;
    } else {
        detalle.push({
            idProducto: producto.id,
            codigo: producto.codigo,
            nombre: producto.nombre,
            precio: Number(producto.precioVenta),
            stock: producto.stockActual,
            cantidad: cantidad
        });
    }

    document.getElementById("buscar").value = "";
    document.getElementById("cantidad").value = 1;
    dibujarCatalogo();
    dibujarDetalle();
    calcularTotales();
}

function dibujarDetalle() {
    const cuerpo = document.getElementById("tablaDetalle");

    if (detalle.length === 0) {
        cuerpo.innerHTML = App.filaVacia(5, "Agregue productos para registrar la venta.");
        return;
    }

    cuerpo.innerHTML = detalle.map((d, indice) => `
        <tr>
            <td>${Formato.texto(d.codigo + " - " + d.nombre)}</td>
            <td class="numero">
                <input type="number" min="1" max="${d.stock}" value="${d.cantidad}"
                       data-cantidad="${indice}" style="width:78px;text-align:right;">
            </td>
            <td class="numero">${Formato.soles(d.precio)}</td>
            <td class="numero">${Formato.soles(d.precio * d.cantidad)}</td>
            <td class="centro"><button class="peligro pequeno" data-quitar="${indice}">Quitar</button></td>
        </tr>`).join("");

    cuerpo.querySelectorAll("[data-cantidad]").forEach(campo =>
        campo.addEventListener("change", () => cambiarCantidad(Number(campo.dataset.cantidad), campo)));
    cuerpo.querySelectorAll("[data-quitar]").forEach(boton =>
        boton.addEventListener("click", () => {
            detalle.splice(Number(boton.dataset.quitar), 1);
            dibujarDetalle();
            calcularTotales();
        }));
}

function cambiarCantidad(indice, campo) {
    const nueva = Number(campo.value || 1);
    const linea = detalle[indice];

    if (nueva < 1) {
        campo.value = linea.cantidad;
        return;
    }
    if (nueva > linea.stock) {
        Mensaje.error(`Solo hay ${linea.stock} unidad(es) de ${linea.nombre}.`);
        campo.value = linea.cantidad;
        return;
    }

    linea.cantidad = nueva;
    dibujarDetalle();
    calcularTotales();
}

function calcularTotales() {
    const total = detalle.reduce((suma, d) => suma + d.precio * d.cantidad, 0);
    const valorVenta = total / (1 + IGV);
    const igv = total - valorVenta;

    const recibido = Number(document.getElementById("montoRecibido").value || 0);
    const esEfectivo = document.getElementById("metodoPago").value === "EFECTIVO";
    const vuelto = esEfectivo && recibido > total ? recibido - total : 0;

    document.getElementById("lblSubtotal").textContent = Formato.soles(valorVenta);
    document.getElementById("lblIgv").textContent = Formato.soles(igv);
    document.getElementById("lblTotal").textContent = Formato.soles(total);
    document.getElementById("lblVuelto").textContent = Formato.soles(vuelto);

    document.getElementById("btnConfirmar").disabled = detalle.length === 0;
}

function cambiarMetodoPago() {
    const esEfectivo = document.getElementById("metodoPago").value === "EFECTIVO";
    document.getElementById("campoRecibido").style.display = esEfectivo ? "" : "none";
    if (!esEfectivo) {
        document.getElementById("montoRecibido").value = "";
    }
    calcularTotales();
}

function vaciar() {
    detalle = [];
    document.getElementById("formVenta").reset();
    cambiarMetodoPago();
    dibujarDetalle();
    calcularTotales();
    Mensaje.ocultar();
}

async function confirmar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    if (detalle.length === 0) {
        Mensaje.error("Debe agregar al menos un producto a la venta.");
        return;
    }

    const total = detalle.reduce((suma, d) => suma + d.precio * d.cantidad, 0);
    const metodoPago = document.getElementById("metodoPago").value;
    const recibido = Number(document.getElementById("montoRecibido").value || 0);

    if (metodoPago === "EFECTIVO" && recibido > 0 && recibido < total) {
        Mensaje.error("El monto recibido es menor que el total de la venta.");
        return;
    }

    const peticion = {
        clienteNombre: document.getElementById("clienteNombre").value.trim(),
        clienteDocumento: document.getElementById("clienteDocumento").value.trim() || null,
        metodoPago: metodoPago,
        estadoPago: "PAGADO",
        montoRecibido: metodoPago === "EFECTIVO" && recibido > 0 ? recibido : null,
        items: detalle.map(d => ({ idProducto: d.idProducto, cantidad: d.cantidad }))
    };

    const boton = document.getElementById("btnConfirmar");
    boton.disabled = true;
    boton.textContent = "Registrando...";

    try {
        const respuesta = await Api.post("/api/ventas", peticion);
        /* RF-18: se continua al comprobante de la venta registrada. */
        window.location.href = "comprobante.html?id=" + respuesta.data.id;
    } catch (error) {
        Mensaje.error(error.message);
        boton.disabled = false;
        boton.textContent = "Confirmar venta";
    }
}
