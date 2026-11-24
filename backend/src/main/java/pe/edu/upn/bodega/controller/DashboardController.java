/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : DashboardController.java
 * Descripcion   : Controlador REST de los indicadores del panel principal.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.dto.ResumenDashboard;
import pe.edu.upn.bodega.service.ReporteService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReporteService reporteService;

    public DashboardController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<ResumenDashboard>> resumen() {
        return ResponseEntity.ok(ApiResponse.exito("Indicadores del panel.", reporteService.resumen()));
    }
}
