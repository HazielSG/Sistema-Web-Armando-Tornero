/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ProveedorService.java
 * Descripcion   : Servicio de negocio para registrar y consultar proveedores (RF-08, CU-04).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.Proveedor;
import pe.edu.upn.bodega.repository.ProveedorRepository;
import pe.edu.upn.bodega.util.NegocioException;
import pe.edu.upn.bodega.util.RecursoNoEncontradoException;

import java.util.List;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final AuditoriaService auditoriaService;

    public ProveedorService(ProveedorRepository proveedorRepository,
                            AuditoriaService auditoriaService) {
        this.proveedorRepository = proveedorRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listar() {
        return proveedorRepository.findByActivoTrueOrderByRazonSocialAsc();
    }

    @Transactional(readOnly = true)
    public Proveedor obtener(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el proveedor solicitado."));
    }

    @Transactional
    public Proveedor registrar(Proveedor proveedor) {
        if (proveedor.getRuc() == null || proveedor.getRuc().length() != 11) {
            throw new NegocioException("El RUC debe tener 11 digitos.");
        }
        if (proveedorRepository.existsByRuc(proveedor.getRuc())) {
            throw new NegocioException("Ya existe un proveedor registrado con ese RUC.");
        }
        auditoriaService.marcarUsuarioBd();
        proveedor.setId(null);
        proveedor.setActivo(Boolean.TRUE);
        Proveedor guardado = proveedorRepository.save(proveedor);

        auditoriaService.registrar(AccionAuditoria.CREAR, "PROVEEDOR", guardado.getId(),
                "Se registro el proveedor " + guardado.getRazonSocial(), null,
                "{\"ruc\":\"" + guardado.getRuc() + "\",\"razon_social\":\"" + guardado.getRazonSocial() + "\"}");
        return guardado;
    }

    @Transactional
    public Proveedor actualizar(Long id, Proveedor datos) {
        Proveedor proveedor = obtener(id);
        auditoriaService.marcarUsuarioBd();
        String anterior = "{\"razon_social\":\"" + proveedor.getRazonSocial()
                + "\",\"telefono\":\"" + proveedor.getTelefono() + "\"}";

        proveedor.setRazonSocial(datos.getRazonSocial());
        proveedor.setContacto(datos.getContacto());
        proveedor.setTelefono(datos.getTelefono());
        proveedor.setEmail(datos.getEmail());
        proveedor.setDireccion(datos.getDireccion());
        if (datos.getActivo() != null) {
            proveedor.setActivo(datos.getActivo());
        }
        Proveedor guardado = proveedorRepository.save(proveedor);

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "PROVEEDOR", guardado.getId(),
                "Se actualizo el proveedor " + guardado.getRazonSocial(), anterior,
                "{\"razon_social\":\"" + guardado.getRazonSocial()
                        + "\",\"telefono\":\"" + guardado.getTelefono() + "\"}");
        return guardado;
    }
}
