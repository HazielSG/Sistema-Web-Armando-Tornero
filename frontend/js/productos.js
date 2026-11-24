/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : productos.js
 * Descripcion   : Logica de la pantalla de gestion de productos: registro,
 *                 consulta por codigo o nombre, actualizacion y baja
 *                 logica (RF-05, RF-06, CU-03, CU-05, CU-10, CU-11).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

let sesionActual = null;

document.addEventListener("DOMContentLoaded", async () => {
    sesionActual = await App.iniciar("productos.html", ["ADMINISTRADOR", "ALMACENERO"]);
    if (!sesionActual) {
        return;
    }

    await cargarCombos();
    await cargarProductos();

    document.getElementById("formProducto").addEventListener("submit", guardar);
    document.getElementById("btnCancelar").addEventListener("click", limpiarFormulario);
    document.getElementById("btnBuscar").addEventListener("click", () => cargarProductos());
    document.getElementById("btnLimpiarBusqueda").addEventListener("click", () => {
        document.getElementById("buscar").value = "";
        cargarProductos();
    });
    document.getElementById("buscar").addEventListener("keydown", (evento) => {
        if (evento.key === "Enter") {
            evento.preventDefault();
            cargarProductos();
        }
    });
});

async function cargarCombos() {
    try {
        const categorias = (await Api.get("/api/categorias")).data || [];
        document.getElementById("idCategoria").innerHTML =
            '<option value="">Seleccione una categoria</option>'
            + categorias.map(c => `<option value="${c.id}">${Formato.texto(c.nombre)}</option>`).join("");
    } catch (error) {
        Mensaje.error("No se pudieron cargar las categorias: " + error.message);
    }

    try {
        const proveedores = (await Api.get("/api/proveedores")).data || [];
        document.getElementById("idProveedor").innerHTML =
            '<option value="">Sin proveedor</option>'
            + proveedores.map(p => `<option value="${p.id}">${Formato.texto(p.razonSocial)}</option>`).join("");
    } catch (error) {
        /* El proveedor es opcional: si no se puede cargar, el combo queda vacio. */
    }
}

async function cargarProductos() {
    const cuerpo = document.getElementById("tablaProductos");
    const buscar = document.getElementById("buscar").value.trim();
    cuerpo.innerHTML = App.filaVacia(8, "Cargando productos...");

    try {
        const ruta = buscar ? "/api/productos?buscar=" + encodeURIComponent(buscar) : "/api/productos";
        const productos = (await Api.get(ruta)).data || [];

        if (productos.length === 0) {
            cuerpo.innerHTML = App.filaVacia(8, "No se encontraron productos con ese criterio.");
            return;
        }

        cuerpo.innerHTML = productos.map(p => `
            <tr>
                <td>${Formato.texto(p.codigo)}</td>
                <td>${Formato.texto(p.nombre)}</td>
                <td>${Formato.texto(p.categoria ? p.categoria.nombre : "-")}</td>
                <td class="numero">${Formato.soles(p.precioVenta)}</td>
                <td class="numero">${p.stockActual}</td>
                <td class="numero">${p.stockMinimo}</td>
                <td class="centro">
                    <span class="etiqueta ${p.stockActual <= p.stockMinimo ? "ambar" : "verde"}">
                        ${p.stockActual <= p.stockMinimo ? "STOCK BAJO" : "NORMAL"}</span>
                </td>
                <td class="centro" style="white-space:nowrap;">
                    <button class="secundario pequeno" data-editar="${p.id}">Editar</button>
                    ${sesionActual.rol === "ADMINISTRADOR"
                        ? `<button class="peligro pequeno" data-desactivar="${p.id}">Desactivar</button>` : ""}
                </td>
            </tr>`).join("");

        cuerpo.querySelectorAll("[data-editar]").forEach(boton =>
            boton.addEventListener("click", () => editar(boton.dataset.editar)));
        cuerpo.querySelectorAll("[data-desactivar]").forEach(boton =>
            boton.addEventListener("click", () => desactivar(boton.dataset.desactivar)));

    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(8, error.message);
    }
}

async function guardar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    const id = document.getElementById("idProducto").value;
    const peticion = {
        codigo: document.getElementById("codigo").value.trim(),
        nombre: document.getElementById("nombre").value.trim(),
        descripcion: document.getElementById("descripcion").value.trim(),
        idCategoria: Number(document.getElementById("idCategoria").value) || null,
        idProveedor: document.getElementById("idProveedor").value
                        ? Number(document.getElementById("idProveedor").value) : null,
        unidadMedida: document.getElementById("unidadMedida").value,
        precioCompra: Number(document.getElementById("precioCompra").value || 0),
        precioVenta: Number(document.getElementById("precioVenta").value || 0),
        stockMinimo: Number(document.getElementById("stockMinimo").value || 0),
        stockInicial: Number(document.getElementById("stockInicial").value || 0),
        activo: true
    };

    if (!peticion.idCategoria) {
        Mensaje.error("Debe seleccionar la categoria del producto.");
        return;
    }

    const boton = document.getElementById("btnGuardar");
    boton.disabled = true;

    try {
        const respuesta = id
            ? await Api.put("/api/productos/" + id, peticion)
            : await Api.post("/api/productos", peticion);
        Mensaje.exito(respuesta.mensaje);
        limpiarFormulario();
        await cargarProductos();
    } catch (error) {
        Mensaje.error(error.message);
    } finally {
        boton.disabled = false;
    }
}

async function editar(id) {
    try {
        const producto = (await Api.get("/api/productos/" + id)).data;

        document.getElementById("idProducto").value = producto.id;
        document.getElementById("codigo").value = producto.codigo;
        document.getElementById("nombre").value = producto.nombre;
        document.getElementById("descripcion").value = producto.descripcion || "";
        document.getElementById("idCategoria").value = producto.categoria ? producto.categoria.id : "";
        document.getElementById("idProveedor").value = producto.proveedor ? producto.proveedor.id : "";
        document.getElementById("unidadMedida").value = producto.unidadMedida;
        document.getElementById("precioCompra").value = producto.precioCompra;
        document.getElementById("precioVenta").value = producto.precioVenta;
        document.getElementById("stockMinimo").value = producto.stockMinimo;

        document.getElementById("tituloFormulario").textContent =
            "Actualizar producto " + producto.codigo;
        document.getElementById("btnGuardar").textContent = "Actualizar producto";
        /* El stock solo se modifica mediante movimientos de inventario. */
        document.getElementById("campoStockInicial").style.display = "none";

        window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
        Mensaje.error(error.message);
    }
}

async function desactivar(id) {
    if (!confirm("El producto se desactivara y dejara de mostrarse en las ventas. "
               + "El historial de operaciones se conserva. Desea continuar?")) {
        return;
    }
    try {
        const respuesta = await Api.eliminar("/api/productos/" + id);
        Mensaje.aviso(respuesta.mensaje);
        await cargarProductos();
    } catch (error) {
        Mensaje.error(error.message);
    }
}

function limpiarFormulario() {
    document.getElementById("formProducto").reset();
    document.getElementById("idProducto").value = "";
    document.getElementById("tituloFormulario").textContent = "Registrar producto";
    document.getElementById("btnGuardar").textContent = "Guardar producto";
    document.getElementById("campoStockInicial").style.display = "";
}
