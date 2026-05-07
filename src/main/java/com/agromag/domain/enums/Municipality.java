package com.agromag.domain.enums;

import lombok.Getter;

@Getter
public enum Municipality {
	SANTA_MARTA("Santa Marta", 11.2408, -74.1992),
	CIENAGA("Ciénaga", 11.0069, -74.2478),
	FUNDACION("Fundación", 10.5172, -74.1922),
	ZONA_BANANERA("Zona Bananera", 10.7617, -74.1556),
	ARACATACA("Aracataca", 10.5911, -74.1850),
	EL_RETEN("El Retén", 10.6106, -74.2683),
	PUEBLOVIEJO("Puebloviejo", 10.9825, -74.3103),
	PIVIJAY("Pivijay", 10.4500, -74.7500),
	PLATO("Plato", 9.7925, -74.7814),
	EL_BANCO("El Banco", 9.0003, -73.9753),
	ALGARROBO("Algarrobo", 10.1667, -74.0833),
	ARIGUANI("Ariguaní", 9.8500, -74.0833),
	CERRO_SAN_ANTONIO("Cerro de San Antonio", 10.3333, -74.8667),
	CHIBOLO("Chibolo", 10.0167, -74.6000),
	CONCORDIA("Concordia", 10.2833, -74.6167),
	EL_PINON("El Piñón", 10.3833, -74.9500),
	GUAMAL("Guamal", 9.1500, -74.2167),
	NUEVA_GRANADA("Nueva Granada", 10.0333, -74.3833),
	PEDRAZA("Pedraza", 10.1833, -74.9167),
	PIJINO_DEL_CARMEN("Pijiño del Carmen", 9.3333, -74.4500),
	REMOLINO("Remolino", 10.6500, -74.7167),
	SABANAS_DE_SAN_ANGEL("Sabanas de San Ángel", 9.9333, -74.2167),
	SALAMINA("Salamina", 10.4833, -74.8000),
	SAN_SEBASTIAN("San Sebastián de Buenavista", 9.2333, -74.3833),
	SAN_ZENON("San Zenón", 9.2500, -74.5000),
	SANTA_ANA("Santa Ana", 9.3167, -74.5667),
	SANTA_BARBARA("Santa Bárbara de Pinto", 9.4333, -74.7000),
	SITIONUEVO("Sitionuevo", 10.7833, -74.8667),
	TENERIFE("Tenerife", 10.5667, -74.8500),
	ZAPAYAN("Zapayán", 10.2167, -74.8500);

	private final String displayName;
	private final double latitude;
	private final double longitude;

	Municipality(String displayName, double latitude, double longitude) {
		this.displayName = displayName;
		this.latitude = latitude;
		this.longitude = longitude;
	}
}
