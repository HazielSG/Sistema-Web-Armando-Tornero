-- =====================================================================
-- Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
-- Archivo       : 02_auditoria_triggers.sql
-- Descripcion   : Disparadores (triggers) que registran en la tabla
--                 auditoria los cambios realizados directamente sobre las
--                 tablas criticas. Complementan la auditoria que genera la
--                 capa de negocio (AuditoriaService) y permiten detectar
--                 modificaciones efectuadas fuera de la aplicacion.
-- Autor         : Juan Cesar Retuerto Ibanez
-- Creado por    : Juan Cesar Retuerto Ibanez
-- Ultima modificacion : Juan Cesar Retuerto Ibanez
--
-- Nota: la aplicacion define la variable de sesion @usuario_app con el
--       username autenticado. Si la variable no existe, el trigger
--       registra 'SISTEMA_BD' para evidenciar un cambio hecho por fuera
--       de la aplicacion web.
-- =====================================================================

USE bodega_armando;

DELIMITER $$

-- ---------------------------------------------------------------------
-- PRODUCTO
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_producto_insert $$
CREATE TRIGGER trg_producto_insert
AFTER INSERT ON producto
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (username, rol, accion, entidad, entidad_id,
                           descripcion, valor_nuevo, origen, resultado)
    VALUES (IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER', 'CREAR', 'PRODUCTO',
            NEW.id,
            CONCAT('Alta de producto ', NEW.codigo, ' - ', NEW.nombre),
            CONCAT('{"codigo":"', NEW.codigo,
                   '","nombre":"', NEW.nombre,
                   '","precio_venta":', NEW.precio_venta,
                   ',"stock_actual":', NEW.stock_actual,
                   ',"stock_minimo":', NEW.stock_minimo, '}'),
            'TRIGGER_BD', 'EXITO');
END $$

DROP TRIGGER IF EXISTS trg_producto_update $$
CREATE TRIGGER trg_producto_update
AFTER UPDATE ON producto
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (username, rol, accion, entidad, entidad_id,
                           descripcion, valor_anterior, valor_nuevo, origen, resultado)
    VALUES (IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER',
            IF(OLD.activo = TRUE AND NEW.activo = FALSE, 'DESACTIVAR', 'ACTUALIZAR'),
            'PRODUCTO', NEW.id,
            CONCAT('Modificacion de producto ', NEW.codigo, ' - ', NEW.nombre),
            CONCAT('{"nombre":"', OLD.nombre,
                   '","precio_venta":', OLD.precio_venta,
                   ',"stock_actual":', OLD.stock_actual,
                   ',"stock_minimo":', OLD.stock_minimo,
                   ',"activo":', IF(OLD.activo, 'true', 'false'), '}'),
            CONCAT('{"nombre":"', NEW.nombre,
                   '","precio_venta":', NEW.precio_venta,
                   ',"stock_actual":', NEW.stock_actual,
                   ',"stock_minimo":', NEW.stock_minimo,
                   ',"activo":', IF(NEW.activo, 'true', 'false'), '}'),
            'TRIGGER_BD', 'EXITO');
END $$

DROP TRIGGER IF EXISTS trg_producto_delete $$
CREATE TRIGGER trg_producto_delete
AFTER DELETE ON producto
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (username, rol, accion, entidad, entidad_id,
                           descripcion, valor_anterior, origen, resultado)
    VALUES (IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER', 'ELIMINAR', 'PRODUCTO',
            OLD.id,
            CONCAT('Eliminacion fisica del producto ', OLD.codigo, ' - ', OLD.nombre),
            CONCAT('{"codigo":"', OLD.codigo, '","nombre":"', OLD.nombre, '"}'),
            'TRIGGER_BD', 'EXITO');
END $$

-- ---------------------------------------------------------------------
-- USUARIO
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_usuario_insert $$
CREATE TRIGGER trg_usuario_insert
AFTER INSERT ON usuario
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (username, rol, accion, entidad, entidad_id,
                           descripcion, valor_nuevo, origen, resultado)
    VALUES (IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER', 'CREAR', 'USUARIO',
            NEW.id,
            CONCAT('Alta de usuario ', NEW.username, ' con rol ', NEW.rol),
            CONCAT('{"username":"', NEW.username, '","rol":"', NEW.rol, '"}'),
            'TRIGGER_BD', 'EXITO');
END $$

