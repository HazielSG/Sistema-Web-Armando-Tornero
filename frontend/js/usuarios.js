/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : usuarios.js
 * Descripcion   : Logica de la gestion de cuentas de usuario: registro,
 *                 actualizacion, desactivacion y restablecimiento de la
 *                 contrasena (RF-04, CU-01, CU-02).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

document.addEventListener("DOMContentLoaded", async () => {
    const sesion = await App.iniciar("usuarios.html", ["ADMINISTRADOR"]);
    if (!sesion) {
        return;
    }

    await cargar();

    document.getElementById("formUsuario").addEventListener("submit", guardar);
    document.getElementById("btnCancelar").addEventListener("click", limpiar);
});

async function cargar() {
    const cuerpo = document.getElementById("tablaUsuarios");
    cuerpo.innerHTML = App.filaVacia(7, "Cargando usuarios...");

    try {
        const usuarios = (await Api.get("/api/usuarios")).data || [];

        cuerpo.innerHTML = usuarios.map(u => `
            <tr>
                <td><strong>${Formato.texto(u.username)}</strong></td>
                <td>${Formato.texto(u.nombres + " " + u.apellidos)}</td>
                <td><span class="etiqueta ${colorRol(u.rol)}">${Formato.texto(u.rol)}</span></td>
                <td>${Formato.texto(u.email || "-")}</td>
                <td>${Formato.fecha(u.ultimoAcceso)}</td>
                <td class="centro">
                    <span class="etiqueta ${u.activo ? "verde" : "roja"}">${u.activo ? "ACTIVO" : "INACTIVO"}</span>
                </td>
                <td class="centro" style="white-space:nowrap;">
                    <button class="secundario pequeno" data-editar="${u.id}">Editar</button>
                    <button class="secundario pequeno" data-clave="${u.id}">Clave</button>
                    <button class="${u.activo ? "peligro" : "secundario"} pequeno"
                            data-estado="${u.id}" data-activo="${u.activo}">
                        ${u.activo ? "Desactivar" : "Activar"}</button>
                </td>
            </tr>`).join("");

        cuerpo.querySelectorAll("[data-editar]").forEach(boton =>
            boton.addEventListener("click", () => editar(boton.dataset.editar, usuarios)));
        cuerpo.querySelectorAll("[data-clave]").forEach(boton =>
            boton.addEventListener("click", () => restablecerClave(boton.dataset.clave)));
        cuerpo.querySelectorAll("[data-estado]").forEach(boton =>
            boton.addEventListener("click", () =>
                cambiarEstado(boton.dataset.estado, boton.dataset.activo !== "true")));

    } catch (error) {
        cuerpo.innerHTML = App.filaVacia(7, error.message);
    }
}

function colorRol(rol) {
    if (rol === "ADMINISTRADOR") return "azul";
    if (rol === "ALMACENERO") return "verde";
    return "ambar";
}

async function guardar(evento) {
    evento.preventDefault();
    Mensaje.ocultar();

    const id = document.getElementById("idUsuario").value;
    const clave = document.getElementById("password").value;

    if (!id && clave.length < 6) {
        Mensaje.error("La contrasena es obligatoria y debe tener al menos 6 caracteres.");
        return;
    }

    const peticion = {
        username: document.getElementById("username").value.trim(),
        password: clave ? clave : null,
        nombres: document.getElementById("nombres").value.trim(),
        apellidos: document.getElementById("apellidos").value.trim(),
        documento: document.getElementById("documento").value.trim() || null,
        email: document.getElementById("email").value.trim() || null,
        telefono: document.getElementById("telefono").value.trim() || null,
        rol: document.getElementById("rol").value,
        activo: true
    };

    const boton = document.getElementById("btnGuardar");
    boton.disabled = true;

    try {
        const respuesta = id
            ? await Api.put("/api/usuarios/" + id, peticion)
            : await Api.post("/api/usuarios", peticion);
        Mensaje.exito(respuesta.mensaje);
        limpiar();
        await cargar();
    } catch (error) {
        Mensaje.error(error.message);
    } finally {
        boton.disabled = false;
    }
}

function editar(id, usuarios) {
    const usuario = usuarios.find(u => String(u.id) === String(id));
    if (!usuario) {
        return;
    }

    document.getElementById("idUsuario").value = usuario.id;
    document.getElementById("username").value = usuario.username;
    document.getElementById("username").readOnly = true;
    document.getElementById("password").value = "";
    document.getElementById("nombres").value = usuario.nombres;
    document.getElementById("apellidos").value = usuario.apellidos;
    document.getElementById("documento").value = usuario.documento || "";
    document.getElementById("email").value = usuario.email || "";
    document.getElementById("telefono").value = usuario.telefono || "";
    document.getElementById("rol").value = usuario.rol;

    document.getElementById("etiquetaPassword").textContent =
        "Contrasena (dejar vacio para conservarla)";
    document.getElementById("tituloFormulario").textContent = "Actualizar usuario";
    document.getElementById("btnGuardar").textContent = "Actualizar usuario";
    window.scrollTo({ top: 0, behavior: "smooth" });
}

async function cambiarEstado(id, activo) {
    const texto = activo
        ? "La cuenta podra volver a ingresar al sistema. Desea continuar?"
        : "La cuenta no podra ingresar al sistema. Desea continuar?";
    if (!confirm(texto)) {
        return;
    }

    try {
        const respuesta = await Api.put("/api/usuarios/" + id + "/estado", { activo: activo });
        Mensaje.aviso(respuesta.mensaje);
        await cargar();
    } catch (error) {
        Mensaje.error(error.message);
    }
}

async function restablecerClave(id) {
    const clave = prompt("Ingrese la nueva contrasena (minimo 6 caracteres):");
    if (clave === null) {
        return;
    }
    if (clave.trim().length < 6) {
        Mensaje.error("La nueva contrasena debe tener al menos 6 caracteres.");
        return;
    }

    try {
        const respuesta = await Api.put("/api/usuarios/" + id + "/clave", { password: clave.trim() });
        Mensaje.exito(respuesta.mensaje);
    } catch (error) {
        Mensaje.error(error.message);
    }
}

function limpiar() {
    document.getElementById("formUsuario").reset();
    document.getElementById("idUsuario").value = "";
    document.getElementById("username").readOnly = false;
    document.getElementById("etiquetaPassword").textContent = "Contrasena *";
    document.getElementById("tituloFormulario").textContent = "Registrar usuario";
    document.getElementById("btnGuardar").textContent = "Guardar usuario";
}
