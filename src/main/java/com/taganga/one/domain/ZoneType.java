package com.taganga.one.domain;

/**
 * Convenciones de la cartografía ZEPAT (Zona Especial de Pesca Ancestral Indígena
 * Taganguera) del Cabildo Indígena de Taganga.
 */
public enum ZoneType {
    /** Zona Especial de Pesca Ancestral: espacio tradicional de pesca con chinchorro. */
    ZEPAT,
    /** Buceo con afectación a la pesca ancestral. */
    DIVING_IMPACT,
    /** Buceo concertado con la comunidad pesquera. */
    DIVING_AGREED,
    /** No se permite bucear. */
    NO_DIVING,
    SNORKEL,
    /** Snorkel con afectación a la pesca ancestral. */
    SNORKEL_IMPACT,
    /** Snorkel sin afectación a la pesca ancestral. */
    SNORKEL_NO_IMPACT,
    /** Embarcaciones con afectación a la pesca ancestral. */
    BOAT_IMPACT,
    /** Turismo con afectación a la pesca ancestral. */
    TOURISM_IMPACT,
    /** Recorrido del gusano inflable. */
    BANANA_BOAT,
    /** Recorrido de kayak. */
    KAYAK,
    /** Recorrido de moto acuática. */
    JET_SKI,
    /** Ruta de transporte turístico. */
    TOURIST_TRANSPORT
}
