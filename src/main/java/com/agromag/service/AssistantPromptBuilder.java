package com.agromag.service;

import com.agromag.domain.enums.CropType;
import com.agromag.domain.enums.Municipality;
import com.agromag.service.AssistantService.AlertSummary;
import com.agromag.service.AssistantService.CropSummary;
import com.agromag.service.AssistantService.EventSummary;
import com.agromag.service.AssistantService.RecommendationSummary;
import com.agromag.service.AssistantService.UserContext;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class AssistantPromptBuilder {

	private AssistantPromptBuilder() {
	}

	static String build(String fullName, Municipality municipality, UserContext ctx, String webContext) {
		String webBlock = webContext == null || webContext.isBlank()
				? "No se usó búsqueda web para este mensaje."
				: webContext;

		return """
				Eres AGROBOT, el asistente agrícola de Agromag para productores del Magdalena, Colombia.
				Responde siempre en español, en texto plano sin Markdown. No uses **, __, #, tablas ni bloques de código. Usa guiones simples si necesitas listar puntos.
				No menciones modelos de lenguaje, proveedores de IA ni herramientas internas. Si te preguntan qué eres, di que eres AGROBOT, un asistente agrícola especializado en cultivos del Magdalena.
				El usuario es %s, del municipio %s.
				Da respuestas concisas, prácticas y accionables, enfocadas en cultivos de la región (banano, mango, yuca, plátano, maíz, palma).
				Da orientación agrícola práctica, no diagnósticos definitivos. Si hay riesgo alto, recomienda inspección del cultivo y validación con un técnico local.
				No recomiendes químicos específicos ni dosis peligrosas sin aclarar que requieren confirmación técnica.
				Si el usuario pide una recomendación formal de riego, fertilización o fitosanitaria, explica que puede generarla desde la ficha del cultivo para usar clima y parámetros actuales.

				== RESUMEN DE CONTEXTO REAL DE LA APP ==
				Cultivos detectados: %d
				Alertas activas sin leer: %d
				Recomendaciones pendientes: %d
				Eventos recientes: %d

				IMPORTANTE: tienes acceso al contexto de la app mostrado abajo (cultivos, alertas, recomendaciones y eventos). No digas que "no puedes ver" los cultivos si hay datos.
				Si el usuario pregunta por sus cultivos, primero confirma cuántos cultivos detectas y menciona al menos uno por tipo.
				Formato preferido: respuesta breve de 2 a 5 líneas, con viñetas solo cuando ayuden.
				Cierra con una acción concreta: revisar alerta, registrar evento, regar, inspeccionar o esperar.
				Usa la siguiente información de finca como referencia; no inventes cultivos que no aparezcan ahí. Si no hay cultivos, orienta sobre cómo registrarlos en la app.

				== CLIMA ACTUAL ==
				%s

				== CULTIVOS REGISTRADOS ==
				%s

				== ALERTAS ACTIVAS SIN LEER ==
				%s

				== RECOMENDACIONES PENDIENTES DE DECISIÓN ==
				%s

				== ACTIVIDADES RECIENTES REGISTRADAS ==
				%s

				== INFORMACIÓN WEB ACTUALIZADA ==
				%s

				Si hay información web, úsala solo como referencia actualizada y menciona las fuentes por nombre o URL.
				Si las fuentes web contradicen el contexto real de la app, prioriza los datos de la app para cultivos, alertas y recomendaciones del usuario.
				Sé proactivo: si hay alertas críticas o recomendaciones pendientes, menciónalas.
				""".formatted(
				fullName != null && !fullName.isBlank() ? fullName : "productor",
				municipality.getDisplayName(),
				ctx.crops().size(),
				ctx.alerts().size(),
				ctx.recommendations().size(),
				ctx.events().size(),
				ctx.climateInfo(),
				buildCropsContextBlock(ctx.crops()),
				buildAlertsContextBlock(ctx.alerts()),
				buildRecommendationsContextBlock(ctx.recommendations()),
				buildRecentEventsBlock(ctx.events()),
				webBlock
		);
	}

	static String buildCropsContextBlock(List<CropSummary> crops) {
		if (crops == null || crops.isEmpty()) {
			return "El productor no tiene cultivos registrados en la app.";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Cultivos registrados del productor (").append(crops.size()).append("):\n");
		for (int i = 0; i < crops.size(); i++) {
			CropSummary c = crops.get(i);
			sb.append("- Cultivo ").append(i + 1).append(": tipo ")
					.append(c.cropType().label())
					.append(", área ").append(c.areaHectares()).append(" ha")
					.append(", municipio del lote ").append(c.municipality().getDisplayName())
					.append(", siembra ").append(c.sownDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
					.append(", etapa estimada: ").append(estimateGrowthStage(c.sownDate(), c.cropType()))
					.append(", plagas comunes: ").append(c.cropType().getCommonPests())
					.append('\n');
		}
		return sb.toString().trim();
	}

	static String estimateGrowthStage(LocalDate sownDate, CropType cropType) {
		if (sownDate == null) return "Desconocida";
		Period p = Period.between(sownDate, LocalDate.now());
		int weeks = (p.getDays() + p.getMonths() * 30 + p.getYears() * 365) / 7;
		return switch (cropType) {
			case BANANO, PLATANO -> weeks < 4 ? "Germinación" : weeks < 12 ? "Crecimiento vegetativo" : weeks < 30 ? "Floración" : "Cosecha/Maduración";
			case MAIZ -> weeks < 2 ? "Germinación" : weeks < 6 ? "Crecimiento vegetativo" : weeks < 10 ? "Floración" : "Maduración/Cosecha";
			case YUCA -> weeks < 3 ? "Germinación" : weeks < 12 ? "Crecimiento vegetativo" : weeks < 30 ? "Desarrollo de raíces" : "Cosecha";
			case MANGO -> weeks < 4 ? "Germinación" : weeks < 16 ? "Crecimiento vegetativo" : weeks < 24 ? "Floración/Frutificación" : "Cosecha";
			case PALMA -> weeks < 8 ? "Vivero" : weeks < 52 ? "Crecimiento vegetativo" : weeks < 156 ? "Desarrollo" : "Producción";
		};
	}

	static String buildAlertsContextBlock(List<AlertSummary> alerts) {
		if (alerts.isEmpty()) {
			return "No hay alertas sin leer.";
		}
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
		StringBuilder sb = new StringBuilder("Alertas activas:\n");
		for (AlertSummary a : alerts) {
			sb.append("- [").append(a.severity().name()).append("] ")
					.append(a.title())
					.append(": ").append(a.message())
					.append(" (").append(a.cropType().label()).append(")")
					.append(", creada ").append(a.createdAt().format(fmt))
					.append('\n');
		}
		return sb.toString().trim();
	}

	static String buildRecommendationsContextBlock(List<RecommendationSummary> recs) {
		if (recs.isEmpty()) {
			return "No hay recomendaciones pendientes de decisión.";
		}
		StringBuilder sb = new StringBuilder("Recomendaciones pendientes:\n");
		for (RecommendationSummary r : recs) {
			sb.append("- [").append(r.type().name()).append("] Nivel: ")
					.append(r.level().name())
					.append(": ").append(r.message())
					.append(" (").append(r.cropType().label()).append(")")
					.append('\n');
		}
		return sb.toString().trim();
	}

	static String buildRecentEventsBlock(List<EventSummary> events) {
		if (events.isEmpty()) {
			return "No hay actividades recientes registradas.";
		}
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
		StringBuilder sb = new StringBuilder("Actividades recientes:\n");
		for (EventSummary e : events) {
			sb.append("- ").append(e.eventType().name())
					.append(" en ").append(e.cropType().label());
			if (e.quantity() != null) {
				sb.append(" (").append(e.quantity()).append(" ").append(e.unit()).append(")");
			}
			if (e.notes() != null && !e.notes().isBlank()) {
				String notes = e.notes();
				sb.append(" — ").append(notes.length() > 80 ? notes.substring(0, 80) + "..." : notes);
			}
			sb.append(", ").append(e.occurredAt().format(fmt)).append('\n');
		}
		return sb.toString().trim();
	}
}
