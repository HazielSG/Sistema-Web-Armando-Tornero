/* =====================================================================
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : api.js
 * Descripcion   : Capa de comunicacion de la presentacion con el backend.
 *                 Centraliza las peticiones HTTP, el manejo de errores y
 *                 el formato de los datos mostrados en pantalla.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 * ===================================================================== */

/* Si la interfaz se publica en el mismo servidor del backend, se deja vacio.
   Si se abre con otro servidor (por ejemplo Live Server), colocar aqui
   la direccion del backend: "http://localhost:8080". */
const URL_BASE = "";

const Api = {

    async peticion(ruta, opciones = {}) {
        const configuracion = {
            method: opciones.method || "GET",
            credentials: "include",
            headers: { "Accept": "application/json" }
        };

        if (opciones.cuerpo !== undefined) {
            configuracion.headers["Content-Type"] = "application/json";
            configuracion.body = JSON.stringify(opciones.cuerpo);
        }

        let respuesta;
        try {
            respuesta = await fetch(URL_BASE + ruta, configuracion);
        } catch (error) {
            throw new Error("No se pudo conectar con el servidor. Verifique que el backend este en ejecucion.");
        }

        if (respuesta.status === 401) {
            if (!window.location.pathname.endsWith("index.html")
                && window.location.pathname !== "/") {
                window.location.href = "../index.html";
            }
            throw new Error("Su sesion termino. Debe iniciar sesion nuevamente.");
        }

        let datos = null;
        const tipo = respuesta.headers.get("content-type") || "";
        if (tipo.includes("application/json")) {
            datos = await respuesta.json();
        }

        if (!respuesta.ok) {
            throw new Error((datos && datos.mensaje) || "La operacion no se pudo completar.");
        }

        return datos;
    },

    get(ruta) { return this.peticion(ruta); },
    post(ruta, cuerpo) { return this.peticion(ruta, { method: "POST", cuerpo }); },
    put(ruta, cuerpo) { return this.peticion(ruta, { method: "PUT", cuerpo }); },
    eliminar(ruta) { return this.peticion(ruta, { method: "DELETE" }); },

    /** Descarga un archivo generado por el backend (RF-22). */
    descargar(ruta, nombreArchivo) {
        const enlace = document.createElement("a");
        enlace.href = URL_BASE + ruta;
        enlace.download = nombreArchivo;
        document.body.appendChild(enlace);
        enlace.click();
        document.body.removeChild(enlace);
    }
};

/* ------------------------------------------------------------------ */
/* Utilitarios de presentacion                                         */
/* ------------------------------------------------------------------ */
const Formato = {

    soles(valor) {
        const numero = Number(valor || 0);
        return "S/ " + numero.toFixed(2);
    },

    fecha(valor) {
        if (!valor) {
            return "-";
        }
        const texto = String(valor).replace("T", " ");
        const partes = texto.split(" ");
        const fecha = partes[0].split("-");
        if (fecha.length !== 3) {
            return texto;
        }
        const hora = partes[1] ? partes[1].substring(0, 5) : "";
        return `${fecha[2]}/${fecha[1]}/${fecha[0]}${hora ? " " + hora : ""}`;
    },

    hoy() {
        return new Date().toISOString().substring(0, 10);
    },

    haceDias(dias) {
        const fecha = new Date();
        fecha.setDate(fecha.getDate() - dias);
        return fecha.toISOString().substring(0, 10);
    },

    /** Evita que el contenido almacenado altere la estructura de la pagina. */
    texto(valor) {
        if (valor === null || valor === undefined) {
            return "";
        }
        return String(valor)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }
};

/* ------------------------------------------------------------------ */
/* Mensajes en pantalla                                                */
/* ------------------------------------------------------------------ */
const Mensaje = {

    mostrar(tipo, texto) {
        const caja = document.getElementById("mensaje");
        if (!caja) {
            return;
        }
        caja.className = "mensaje visible " + tipo;
        caja.textContent = texto;
        window.scrollTo({ top: 0, behavior: "smooth" });
        if (tipo === "exito") {
            clearTimeout(this.temporizador);
            this.temporizador = setTimeout(() => this.ocultar(), 5000);
        }
    },

    exito(texto) { this.mostrar("exito", texto); },
    error(texto) { this.mostrar("error", texto); },
    aviso(texto) { this.mostrar("aviso", texto); },

    ocultar() {
        const caja = document.getElementById("mensaje");
        if (caja) {
            caja.className = "mensaje";
            caja.textContent = "";
        }
    }
};
