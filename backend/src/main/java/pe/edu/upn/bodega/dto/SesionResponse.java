/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : SesionResponse.java
 * Descripcion   : DTO con los datos de la sesion autenticada que utiliza el frontend para habilitar los modulos segun el rol.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.dto;

public class SesionResponse {

    private Long id;
    private String username;
    private String nombreCompleto;
    private String rol;

    public SesionResponse() {
    }

    public SesionResponse(Long id, String username, String nombreCompleto, String rol) {
        this.id = id;
        this.username = username;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
