<!--
 Proyecto : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 Archivo : README.md
 Descripcion : Guia de instalacion, estructura y pruebas del proyecto.
 Autor : Juan César Retuerto Ibañez
 Creado por : Juan César Retuerto Ibañez
 Última modificación : Juan César Retuerto Ibañez
-->

# Sistema Web de Ventas e Inventario - Bodega Armando Tornero

Implementación del proyecto descrito en el informe de **Diseño y Arquitectura de Software**:
arquitectura lógica de **tres capas**, con módulo de **registro de auditoría** sobre todas las
operaciones del sistema.

| Capa | Tecnología | Carpeta |
|---|---|---|
| Presentación | HTML, CSS y JavaScript | `frontend/` |
| Lógica de negocio | Java 17 con Spring Boot 3.3 | `backend/` |
| Datos | MySQL 8 | `base-datos/` |

Patrones aplicados: MVC, Service Layer, Repository y DTO. Las contraseñas se almacenan con
hash BCrypt y la autorización se resuelve en el backend por rol (Administrador, Almacenero,
Cajero).

---

## 1. Requisitos previos

- JDK 17 o superior
- Maven 3.8 o superior (o el Maven incluido en NetBeans / IntelliJ / Eclipse)
- MySQL 8.x y MySQL Workbench (o cliente de línea de comandos)
- Navegador actualizado (Chrome, Edge o Firefox)

## 2. Instalación de la base de datos

Ejecutar los scripts **en este orden** desde MySQL Workbench o la consola:

```bash
mysql -u root -p < base-datos/01_esquema.sql
mysql -u root -p < base-datos/02_auditoria_triggers.sql
mysql -u root -p < base-datos/03_datos_iniciales.sql
```

| Script | Contenido |
|---|---|
| `01_esquema.sql` | Base de datos `bodega_armando`, 9 tablas, claves foráneas, restricciones `CHECK`, índices y 2 vistas |
| `02_auditoria_triggers.sql` | Disparadores que registran en `auditoria` los cambios hechos directamente en la base de datos, y bloqueo de modificación o borrado de la bitácora |
| `03_datos_iniciales.sql` | 3 usuarios, 6 categorías, 4 proveedores, 15 productos, movimientos de carga inicial y una caja aperturada |

## 3. Configuración y ejecución del backend

Ajustar el usuario y la contraseña de MySQL en
`backend/src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=root
```

Luego, desde la carpeta `backend/`:

```bash
mvn clean package
mvn spring-boot:run
```

El backend queda escuchando en `http://localhost:8080` y además sirve los archivos de la
carpeta `frontend/`, por lo que **no se necesita otro servidor web**.

## 4. Ingreso al sistema

Abrir `http://localhost:8080` y usar una de las cuentas de prueba:

| Usuario | Contraseña | Rol | Módulos habilitados |
|---|---|---|---|
| `admin` | `admin123` | Administrador | Todos, incluidos usuarios, reportes y auditoría |
| `almacen` | `almacen123` | Almacenero | Productos, inventario y proveedores |
| `caja` | `caja123` | Cajero | Registrar venta, historial, cierre de caja e inventario (solo consulta) |

> Cambiar estas contraseñas antes de cualquier uso real.

---

## 5. Estructura del proyecto

```
bodega-armando-tornero/
    base-datos/
        01_esquema.sql
        02_auditoria_triggers.sql
        03_datos_iniciales.sql
    backend/
        pom.xml
        src/main/java/pe/edu/upn/bodega/
            config/       SeguridadConfig (autenticacion y permisos por rol)
            controller/   11 controladores REST
            dto/          objetos de transferencia y respuesta uniforme
            entity/       9 entidades y 8 enumeraciones
            repository/   9 repositorios Spring Data JPA
            service/      9 servicios de negocio (incluye AuditoriaService)
            util/         excepciones y manejador global de errores
        src/main/resources/application.properties
    frontend/
        index.html        inicio de sesion
        css/estilos.css
        js/               api.js, app.js y un archivo por modulo
        pages/            panel, productos, inventario, ventas, comprobante,
                          historial, caja, proveedores, usuarios, reportes, auditoria
```

## 6. Registro de auditoría

La auditoría trabaja en dos niveles:

**a) Capa de negocio (`AuditoriaService`).** Cada servicio deja constancia de su operación
en la tabla `auditoria`, guardando: fecha y hora, usuario, rol, acción, módulo afectado,
identificador del registro, descripción, **valor anterior y valor nuevo** en formato JSON,
dirección IP, recurso invocado y resultado (éxito o error).

Acciones registradas: `LOGIN`, `LOGOUT`, `LOGIN_FALLIDO`, `CREAR`, `ACTUALIZAR`, `ELIMINAR`,
`DESACTIVAR`, `CONSULTAR`, `EXPORTAR`, `ANULAR` y `ACCESO_DENEGADO`.

Los intentos de acceso fallidos y los accesos denegados se graban en una transacción
independiente, de modo que quedan registrados aunque la operación principal termine en error.

