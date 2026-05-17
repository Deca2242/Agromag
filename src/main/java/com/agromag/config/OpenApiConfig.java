package com.agromag.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI 3 / Swagger UI para Agromag.
 * Accede a la UI en: http://localhost:8080/swagger-ui.html
 * Accede al JSON en: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";

	@Bean
	public OpenAPI agromagOpenAPI() {
		return new OpenAPI()
				.info(apiInfo())
				.addServersItem(new Server().url("/").description("Servidor local"))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
				.components(new Components()
						.addSecuritySchemes(BEARER_SCHEME, bearerSecurityScheme()));
	}

	private Info apiInfo() {
		return new Info()
				.title("Agromag API")
				.description("""
						API REST de **Agromag 2.0** — plataforma de gestión agrícola con recomendaciones \
						inteligentes de riego, fertilización y fitosanitarios.

						### Autenticación
						Todos los endpoints (salvo `/api/auth/**`) requieren un **JWT de Supabase**.
						Haz clic en el botón **Authorize 🔒** e ingresa el token con el prefijo `Bearer`.
						""")
				.version("2.0.0-SNAPSHOT")
				.contact(new Contact()
						.name("Equipo Agromag")
						.email("contacto@agromag.co"));
	}

	private SecurityScheme bearerSecurityScheme() {
		return new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("JWT emitido por Supabase Auth. Formato: `Bearer <token>`");
	}
}