DROP TRIGGER IF EXISTS trg_usuario_update $$
CREATE TRIGGER trg_usuario_update
AFTER UPDATE ON usuario
FOR EACH ROW
BEGIN
    IF NOT (OLD.ultimo_acceso <=> NEW.ultimo_acceso)
       AND OLD.rol = NEW.rol
       AND OLD.activo = NEW.activo
       AND OLD.password_hash = NEW.password_hash THEN
        SET @ignorar = 1;   -- solo se actualizo el ultimo acceso: no se audita aqui
    ELSE
        INSERT INTO auditoria (username, rol, accion, entidad, entidad_id,
                               descripcion, valor_anterior, valor_nuevo, origen, resultado)
        VALUES (IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER',
                IF(OLD.activo = TRUE AND NEW.activo = FALSE, 'DESACTIVAR', 'ACTUALIZAR'),
                'USUARIO', NEW.id,
                CONCAT('Modificacion de la cuenta ', NEW.username),
                CONCAT('{"rol":"', OLD.rol, '","activo":', IF(OLD.activo, 'true', 'false'),
                       ',"clave_cambiada":false}'),
                CONCAT('{"rol":"', NEW.rol, '","activo":', IF(NEW.activo, 'true', 'false'),
                       ',"clave_cambiada":',
                       IF(OLD.password_hash <> NEW.password_hash, 'true', 'false'), '}'),
                'TRIGGER_BD', 'EXITO');
    END IF;
END $$

-- ---------------------------------------------------------------------
-- VENTA
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_venta_insert $$
CREATE TRIGGER trg_venta_insert
AFTER INSERT ON venta
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (id_usuario, username, rol, accion, entidad, entidad_id,
                           descripcion, valor_nuevo, origen, resultado)
    VALUES (NEW.id_usuario, IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER', 'CREAR',
            'VENTA', NEW.id,
            CONCAT('Registro de venta ', NEW.numero_comprobante, ' por S/ ', NEW.total),
            CONCAT('{"comprobante":"', NEW.numero_comprobante,
                   '","total":', NEW.total,
                   ',"metodo_pago":"', NEW.metodo_pago, '"}'),
            'TRIGGER_BD', 'EXITO');
END $$

DROP TRIGGER IF EXISTS trg_venta_update $$
CREATE TRIGGER trg_venta_update
AFTER UPDATE ON venta
FOR EACH ROW
BEGIN
    IF OLD.estado <> NEW.estado THEN
        INSERT INTO auditoria (id_usuario, username, rol, accion, entidad, entidad_id,
                               descripcion, valor_anterior, valor_nuevo, origen, resultado)
        VALUES (NEW.id_usuario, IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER',
                IF(NEW.estado = 'ANULADA', 'ANULAR', 'ACTUALIZAR'),
                'VENTA', NEW.id,
                CONCAT('Cambio de estado de la venta ', NEW.numero_comprobante),
                CONCAT('{"estado":"', OLD.estado, '"}'),
                CONCAT('{"estado":"', NEW.estado, '"}'),
                'TRIGGER_BD', 'EXITO');
    END IF;
END $$

-- ---------------------------------------------------------------------
-- MOVIMIENTO DE INVENTARIO
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_movimiento_insert $$
CREATE TRIGGER trg_movimiento_insert
AFTER INSERT ON movimiento_inventario
FOR EACH ROW
BEGIN
    INSERT INTO auditoria (id_usuario, username, rol, accion, entidad, entidad_id,
                           descripcion, valor_anterior, valor_nuevo, origen, resultado)
    VALUES (NEW.id_usuario, IFNULL(@usuario_app, 'SISTEMA_BD'), 'TRIGGER', 'CREAR',
            'MOVIMIENTO_INVENTARIO', NEW.id,
            CONCAT('Movimiento ', NEW.tipo, ' de ', NEW.cantidad,
                   ' unidad(es). Motivo: ', NEW.motivo),
            CONCAT('{"stock":', NEW.stock_anterior, '}'),
            CONCAT('{"stock":', NEW.stock_resultante, '}'),
            'TRIGGER_BD', 'EXITO');
END $$

-- ---------------------------------------------------------------------
-- Proteccion de la bitacora: la auditoria no se modifica ni se elimina
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_auditoria_no_update $$
CREATE TRIGGER trg_auditoria_no_update
BEFORE UPDATE ON auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Los registros de auditoria no pueden modificarse.';
END $$

DROP TRIGGER IF EXISTS trg_auditoria_no_delete $$
CREATE TRIGGER trg_auditoria_no_delete
BEFORE DELETE ON auditoria
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Los registros de auditoria no pueden eliminarse.';
END $$

DELIMITER ;
