/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : app.js
 * Descripcion   : Elementos comunes de la capa de presentacion: validacion
 *                 de la sesion, menu lateral segun el rol del usuario
 *                 (RF-03) y cierre de sesion (RF-02).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

/* Modulos del sistema y roles que pueden ingresar a cada uno. */
const MODULOS = [
    { archivo: "panel.html",       titulo: "Panel principal", roles: ["ADMINISTRADOR", "ALMACENERO", "CAJERO"] },
    { archivo: "productos.html",   titulo: "Productos",       roles: ["ADMINISTRADOR", "ALMACENERO"] },
    { archivo: "inventario.html",  titulo: "Inventario",      roles: ["ADMINISTRADOR", "ALMACENERO", "CAJERO"] },
    { archivo: "ventas.html",      titulo: "Registrar venta", roles: ["CAJERO", "ADMINISTRADOR"] },
    { archivo: "historial.html",   titulo: "Historial de ventas", roles: ["CAJERO", "ADMINISTRADOR"] },
    { archivo: "caja.html",        titulo: "Cierre de caja",  roles: ["CAJERO", "ADMINISTRADOR"] },
    { archivo: "proveedores.html", titulo: "Proveedores",     roles: ["ADMINISTRADOR", "ALMACENERO"] },
    { archivo: "usuarios.html",    titulo: "Usuarios",        roles: ["ADMINISTRADOR"] },
    { archivo: "reportes.html",    titulo: "Reportes",        roles: ["ADMINISTRADOR"] },
    { archivo: "auditoria.html",   titulo: "Auditoria",       roles: ["ADMINISTRADOR"] }
];

const App = {

    sesion: null,

    /**
     * Comprueba que exista una sesion activa, dibuja el menu segun el rol
     * y verifica que el usuario tenga permiso para la pagina solicitada.
     */
    async iniciar(archivoActual, rolesPermitidos) {
        try {
            const respuesta = await Api.get("/api/auth/sesion");
            this.sesion = respuesta.data;
        } catch (error) {
            window.location.href = "../index.html";
            return null;
        }

        if (rolesPermitidos && !rolesPermitidos.includes(this.sesion.rol)) {
            document.querySelector(".contenido").innerHTML =
                '<div class="tarjeta"><h2>Acceso restringido</h2>'
                + '<p>Su rol (' + Formato.texto(this.sesion.rol) + ') no tiene permiso para utilizar este modulo.</p>'
                + '<div class="acciones"><a href="panel.html"><button>Volver al panel</button></a></div></div>';
            this.dibujarMenu(archivoActual);
            return null;
        }

        this.dibujarMenu(archivoActual);
        return this.sesion;
    },

    dibujarMenu(archivoActual) {
        const lateral = document.querySelector(".barra-lateral");
        if (!lateral) {
            return;
        }

        const enlaces = MODULOS
            .filter(modulo => modulo.roles.includes(this.sesion.rol))
            .map(modulo => {
                const activo = modulo.archivo === archivoActual ? ' class="activo"' : "";
                return `<a href="${modulo.archivo}"${activo}>${modulo.titulo}</a>`;
            })
            .join("");

        lateral.innerHTML = `
            <div class="titulo">
                <span class="nombre">Bodega Armando Tornero</span>
                <span class="rol">${Formato.texto(this.sesion.rol)}</span>
            </div>
            <nav>${enlaces}</nav>
            <div class="pie">
                <p style="margin:0 0 10px;font-size:13px;">${Formato.texto(this.sesion.nombreCompleto)}</p>
                <button class="secundario pequeno" id="btnSalir">Cerrar sesion</button>
            </div>`;

        const boton = document.getElementById("btnSalir");
        if (boton) {
            boton.addEventListener("click", () => this.cerrarSesion());
        }
    },

    async cerrarSesion() {
        try {
            await Api.post("/api/auth/logout", {});
        } catch (error) {
            /* La sesion se cierra igualmente en la interfaz. */
        }
        window.location.href = "../index.html";
    },

    /** Dibuja una fila de aviso cuando una tabla no tiene registros. */
    filaVacia(columnas, texto) {
        return `<tr><td class="vacio" colspan="${columnas}">${texto}</td></tr>`;
    }
};
