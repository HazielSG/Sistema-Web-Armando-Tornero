-- =====================================================================
-- Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
-- Archivo       : 03_datos_iniciales.sql
-- Descripcion   : Datos de prueba para ejecutar los casos de prueba
--                 PF-01 a PF-10 del informe.
-- Autor         : Juan Cesar Retuerto Ibanez
-- Creado por    : Juan Cesar Retuerto Ibanez
-- Ultima modificacion : Juan Cesar Retuerto Ibanez
--
-- Credenciales de prueba (las contrasenas se almacenan con hash BCrypt):
--   admin    / admin123     -> ADMINISTRADOR
--   almacen  / almacen123   -> ALMACENERO
--   caja     / caja123      -> CAJERO
-- =====================================================================

USE bodega_armando;

SET @usuario_app = 'carga_inicial';

-- ---------------------------------------------------------------------
-- Usuarios
-- ---------------------------------------------------------------------
INSERT INTO usuario (username, password_hash, nombres, apellidos, documento, email, telefono, rol, activo) VALUES
('admin',   '$2b$10$NzyOIczFUjHtlRa2cNoAe..jKaXyCPx8/AxYthE1Y0gZQC5PRqhLy', 'Armando', 'Tornero Salazar', '41256398', 'admin@bodegaarmando.pe',   '987654321', 'ADMINISTRADOR', TRUE),
('almacen', '$2b$10$FjmlBjzWNGMEqpYueCnE1.eLTGcKSvv5RylYHxgzWn3UPb7JtK5Tu', 'Henry',   'Tornero Nanez',   '45871236', 'almacen@bodegaarmando.pe', '987654322', 'ALMACENERO',    TRUE),
('caja',    '$2b$10$1rlo6ICeZ0X2HP/n4tNdBu1n8Ql.PRghHEjtmDbOvuq9qO2S3plba', 'Daniela', 'Rojas Vega',      '72581439', 'caja@bodegaarmando.pe',    '987654323', 'CAJERO',        TRUE);

-- ---------------------------------------------------------------------
-- Categorias
-- ---------------------------------------------------------------------
INSERT INTO categoria (nombre, descripcion) VALUES
('Abarrotes',          'Arroz, azucar, fideos, aceite y menestras'),
('Bebidas',            'Gaseosas, aguas, jugos y rehidratantes'),
('Lacteos',            'Leche, yogurt, queso y mantequilla'),
('Limpieza',           'Detergentes, lejia, jabon y utiles de aseo'),
('Snacks y golosinas', 'Galletas, chocolates y piqueos'),
('Cuidado personal',   'Jabon de tocador, shampoo y papel higienico');

-- ---------------------------------------------------------------------
-- Proveedores
-- ---------------------------------------------------------------------
INSERT INTO proveedor (ruc, razon_social, contacto, telefono, email, direccion) VALUES
('20100055237', 'Alicorp S.A.A.',                 'Jorge Mendoza', '014150800', 'ventas@alicorp.com.pe',   'Av. Argentina 4793, Callao'),
('20100113610', 'Corporacion Lindley S.A.',       'Ruth Palacios', '013154000', 'contacto@lindley.pe',     'Jr. Cajamarca 371, Rimac'),
('20100190797', 'Gloria S.A.',                    'Luis Carrion',  '014706000', 'atencion@gloria.com.pe',  'Av. Republica de Panama 2461, Lima'),
('20512345678', 'Distribuidora Comas E.I.R.L.',   'Maria Quispe',  '997845120', 'ventas@distcomas.pe',     'Av. Tupac Amaru 1250, Comas');

