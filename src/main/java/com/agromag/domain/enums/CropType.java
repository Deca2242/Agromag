package com.agromag.domain.enums;

// Tipos de cultivos soportados en la región del Magdalena
public enum CropType {
<<<<<<< Updated upstream
=======
<<<<<<< Updated upstream
    BANANO,
    MANGO,
    YUCA,
    PLATANO,
    MAIZ,
    PALMA
=======
>>>>>>> Stashed changes
	BANANO("sigatoka negra, trips, picudo negro"),
	MANGO("mosca de la fruta, antracnosis, trips"),
	YUCA("mosca blanca, ácaros, bacteriosis"),
	PLATANO("sigatoka negra, trips, picudo negro"),
	MAIZ("cogollero (Spodoptera), roya, pudrición de mazorca"),
	PALMA("Rhynchophorus palmarum, pudrición del cogollo, roya");

	private final String commonPests;

	CropType(String commonPests) {
		this.commonPests = commonPests;
	}

	// Plagas y enfermedades comunes en la región del Magdalena para este tipo de cultivo
	public String getCommonPests() {
		return commonPests;
	}
<<<<<<< Updated upstream
=======

	// Etiqueta legible para el nombre del cultivo
	public String label() {
		return switch (this) {
			case BANANO -> "Banano";
			case MANGO -> "Mango";
			case YUCA -> "Yuca";
			case PLATANO -> "Plátano";
			case MAIZ -> "Maíz";
			case PALMA -> "Palma";
		};
	}
>>>>>>> Stashed changes
>>>>>>> Stashed changes
}
