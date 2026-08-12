package ch.minilpp.model;

import java.math.BigDecimal;

/** Une ligne de projection : ce que devient l'avoir de vieillesse durant une année. */
public record AnneeProjection(
        int annee,
        int age,
        BigDecimal avoirInitial,
        BigDecimal salaireCoordonne,
        BigDecimal tauxBonification,
        BigDecimal bonification,
        BigDecimal interet,
        BigDecimal avoirFinal) {
}
