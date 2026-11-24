/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CajaService.java
 * Descripcion   : Servicio de negocio de la apertura y el cierre de caja del turno del cajero (RF-19, CU-18).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.dto.AperturaCajaRequest;
import pe.edu.upn.bodega.dto.CierreCajaRequest;
import pe.edu.upn.bodega.entity.*;
import pe.edu.upn.bodega.repository.CierreCajaRepository;
import pe.edu.upn.bodega.repository.UsuarioRepository;
import pe.edu.upn.bodega.repository.VentaRepository;
import pe.edu.upn.bodega.util.NegocioException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CajaService {

    private final CierreCajaRepository cierreCajaRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public CajaService(CierreCajaRepository cierreCajaRepository,
                       VentaRepository ventaRepository,
                       UsuarioRepository usuarioRepository,
                       AuditoriaService auditoriaService) {
        this.cierreCajaRepository = cierreCajaRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public Optional<CierreCaja> cajaAbierta() {
        Usuario usuario = usuarioAutenticado();
        return cierreCajaRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuario.getId(), EstadoCaja.ABIERTA);
    }

    @Transactional
    public CierreCaja aperturar(AperturaCajaRequest peticion) {
        if (cajaAbierta().isPresent()) {
            throw new NegocioException("Ya existe una caja abierta para su usuario.");
        }
        auditoriaService.marcarUsuarioBd();

        CierreCaja caja = new CierreCaja();
        caja.setUsuario(usuarioAutenticado());
        caja.setFechaApertura(LocalDateTime.now());
        caja.setMontoInicial(peticion.getMontoInicial());
        caja.setEstado(EstadoCaja.ABIERTA);

        CierreCaja guardada = cierreCajaRepository.save(caja);

        auditoriaService.registrar(AccionAuditoria.CREAR, "CIERRE_CAJA", guardada.getId(),
                "Apertura de caja con monto inicial S/ " + guardada.getMontoInicial(),
                null, "{\"estado\":\"ABIERTA\",\"monto_inicial\":" + guardada.getMontoInicial() + "}");
        return guardada;
    }

    /** Resumen de las operaciones del turno antes de confirmar el cierre. */
    @Transactional(readOnly = true)
    public CierreCaja resumenTurno() {
        CierreCaja caja = cajaAbierta()
                .orElseThrow(() -> new NegocioException("No tiene una caja abierta. Debe aperturarla primero."));
        return calcularTotales(caja);
    }

    @Transactional
    public CierreCaja cerrar(CierreCajaRequest peticion) {
        CierreCaja caja = cajaAbierta()
                .orElseThrow(() -> new NegocioException("No tiene una caja abierta para cerrar."));
        auditoriaService.marcarUsuarioBd();

        calcularTotales(caja);

        BigDecimal esperadoEfectivo = caja.getMontoInicial().add(caja.getTotalEfectivo())
                .setScale(2, RoundingMode.HALF_UP);

        caja.setMontoDeclarado(peticion.getMontoDeclarado());
        caja.setDiferencia(peticion.getMontoDeclarado().subtract(esperadoEfectivo)
                .setScale(2, RoundingMode.HALF_UP));
        caja.setObservaciones(peticion.getObservaciones());
        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado(EstadoCaja.CERRADA);

        CierreCaja guardada = cierreCajaRepository.save(caja);

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "CIERRE_CAJA", guardada.getId(),
                "Cierre de caja con " + guardada.getCantidadVentas() + " venta(s), total S/ "
                        + guardada.getTotalVentas() + ", diferencia S/ " + guardada.getDiferencia(),
                "{\"estado\":\"ABIERTA\"}",
                "{\"estado\":\"CERRADA\",\"total_ventas\":" + guardada.getTotalVentas()
                        + ",\"monto_declarado\":" + guardada.getMontoDeclarado()
                        + ",\"diferencia\":" + guardada.getDiferencia() + "}");

        return guardada;
    }

    @Transactional(readOnly = true)
    public List<CierreCaja> historial() {
        return cierreCajaRepository.findTop30ByOrderByFechaAperturaDesc();
    }

    @Transactional(readOnly = true)
    public List<Venta> ventasDelTurno(Long idCierre) {
        return ventaRepository.findByCierreCajaIdOrderByFechaAsc(idCierre);
    }

    private CierreCaja calcularTotales(CierreCaja caja) {
        List<Venta> ventas = ventaRepository.findByCierreCajaIdOrderByFechaAsc(caja.getId());

        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal tarjeta = BigDecimal.ZERO;
        BigDecimal digital = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        int cantidad = 0;

        for (Venta venta : ventas) {
            if (venta.getEstado() == EstadoVenta.ANULADA) {
                continue;
            }
            cantidad++;
            total = total.add(venta.getTotal());
            switch (venta.getMetodoPago()) {
                case EFECTIVO -> efectivo = efectivo.add(venta.getTotal());
                case TARJETA -> tarjeta = tarjeta.add(venta.getTotal());
                default -> digital = digital.add(venta.getTotal());
            }
        }

        caja.setTotalEfectivo(efectivo.setScale(2, RoundingMode.HALF_UP));
        caja.setTotalTarjeta(tarjeta.setScale(2, RoundingMode.HALF_UP));
        caja.setTotalDigital(digital.setScale(2, RoundingMode.HALF_UP));
        caja.setTotalVentas(total.setScale(2, RoundingMode.HALF_UP));
        caja.setCantidadVentas(cantidad);
        return caja;
    }

    private Usuario usuarioAutenticado() {
        String username = auditoriaService.usernameActual();
        return usuarioRepository.findByUsername(username == null ? "" : username)
                .orElseThrow(() -> new NegocioException("No se pudo identificar al usuario de la sesion."));
    }
}
