/*
 * Proyecto      : Sistema Web de Ventas e Inventario - Bodega Armando Tornero
 * Archivo       : SeguridadConfig.java
 * Descripcion   : Configuracion de autenticacion y autorizacion. Define el cifrado BCrypt de las contrasenas y los permisos de cada rol sobre los recursos del backend (RF-03, RNF-02, AC-03). Todo intento de acceso no autorizado queda registrado en la bitacora de auditoria.
 * Autor         : Juan Cesar Retuerto Ibanez
 * Creado por    : Juan Cesar Retuerto Ibanez
 * Ultima modificacion : Juan Cesar Retuerto Ibanez
 */
package pe.edu.upn.bodega.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.edu.upn.bodega.entity.AccionAuditoria;
import pe.edu.upn.bodega.entity.ResultadoAuditoria;
import pe.edu.upn.bodega.service.AuditoriaService;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    private final AuditoriaService auditoriaService;

    public SeguridadConfig(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion)
            throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(peticiones -> peticiones
                // Recursos publicos de la capa de presentacion
                .requestMatchers("/", "/index.html", "/pages/**", "/css/**", "/js/**",
                                 "/favicon.ico", "/error").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/sesion").permitAll()

                // Catalogo: consulta disponible para los tres roles (RF-06, RF-11)
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**")
                    .hasAnyRole("ADMINISTRADOR", "ALMACENERO", "CAJERO")
                // Registro y actualizacion de productos (RF-05)
                .requestMatchers(HttpMethod.POST, "/api/productos/**")
                    .hasAnyRole("ADMINISTRADOR", "ALMACENERO")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**")
                    .hasAnyRole("ADMINISTRADOR", "ALMACENERO")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMINISTRADOR")

                // Categorias y proveedores (RF-07, RF-08)
                .requestMatchers("/api/categorias/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/proveedores/**")
                    .hasAnyRole("ADMINISTRADOR", "ALMACENERO")
                .requestMatchers("/api/proveedores/**").hasRole("ADMINISTRADOR")

                // Inventario (RF-09 a RF-12)
                .requestMatchers(HttpMethod.GET, "/api/inventario/**")
                    .hasAnyRole("ADMINISTRADOR", "ALMACENERO", "CAJERO")
                .requestMatchers("/api/inventario/**").hasAnyRole("ADMINISTRADOR", "ALMACENERO")

                // Ventas y caja (RF-13 a RF-19)
                .requestMatchers(HttpMethod.POST, "/api/ventas/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/ventas/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                .requestMatchers("/api/caja/**").hasAnyRole("CAJERO", "ADMINISTRADOR")

                // Administracion, reportes y auditoria (RF-04, RF-20 a RF-22)
                .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/reportes/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/auditoria/**").hasRole("ADMINISTRADOR")

                .anyRequest().authenticated()
            )
            .exceptionHandling(manejo -> manejo
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        "{\"ok\":false,\"mensaje\":\"Debe iniciar sesion para continuar.\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    // Deja constancia del intento de acceso no autorizado (prueba PF-08)
                    auditoriaService.registrarIndependiente(
                            AccionAuditoria.ACCESO_DENEGADO,
                            "SEGURIDAD",
                            null,
                            "Intento de acceso no autorizado a " + request.getMethod()
                                    + " " + request.getRequestURI(),
                            ResultadoAuditoria.ERROR,
                            auditoriaService.usernameActual(),
                            auditoriaService.rolActual());

                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        "{\"ok\":false,\"mensaje\":\"Su rol no tiene permiso para ejecutar esta operacion.\"}");
                })
            )
            .logout(logout -> logout.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        // Origenes permitidos durante el desarrollo de la capa de presentacion.
        configuracion.setAllowedOriginPatterns(List.of(
                "http://localhost:*", "http://127.0.0.1:*"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
