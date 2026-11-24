/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : AccionAuditoria.java
 * Descripcion   : Acciones que se registran en la bitacora de auditoria.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.entity;

public enum AccionAuditoria {
    LOGIN,
    LOGOUT,
    LOGIN_FALLIDO,
    CREAR,
    ACTUALIZAR,
    ELIMINAR,
    DESACTIVAR,
    CONSULTAR,
    EXPORTAR,
    ANULAR,
    ACCESO_DENEGADO
}
