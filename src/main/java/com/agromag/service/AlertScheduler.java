package com.agromag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// Ejecuta evaluacion automatica de alertas cada 1 hora para todos los cultivos
@Service
public class AlertScheduler {

	private static final Logger log = LoggerFactory.getLogger(AlertScheduler.class);

	private final AlertService alertService;

	public AlertScheduler(AlertService alertService) {
		this.alertService = alertService;
	}

	// Cada 1 hora (3600000 ms) — evalua condiciones y genera alertas automaticas
	// initialDelay = 60000 ms (1 min) para permitir que la app se inicialice
	@Scheduled(fixedRate = 3600000, initialDelay = 60000)
	public void runHourlyAlertEvaluation() {
		log.info("alert_scheduler_start");
		try {
			alertService.cleanupOrphanedAlerts();
			int created = alertService.evaluateAndCreateAlertsForAllCrops();
			log.info("alert_scheduler_done alertsCreated={}", created);
		} catch (Exception e) {
			log.error("alert_scheduler_failed error={}", e.getMessage(), e);
		}
	}
}