**b) Base de datos (disparadores).** Los triggers de `02_auditoria_triggers.sql` registran los
cambios efectuados sobre `producto`, `usuario`, `venta` y `movimiento_inventario`, incluso si
alguien los modifica por fuera de la aplicación. La aplicación informa al motor el usuario
autenticado mediante la variable de sesión `@usuario_app`; cuando un cambio no viene de la
aplicación, el registro queda con el usuario `SISTEMA_BD`. Además, dos disparadores impiden
modificar o eliminar los registros de la bitácora.

**Consulta.** El módulo *Auditoría* (solo Administrador) permite filtrar por rango de fechas,
usuario, acción y módulo, muestra el resumen de operaciones por acción, pagina los resultados
y exporta la bitácora a CSV. La propia exportación también queda auditada.

## 7. Servicios del backend

| Recurso | Método | Roles | Requisito |
|---|---|---|---|
| `/api/auth/login`, `/logout`, `/sesion` | POST / GET | Público / autenticado | RF-01, RF-02 |
| `/api/dashboard/resumen` | GET | Todos | Panel principal |
| `/api/productos` | GET | Todos | RF-06 |
| `/api/productos` | POST / PUT | Administrador, Almacenero | RF-05 |
| `/api/productos/{id}` | DELETE | Administrador | CU-05 |
| `/api/productos/stock-bajo` | GET | Todos | RF-12 |
| `/api/categorias` | GET / POST / PUT | GET todos; alta y cambio solo Administrador | RF-07 |
| `/api/proveedores` | GET / POST / PUT | GET Administrador y Almacenero; alta y cambio Administrador | RF-08 |
| `/api/inventario/movimientos` | GET | Todos | RF-11 |
| `/api/inventario/movimientos` | POST | Administrador, Almacenero | RF-09, RF-10 |
| `/api/ventas` | POST / GET | Cajero, Administrador | RF-13 a RF-17, RF-20 |
| `/api/ventas/{id}/comprobante` | GET | Cajero, Administrador | RF-18 |
| `/api/ventas/{id}/anular` | PUT | Administrador | Anulación con reposición de stock |
| `/api/caja/apertura`, `/resumen`, `/cierre`, `/historial` | POST / GET | Cajero, Administrador | RF-19 |
| `/api/usuarios` | GET / POST / PUT | Administrador | RF-04, CU-01, CU-02 |
| `/api/reportes/...` | GET | Administrador | RF-21, RF-22 |
| `/api/auditoria`, `/resumen`, `/filtros` | GET | Administrador | RF-20 |

## 8. Reglas de negocio implementadas

- La venta valida el stock disponible **antes** de confirmarse (RF-14).
- El comprobante, su detalle y el descuento de stock se guardan en **una sola transacción**
 (RF-17, RNF-03); si algún producto falla, no se registra nada.
- Los precios de la bodega incluyen IGV: el sistema desagrega el valor de venta y el IGV (18 %).
- El correlativo del comprobante tiene el formato `B001-AAMMDD-NNNN`.
- Los productos no se eliminan físicamente: se desactivan para conservar el historial de ventas.
- Cada movimiento de inventario guarda el stock anterior, el resultante, el motivo y el
 usuario responsable.
- La venta se asocia al turno de caja abierto del cajero; el cierre calcula el efectivo
 esperado y la diferencia frente al dinero contado.
- Al anular una venta se repone el stock y se registra el motivo en la auditoría.
- Un usuario no puede desactivar la cuenta con la que está trabajando.

## 9. Pruebas de funcionalidad del informe

| Código | Prueba | Cómo ejecutarla |
|---|---|---|
| PF-01 | Inicio de sesión | Ingresar con `caja` / `caja123`; luego probar una contraseña errada y revisar el `LOGIN_FALLIDO` en Auditoría |
| PF-02 | Registro de productos | Como `almacen`, registrar un producto y buscarlo en el catálogo |
| PF-03 | Ingreso de stock | Inventario -> movimiento `ENTRADA`; verificar el stock resultante |
| PF-04 | Alerta de stock bajo | Los productos `PRD-0011` y `PRD-0014` inician bajo el mínimo y aparecen en el panel |
| PF-05 | Registro de una venta | Como `caja`, agregar productos, pagar en efectivo y revisar el comprobante |
| PF-06 | Venta sin stock suficiente | Pedir más unidades de las disponibles: el sistema lo impide |
| PF-07 | Generación del comprobante | Botón **Comprobante** en el historial de ventas |
| PF-08 | Permisos por rol | Como `caja`, abrir `pages/usuarios.html`: se bloquea y queda `ACCESO_DENEGADO` en Auditoría |
| PF-09 | Cierre de caja | Cierre de caja -> contar el efectivo y registrar el cierre |
| PF-10 | Consulta y filtrado del historial | Historial de ventas y Auditoría, filtrando por rango de fechas |

## 10. Notas técnicas

- El esquema lo crean los scripts SQL; Hibernate está configurado con
 `spring.jpa.hibernate.ddl-auto=none` y no modifica las tablas.
- La protección CSRF está desactivada porque la interfaz se sirve desde el mismo origen que
 el backend. Si el sistema se publica en Internet, debe habilitarse junto con HTTPS.
- Si la interfaz se abre con otro servidor (por ejemplo Live Server), colocar la dirección del
 backend en la constante `URL_BASE` de `frontend/js/api.js`; el CORS ya admite `localhost`.
- Antes del despliegue conviene programar copias de seguridad de la base de datos (RNF-08).
