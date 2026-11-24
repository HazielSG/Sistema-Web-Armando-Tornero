/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : CategoriaService.java
 * Descripcion   : Servicio de negocio para la gestion de categorias de productos (RF-07).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.Categoria;
import pe.edu.upn.bodega.repository.CategoriaRepository;
import pe.edu.upn.bodega.util.NegocioException;
import pe.edu.upn.bodega.util.RecursoNoEncontradoException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final AuditoriaService auditoriaService;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            AuditoriaService auditoriaService) {
        this.categoriaRepository = categoriaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categoriaRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Categoria obtener(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro la categoria solicitada."));
    }

    @Transactional
    public Categoria registrar(Categoria categoria) {
        if (categoriaRepository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new NegocioException("Ya existe una categoria con ese nombre.");
        }
        auditoriaService.marcarUsuarioBd();
        categoria.setId(null);
        categoria.setActivo(Boolean.TRUE);
        Categoria guardada = categoriaRepository.save(categoria);

        auditoriaService.registrar(AccionAuditoria.CREAR, "CATEGORIA", guardada.getId(),
                "Se registro la categoria " + guardada.getNombre(), null,
                "{\"nombre\":\"" + guardada.getNombre() + "\"}");
        return guardada;
    }

    @Transactional
    public Categoria actualizar(Long id, Categoria datos) {
        Categoria categoria = obtener(id);
        auditoriaService.marcarUsuarioBd();
        String anterior = "{\"nombre\":\"" + categoria.getNombre() + "\"}";
        categoria.setNombre(datos.getNombre());
        categoria.setDescripcion(datos.getDescripcion());
        if (datos.getActivo() != null) {
            categoria.setActivo(datos.getActivo());
        }
        Categoria guardada = categoriaRepository.save(categoria);

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "CATEGORIA", guardada.getId(),
                "Se actualizo la categoria " + guardada.getNombre(), anterior,
                "{\"nombre\":\"" + guardada.getNombre() + "\",\"activo\":" + guardada.getActivo() + "}");
        return guardada;
    }
}