-- ---------------------------------------------------------------------
-- Productos
-- ---------------------------------------------------------------------
INSERT INTO producto (codigo, nombre, descripcion, id_categoria, id_proveedor, unidad_medida, precio_compra, precio_venta, stock_actual, stock_minimo) VALUES
('PRD-0001', 'Arroz Costeno extra 5 kg',        'Saco de arroz extra',             1, 1, 'BOLSA',  21.50, 25.90, 40,  10),
('PRD-0002', 'Azucar rubia Cartavio 1 kg',      'Azucar rubia domestica',          1, 4, 'BOLSA',   3.80,  4.90, 60,  15),
('PRD-0003', 'Aceite Primor 1 L',               'Aceite vegetal',                  1, 1, 'UNIDAD',  8.20,  9.90, 35,  12),
('PRD-0004', 'Fideo Don Vittorio spaghetti',    'Paquete 500 g',                   1, 1, 'UNIDAD',  3.10,  3.90, 50,  15),
('PRD-0005', 'Coca Cola 1.5 L',                 'Gaseosa',                         2, 2, 'UNIDAD',  6.50,  8.50, 48,  12),
('PRD-0006', 'Inca Kola 1.5 L',                 'Gaseosa',                         2, 2, 'UNIDAD',  6.50,  8.50, 45,  12),
('PRD-0007', 'Agua San Luis 625 ml',            'Agua de mesa sin gas',            2, 2, 'UNIDAD',  1.20,  2.00, 72,  24),
('PRD-0008', 'Leche Gloria evaporada 400 g',    'Leche entera en lata',            3, 3, 'UNIDAD',  3.90,  4.80, 60,  18),
('PRD-0009', 'Yogurt Gloria fresa 1 L',         'Yogurt bebible',                  3, 3, 'UNIDAD',  6.80,  8.20, 24,   8),
('PRD-0010', 'Detergente Bolivar 780 g',        'Detergente en polvo',             4, 1, 'BOLSA',   7.40,  9.50, 30,  10),
('PRD-0011', 'Lejia Clorox 1 L',                'Lejia desinfectante',             4, 4, 'UNIDAD',  4.20,  5.50,  8,  10),
('PRD-0012', 'Galleta Soda San Jorge',          'Paquete de 6 unidades',           5, 4, 'PAQUETE', 2.60,  3.50, 40,  12),
('PRD-0013', 'Chocolate Sublime 32 g',          'Chocolate con mani',              5, 4, 'UNIDAD',  1.50,  2.00, 90,  30),
('PRD-0014', 'Papel higienico Elite x4',        'Paquete de 4 rollos',             6, 4, 'PAQUETE', 6.90,  8.90,  6,  10),
('PRD-0015', 'Jabon Bolivar 125 g',             'Jabon de tocador',                6, 1, 'UNIDAD',  2.30,  3.00, 36,  12);

-- ---------------------------------------------------------------------
-- Movimientos de inventario (carga inicial del stock)
-- ---------------------------------------------------------------------
INSERT INTO movimiento_inventario (id_producto, tipo, cantidad, stock_anterior, stock_resultante, motivo, referencia, id_usuario)
SELECT  p.id, 'ENTRADA', p.stock_actual, 0, p.stock_actual,
        'Inventario inicial registrado al implantar el sistema', 'CARGA-INICIAL', 2
FROM    producto p;

-- ---------------------------------------------------------------------
-- Caja aperturada para el cajero (permite registrar ventas de prueba)
-- ---------------------------------------------------------------------
INSERT INTO cierre_caja (id_usuario, fecha_apertura, monto_inicial, estado)
VALUES (3, NOW(), 50.00, 'ABIERTA');

SET @usuario_app = NULL;

-- ---------------------------------------------------------------------
-- Verificacion rapida
-- ---------------------------------------------------------------------
SELECT 'usuarios'   AS tabla, COUNT(*) AS registros FROM usuario
UNION ALL SELECT 'categorias',  COUNT(*) FROM categoria
UNION ALL SELECT 'proveedores', COUNT(*) FROM proveedor
UNION ALL SELECT 'productos',   COUNT(*) FROM producto
UNION ALL SELECT 'movimientos', COUNT(*) FROM movimiento_inventario
UNION ALL SELECT 'auditoria',   COUNT(*) FROM auditoria;

SELECT * FROM v_stock_bajo;
