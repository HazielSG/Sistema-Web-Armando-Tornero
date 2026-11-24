-- =====================================================================
-- Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
-- Archivo       : 01_esquema.sql
-- Descripcion   : Creacion de la base de datos, tablas, restricciones e
--                 indices de la capa de datos (MySQL 8.x).
-- Autor         : Juan Cesar Retuerto Ibanez
-- Creado por    : Juan Cesar Retuerto Ibanez
-- Ultima modificacion : Juan Cesar Retuerto Ibanez
-- =====================================================================

DROP DATABASE IF EXISTS bodega_armando;
CREATE DATABASE bodega_armando
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE bodega_armando;

-- ---------------------------------------------------------------------
-- Tabla: usuario  (RF-01, RF-02, RF-03, RF-04)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    documento       VARCHAR(15)  NULL,
    email           VARCHAR(120) NULL,
    telefono        VARCHAR(20)  NULL,
    rol             ENUM('ADMINISTRADOR','ALMACENERO','CAJERO') NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso   DATETIME     NULL,
    CONSTRAINT uk_usuario_username UNIQUE (username),
    CONSTRAINT uk_usuario_email    UNIQUE (email)
) ENGINE = InnoDB;

CREATE INDEX idx_usuario_rol ON usuario (rol);

-- ---------------------------------------------------------------------
-- Tabla: categoria  (RF-07)
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_categoria_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: proveedor  (RF-08)
-- ---------------------------------------------------------------------
CREATE TABLE proveedor (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    ruc           VARCHAR(11)  NOT NULL,
    razon_social  VARCHAR(150) NOT NULL,
    contacto      VARCHAR(100) NULL,
    telefono      VARCHAR(20)  NULL,
    email         VARCHAR(120) NULL,
    direccion     VARCHAR(200) NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_proveedor_ruc UNIQUE (ruc)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Tabla: producto  (RF-05, RF-06, RF-11, RF-12)
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(30)    NOT NULL,
    nombre         VARCHAR(150)   NOT NULL,
    descripcion    VARCHAR(255)   NULL,
    id_categoria   BIGINT         NOT NULL,
    id_proveedor   BIGINT         NULL,
    unidad_medida  VARCHAR(20)    NOT NULL DEFAULT 'UNIDAD',
    precio_compra  DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    precio_venta   DECIMAL(10,2)  NOT NULL,
    stock_actual   INT            NOT NULL DEFAULT 0,
    stock_minimo   INT            NOT NULL DEFAULT 0,
    activo         BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_registro DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_producto_codigo UNIQUE (codigo),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor (id),
    CONSTRAINT ck_producto_precio  CHECK (precio_venta >= 0),
    CONSTRAINT ck_producto_stock   CHECK (stock_actual >= 0),
    CONSTRAINT ck_producto_minimo  CHECK (stock_minimo >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_producto_nombre    ON producto (nombre);
CREATE INDEX idx_producto_categoria ON producto (id_categoria);

-- ---------------------------------------------------------------------
-- Tabla: cierre_caja  (RF-19)
-- Se crea antes de venta porque venta referencia el cierre.
-- ---------------------------------------------------------------------
CREATE TABLE cierre_caja (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario       BIGINT        NOT NULL,
    fecha_apertura   DATETIME      NOT NULL,
    fecha_cierre     DATETIME      NULL,
    monto_inicial    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_efectivo   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_tarjeta    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_digital    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_ventas     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    cantidad_ventas  INT           NOT NULL DEFAULT 0,
    monto_declarado  DECIMAL(10,2) NULL,
    diferencia       DECIMAL(10,2) NULL,
    observaciones    VARCHAR(255)  NULL,
    estado           ENUM('ABIERTA','CERRADA') NOT NULL DEFAULT 'ABIERTA',
    CONSTRAINT fk_cierre_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id)
) ENGINE = InnoDB;

CREATE INDEX idx_cierre_estado ON cierre_caja (estado);

-- ---------------------------------------------------------------------
-- Tabla: venta  (RF-13 a RF-18)
-- ---------------------------------------------------------------------
CREATE TABLE venta (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_comprobante  VARCHAR(20)   NOT NULL,
    fecha               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario          BIGINT        NOT NULL,
    id_cierre_caja      BIGINT        NULL,
    cliente_nombre      VARCHAR(150)  NULL,
    cliente_documento   VARCHAR(15)   NULL,
    subtotal            DECIMAL(10,2) NOT NULL,
    igv                 DECIMAL(10,2) NOT NULL,
    total               DECIMAL(10,2) NOT NULL,
    metodo_pago         ENUM('EFECTIVO','TARJETA','YAPE','PLIN','TRANSFERENCIA') NOT NULL,
    estado_pago         ENUM('PAGADO','PENDIENTE')            NOT NULL DEFAULT 'PAGADO',
    estado              ENUM('EMITIDA','ANULADA')             NOT NULL DEFAULT 'EMITIDA',
    monto_recibido      DECIMAL(10,2) NULL,
    vuelto              DECIMAL(10,2) NULL,
    CONSTRAINT uk_venta_comprobante UNIQUE (numero_comprobante),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario)     REFERENCES usuario (id),
    CONSTRAINT fk_venta_cierre  FOREIGN KEY (id_cierre_caja) REFERENCES cierre_caja (id),
    CONSTRAINT ck_venta_total CHECK (total >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_venta_fecha   ON venta (fecha);
CREATE INDEX idx_venta_usuario ON venta (id_usuario);

-- ---------------------------------------------------------------------
-- Tabla: detalle_venta  (RF-15, RF-17)
-- ---------------------------------------------------------------------
CREATE TABLE detalle_venta (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_venta        BIGINT        NOT NULL,
    id_producto     BIGINT        NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_venta    FOREIGN KEY (id_venta)    REFERENCES venta (id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_detalle_venta ON detalle_venta (id_venta);

-- ---------------------------------------------------------------------
-- Tabla: movimiento_inventario  (RF-09, RF-10)
-- ---------------------------------------------------------------------
CREATE TABLE movimiento_inventario (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_producto       BIGINT       NOT NULL,
    tipo              ENUM('ENTRADA','SALIDA','AJUSTE') NOT NULL,
    cantidad          INT          NOT NULL,
    stock_anterior    INT          NOT NULL,
    stock_resultante  INT          NOT NULL,
    motivo            VARCHAR(150) NOT NULL,
    referencia        VARCHAR(50)  NULL,
    id_usuario        BIGINT       NOT NULL,
    fecha             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (id_producto) REFERENCES producto (id),
    CONSTRAINT fk_movimiento_usuario  FOREIGN KEY (id_usuario)  REFERENCES usuario (id),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_movimiento_fecha    ON movimiento_inventario (fecha);
CREATE INDEX idx_movimiento_producto ON movimiento_inventario (id_producto);

-- ---------------------------------------------------------------------
-- Tabla: auditoria  (RNF-02, restriccion etica: registro de acciones)
-- Bitacora de todas las operaciones relevantes del sistema.
-- ---------------------------------------------------------------------
CREATE TABLE auditoria (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario     BIGINT       NULL,
    username       VARCHAR(50)  NOT NULL,
    rol            VARCHAR(20)  NULL,
    accion         ENUM('LOGIN','LOGOUT','LOGIN_FALLIDO','CREAR','ACTUALIZAR',
                        'ELIMINAR','DESACTIVAR','CONSULTAR','EXPORTAR',
                        'ANULAR','ACCESO_DENEGADO') NOT NULL,
    entidad        VARCHAR(50)  NOT NULL,
    entidad_id     VARCHAR(50)  NULL,
    descripcion    VARCHAR(500) NOT NULL,
    valor_anterior TEXT         NULL,
    valor_nuevo    TEXT         NULL,
    direccion_ip   VARCHAR(45)  NULL,
    origen         VARCHAR(255) NULL,
    resultado      ENUM('EXITO','ERROR') NOT NULL DEFAULT 'EXITO',
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id)
) ENGINE = InnoDB;

CREATE INDEX idx_auditoria_fecha   ON auditoria (fecha_hora);
CREATE INDEX idx_auditoria_usuario ON auditoria (username);
CREATE INDEX idx_auditoria_accion  ON auditoria (accion);
CREATE INDEX idx_auditoria_entidad ON auditoria (entidad);

-- ---------------------------------------------------------------------
-- Vista: v_stock_bajo  (RF-12 alerta de stock bajo)
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_stock_bajo AS
SELECT  p.id,
        p.codigo,
        p.nombre,
        c.nombre AS categoria,
        p.stock_actual,
        p.stock_minimo,
        (p.stock_minimo - p.stock_actual) AS faltante
FROM    producto p
JOIN    categoria c ON c.id = p.id_categoria
WHERE   p.activo = TRUE
  AND   p.stock_actual <= p.stock_minimo;

-- ---------------------------------------------------------------------
-- Vista: v_ventas_resumen_dia
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_ventas_resumen_dia AS
SELECT  DATE(v.fecha)      AS dia,
        COUNT(*)           AS cantidad_ventas,
        SUM(v.total)       AS total_vendido,
        SUM(v.igv)         AS total_igv
FROM    venta v
WHERE   v.estado = 'EMITIDA'
GROUP BY DATE(v.fecha);
