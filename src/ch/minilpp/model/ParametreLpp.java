package ch.minilpp.model;

import java.math.BigDecimal;

/**
 * Les paramètres légaux d'une année civile.
 *
 * <p>Ils ne sont JAMAIS écrits en dur dans le code : ils changent chaque année
 * et un logiciel de caisse doit pouvoir recalculer une année passée avec les
 * paramètres de cette année-là. D'où la lecture en base, par année.</p>
 */
public record ParametreLpp(
        int annee,
        BigDecimal seuilEntree,
        BigDecimal deductionCoordination,
        BigDecimal salaireMaxLpp,
        BigDecimal salaireCoordonneMin,
        BigDecimal tauxInteretMinimal,
        BigDecimal tauxConversion) {
}
