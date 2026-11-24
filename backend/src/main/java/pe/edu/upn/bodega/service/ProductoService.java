/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : ProductoService.java
 * Descripcion   : Servicio de negocio del catalogo de productos: registro, consulta, actualizacion y baja logica (RF-05, RF-06, CU-03, CU-05, CU-10, CU-11).
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.bodega.dto.ProductoRequest;
import pe.edu.upn.bodega.entity.*;
import pe.edu.upn.bodega.repository.*;
import pe.edu.upn.bodega.util.NegocioException;
import pe.edu.upn.bodega.util.RecursoNoEncontradoException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           ProveedorRepository proveedorRepository,
                           MovimientoInventarioRepository movimientoRepository,
                           UsuarioRepository usuarioRepository,
                           AuditoriaService auditoriaService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    /** RF-06: consulta por codigo o nombre. */
    @Transactional(readOnly = true)
    public List<Producto> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listar();
        }
        return productoRepository.buscar(texto.trim());
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontro el producto solicitado."));
    }

    @Transactional(readOnly = true)
    public Producto obtenerPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un producto con el codigo " + codigo + "."));
    }

    /** RF-12: productos que alcanzaron el stock minimo. */
    @Transactional(readOnly = true)
    public List<Producto> listarStockBajo() {
        return productoRepository.listarStockBajo();
    }

    @Transactional
    public Producto registrar(ProductoRequest peticion) {
        if (productoRepository.existsByCodigo(peticion.getCodigo().trim())) {
            throw new NegocioException("Ya existe un producto con el codigo " + peticion.getCodigo() + ".");
        }
        if (peticion.getPrecioVenta().compareTo(peticion.getPrecioCompra()) < 0) {
            throw new NegocioException("El precio de venta no puede ser menor que el precio de compra.");
        }

        auditoriaService.marcarUsuarioBd();

        Categoria categoria = categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new NegocioException("La categoria indicada no existe."));

        Producto producto = new Producto();
        producto.setCodigo(peticion.getCodigo().trim());
        producto.setNombre(peticion.getNombre().trim());
        producto.setDescripcion(peticion.getDescripcion());
        producto.setCategoria(categoria);
        if (peticion.getIdProveedor() != null) {
            producto.setProveedor(proveedorRepository.findById(peticion.getIdProveedor())
                    .orElseThrow(() -> new NegocioException("El proveedor indicado no existe.")));
        }
        producto.setUnidadMedida(peticion.getUnidadMedida() == null ? "UNIDAD" : peticion.getUnidadMedida());
        producto.setPrecioCompra(peticion.getPrecioCompra());
        producto.setPrecioVenta(peticion.getPrecioVenta());
        producto.setStockMinimo(peticion.getStockMinimo());
        producto.setStockActual(peticion.getStockInicial() == null ? 0 : peticion.getStockInicial());
        producto.setActivo(Boolean.TRUE);
        producto.setFechaRegistro(LocalDateTime.now());

        Producto guardado = productoRepository.save(producto);

        // Si se declaro stock inicial se deja constancia del movimiento de inventario.
        if (guardado.getStockActual() > 0) {
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setProducto(guardado);
            movimiento.setTipo(TipoMovimiento.ENTRADA);
            movimiento.setCantidad(guardado.getStockActual());
            movimiento.setStockAnterior(0);
            movimiento.setStockResultante(guardado.getStockActual());
            movimiento.setMotivo("Stock inicial registrado al crear el producto");
            movimiento.setReferencia("ALTA-PRODUCTO");
            movimiento.setUsuario(usuarioAutenticado());
            movimiento.setFecha(LocalDateTime.now());
            movimientoRepository.save(movimiento);
        }

        auditoriaService.registrar(AccionAuditoria.CREAR, "PRODUCTO", guardado.getId(),
                "Se registro el producto " + guardado.getCodigo() + " - " + guardado.getNombre(),
                null, serializar(guardado));
        return guardado;
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest peticion) {
        Producto producto = obtener(id);
        auditoriaService.marcarUsuarioBd();

        String anterior = serializar(producto);

        if (!producto.getCodigo().equals(peticion.getCodigo().trim())
                && productoRepository.existsByCodigo(peticion.getCodigo().trim())) {
            throw new NegocioException("El codigo " + peticion.getCodigo() + " ya pertenece a otro producto.");
        }

        producto.setCodigo(peticion.getCodigo().trim());
        producto.setNombre(peticion.getNombre().trim());
        producto.setDescripcion(peticion.getDescripcion());
        producto.setCategoria(categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new NegocioException("La categoria indicada no existe.")));
        producto.setProveedor(peticion.getIdProveedor() == null ? null
                : proveedorRepository.findById(peticion.getIdProveedor())
                    .orElseThrow(() -> new NegocioException("El proveedor indicado no existe.")));
        producto.setUnidadMedida(peticion.getUnidadMedida());
        producto.setPrecioCompra(peticion.getPrecioCompra());
        producto.setPrecioVenta(peticion.getPrecioVenta());
        producto.setStockMinimo(peticion.getStockMinimo());
        if (peticion.getActivo() != null) {
            producto.setActivo(peticion.getActivo());
        }

        Producto guardado = productoRepository.save(producto);

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "PRODUCTO", guardado.getId(),
                "Se actualizo el producto " + guardado.getCodigo() + " - " + guardado.getNombre(),
                anterior, serializar(guardado));
        return guardado;
    }

    /**
     * CU-05: el producto no se elimina fisicamente para no afectar las ventas
     * historicas; se desactiva y queda registrado en la auditoria.
     */
    @Transactional
    public Producto desactivar(Long id) {
        Producto producto = obtener(id);
        auditoriaService.marcarUsuarioBd();

        producto.setActivo(Boolean.FALSE);
        Producto guardado = productoRepository.save(producto);

        auditoriaService.registrar(AccionAuditoria.DESACTIVAR, "PRODUCTO", guardado.getId(),
                "Se desactivo el producto " + guardado.getCodigo() + " - " + guardado.getNombre(),
                "{\"activo\":true}", "{\"activo\":false}");
        return guardado;
    }

    @Transactional
    public Producto reactivar(Long id) {
        Producto producto = obtener(id);
        auditoriaService.marcarUsuarioBd();
        producto.setActivo(Boolean.TRUE);
        Producto guardado = productoRepository.save(producto);

        auditoriaService.registrar(AccionAuditoria.ACTUALIZAR, "PRODUCTO", guardado.getId(),
                "Se reactivo el producto " + guardado.getCodigo(),
                "{\"activo\":false}", "{\"activo\":true}");
        return guardado;
    }

    private Usuario usuarioAutenticado() {
        String username = auditoriaService.usernameActual();
        return usuarioRepository.findByUsername(username == null ? "" : username)
                .orElseThrow(() -> new NegocioException("No se pudo identificar al usuario de la sesion."));
    }

    private static String serializar(Producto p) {
        return "{\"codigo\":\"" + p.getCodigo()
                + "\",\"nombre\":\"" + p.getNombre()
                + "\",\"precio_venta\":" + p.getPrecioVenta()
                + ",\"stock_actual\":" + p.getStockActual()
                + ",\"stock_minimo\":" + p.getStockMinimo()
                + ",\"activo\":" + p.getActivo() + "}";
    }
}
