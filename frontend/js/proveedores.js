/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : proveedores.js
 * Descripcion   : Logica de la pantalla de proveedores: registro, consulta
 *                 y actualizacion (RF-08, CU-04).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

let sesionActual = null;

document.addEventListener("DOMContentLoaded", async () => {
    sesionActual = await App.iniciar("proveedores.html", ["ADMINISTRADOR", "ALMACENERO"]);
    if (!sesionActual) {
        return;
    }

    /* El almacenero solo consulta la informacion de los proveedores. */
    if (sesionActual.rol !== "ADMINISTRADOR") {
        document.getElementById("tarjetaFormulario").style.display = "none";
    }

    await cargar();

    document.getElementById("formProveedor").addEventListener("submit", guardar);
    document.getElementById("btnCancelar").addEventListener("click", limpiar);
});

async function cargar() {
    const cuerpo = document.getElementById("tablaProveedores");
    cuerpo.innerHTML = App.filaVacia(7, "Cargando proveedores...");

    try {
        const proveedores = (await Api.get("/api/proveedores")).data || [];
        if (proveedores.length === 0) {
            cuerpo.innerHTML = App.filaVacia(7, "No hay proveedores registrados.");
            return;
        }

        cuerpo.innerHTML = proveedores.map(p => `
            <tr>
                <td>${Formato.texto(p.ruc)}</td>
                <td>${Formato.texto(p.razonSocial)}</td>
                <td>${Formato.texto(p.contacto || "-")}</td>
                <td>${Formato.texto(p.telefono || "-")}</td>
                <td>${Formato.texto(p.email || "-")}</td>
                <td>${Formato.texto(p.direccion || "-")}</td>
                <td class="centro">
                    ${sesionActual.rol === "ADMINISTRADOR"
                        ? `<button class="secundario pequeno" data-editar="${p.id}">Editar</button>` : "-"}
                </td>
            </tr>`).join("");

        cuerpo.querySelectorAll("[data-editar]").forEach(boton =>
            boton.addEventListener("click", () => editar(boton.dataset.editar, proveedores)));

    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(7, error.message);
    }
}

async function guardar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    const id = document.getElementById("idProveedor").value;
    const proveedor = {
        ruc: document.getElementById("ruc").value.trim(),
        razonSocial: document.getElementById("razonSocial").value.trim(),
        contacto: document.getElementById("contacto").value.trim() || null,
        telefono: document.getElementById("telefono").value.trim() || null,
        email: document.getElementById("email").value.trim() || null,
        direccion: document.getElementById("direccion").value.trim() || null,
        activo: true
    };

    const boton = document.getElementById("btnGuardar");
    boton.disabled = true;

    try {
        const respuesta = id
            ? await Api.put("/api/proveedores/" + id, proveedor)
            : await Api.post("/api/proveedores", proveedor);
        Mensaje.exito(respuesta.mensaje);
        limpiar();
        await cargar();
    } catch (error) {
        Mensaje.error(error.message);
    } finally {
        boton.disabled = false;
    }
}

function editar(id, proveedores) {
    const proveedor = proveedores.find(p => String(p.id) === String(id));
    if (!proveedor) {
        return;
    }

    document.getElementById("idProveedor").value = proveedor.id;
    document.getElementById("ruc").value = proveedor.ruc;
    document.getElementById("ruc").readOnly = true;
    document.getElementById("razonSocial").value = proveedor.razonSocial;
    document.getElementById("contacto").value = proveedor.contacto || "";
    document.getElementById("telefono").value = proveedor.telefono || "";
    document.getElementById("email").value = proveedor.email || "";
    document.getElementById("direccion").value = proveedor.direccion || "";

    document.getElementById("tituloFormulario").textContent = "Actualizar proveedor";
    document.getElementById("btnGuardar").textContent = "Actualizar proveedor";
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function limpiar() {
    document.getElementById("formProveedor").reset();
    document.getElementById("idProveedor").value = "";
    document.getElementById("ruc").readOnly = false;
    document.getElementById("tituloFormulario").textContent = "Registrar proveedor";
    document.getElementById("btnGuardar").textContent = "Guardar proveedor";
}
