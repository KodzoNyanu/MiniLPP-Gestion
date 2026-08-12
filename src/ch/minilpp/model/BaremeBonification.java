package ch.minilpp.model;

import java.math.BigDecimal;

/**
 * Barème légal minimal des bonifications de vieillesse, par tranche d'âge.
 *
 * <p>Il est ici en dur pour que le calcul soit testable sans base de données.</p>
 *
 * <p>TODO : le remplacer par une {@code Strategy} alimentée depuis la table
 * {@code bareme_bonification}, afin de supporter un plan surobligatoire propre
 * à une caisse cliente sans toucher au service.</p>
 */
public enum BaremeBonification {

    AUCUNE(0, 24, "0.00"),
    TRANCHE_25_34(25, 34, "0.07"),
    TRANCHE_35_44(35, 44, "0.10"),
    TRANCHE_45_54(45, 54, "0.15"),
    TRANCHE_55_PLUS(55, 200, "0.18");

    private final int ageMin;
    private final int ageMax;
    private final BigDecimal taux;

    BaremeBonification(int ageMin, int ageMax, String taux) {
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.taux = new BigDecimal(taux);
    }

    public BigDecimal taux() {
        return taux;
    }

    public static BaremeBonification pourAge(int age) {
        for (BaremeBonification b : values()) {
            if (age >= b.ageMin && age <= b.ageMax) {
                return b;
            }
        }
        return AUCUNE;
    }
}
