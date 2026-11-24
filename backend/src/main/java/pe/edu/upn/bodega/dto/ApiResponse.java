/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ApiResponse.java
 * Descripcion   : Envoltura uniforme de las respuestas que entrega el backend a la capa de presentacion.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

public class ApiResponse<T> {

    private boolean ok;
    private String mensaje;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(boolean ok, String mensaje, T data) {
        this.ok = ok;
        this.mensaje = mensaje;
        this.data = data;
    }

    public static <T> ApiResponse<T> exito(String mensaje, T data) {
        return new ApiResponse<>(true, mensaje, data);
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return new ApiResponse<>(false, mensaje, null);
    }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
