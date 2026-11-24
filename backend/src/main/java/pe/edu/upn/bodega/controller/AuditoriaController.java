/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : AuditoriaController.java
 * Descripcion   : Controlador REST de la bitacora de auditoria: consulta filtrada del historial de operaciones, resumen por accion y exportacion (RF-20, RF-22, CU-06, CU-07).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.controller;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upn.bodega.dto.ApiResponse;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.Auditoria;
import pe.edu.upn.bodega.service.AuditoriaService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    /** RF-20: consulta y filtrado del historial de operaciones. */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> filtrar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) AccionAuditoria accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        Page<Auditoria> resultado = auditoriaService.filtrar(desde, hasta, usuario, accion, entidad,
                                                             pagina, tamano);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("registros", resultado.getContent());
        cuerpo.put("pagina", resultado.getNumber());
        cuerpo.put("totalPaginas", resultado.getTotalPages());
        cuerpo.put("totalRegistros", resultado.getTotalElements());

        return ResponseEntity.ok(ApiResponse.exito(
                resultado.getTotalElements() + " operacion(es) registrada(s).", cuerpo));
    }

    @GetMapping("/ultimas")
    public ResponseEntity<ApiResponse<List<Auditoria>>> ultimas() {
        return ResponseEntity.ok(ApiResponse.exito("Ultimas operaciones registradas.",
                auditoriaService.ultimosMovimientos()));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] registro : auditoriaService.resumenPorAccion(desde, hasta)) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("accion", String.valueOf(registro[0]));
            fila.put("cantidad", registro[1]);
            filas.add(fila);
        }
        return ResponseEntity.ok(ApiResponse.exito("Resumen de operaciones por accion.", filas));
    }

    /** Valores disponibles para los combos de filtro de la pantalla de auditoria. */
    @GetMapping("/filtros")
    public ResponseEntity<ApiResponse<Map<String, Object>>> filtrosDisponibles() {
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("acciones", AccionAuditoria.values());
        filtros.put("entidades", auditoriaService.listarEntidades());
        filtros.put("usuarios", auditoriaService.listarUsuarios());
        return ResponseEntity.ok(ApiResponse.exito("Filtros disponibles.", filtros));
    }
}
