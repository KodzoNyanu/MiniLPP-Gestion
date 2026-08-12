package ch.minilpp.service;

import ch.minilpp.model.AnneeProjection;
import ch.minilpp.model.Assure;
import ch.minilpp.model.BaremeBonification;
import ch.minilpp.model.ParametreLpp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Le cœur métier : salaire coordonné, bonifications, avoir de vieillesse, rente.
 *
 * <p>Deux règles non négociables dans ce fichier :</p>
 * <ol>
 *   <li>Tous les montants sont des {@link BigDecimal}. Jamais de {@code double} :
 *       {@code 0.1 + 0.2} ne vaut pas {@code 0.3} en virgule flottante, et une
 *       caisse de pension ne peut pas se permettre un centime d'écart.</li>
 *   <li>Aucun accès base de données. Ce service reçoit ses données, il ne va pas
 *       les chercher — c'est ce qui le rend testable sans MariaDB.</li>
 * </ol>
 */
public class PrevoyanceService {

    private static final int ECHELLE = 2;
    private static final RoundingMode ARRONDI = RoundingMode.HALF_UP;

    /**
     * Si vrai, le seuil d'entrée et la déduction de coordination sont adaptés au
     * taux d'activité — pratique courante des plans à temps partiel, mais qui
     * dépend du règlement de la caisse. C'est exactement le genre de règle qui
     * doit être paramétrable et non codée en dur.
     */
    private final boolean deductionAdapteeAuTauxActivite;

    public PrevoyanceService() {
        this(false);
    }

    public PrevoyanceService(boolean deductionAdapteeAuTauxActivite) {
        this.deductionAdapteeAuTauxActivite = deductionAdapteeAuTauxActivite;
    }

    /**
     * Salaire coordonné (= salaire assuré) :
     * salaire brut plafonné, moins la déduction de coordination, borné par le minimum.
     * Retourne zéro si le salaire est sous le seuil d'entrée : l'assuré n'est pas soumis.
     */
    public BigDecimal salaireCoordonne(Assure assure, ParametreLpp p) {
        BigDecimal facteur = facteurTauxActivite(assure);
        BigDecimal seuil = p.seuilEntree().multiply(facteur);
        BigDecimal brut = assure.salaireAnnuelBrut();

        if (brut.compareTo(seuil) < 0) {
            return BigDecimal.ZERO.setScale(ECHELLE);
        }

        BigDecimal plafond = p.salaireMaxLpp().multiply(facteur);
        BigDecimal deduction = p.deductionCoordination().multiply(facteur);
        BigDecimal minimum = p.salaireCoordonneMin().multiply(facteur);

        BigDecimal coordonne = brut.min(plafond).subtract(deduction);
        if (coordonne.compareTo(minimum) < 0) {
            coordonne = minimum;
        }
        return coordonne.setScale(ECHELLE, ARRONDI);
    }

    /** Taux de bonification applicable à l'âge atteint dans l'année. */
    public BigDecimal tauxBonification(int age) {
        return BaremeBonification.pourAge(age).taux();
    }

    /** Bonification de vieillesse annuelle = salaire coordonné × taux lié à l'âge. */
    public BigDecimal bonificationAnnuelle(Assure assure, ParametreLpp p, int annee) {
        BigDecimal taux = tauxBonification(assure.ageDansAnnee(annee));
        return salaireCoordonne(assure, p).multiply(taux).setScale(ECHELLE, ARRONDI);
    }

    /**
     * Projection de l'avoir de vieillesse jusqu'à l'âge de retraite.
     *
     * <p>Convention retenue : l'intérêt de l'année est crédité sur l'avoir au
     * 1er janvier, la bonification est ajoutée en fin d'année (donc sans intérêt
     * la première année). C'est la convention LPP usuelle — et c'est le genre de
     * détail qu'il faut savoir expliquer, parce que c'est là que les écarts de
     * calcul apparaissent entre deux logiciels.</p>
     */
    public List<AnneeProjection> projection(Assure assure,
                                            ParametreLpp p,
                                            BigDecimal avoirActuel,
                                            int anneeDepart,
                                            int ageRetraite) {
        List<AnneeProjection> lignes = new ArrayList<>();
        BigDecimal avoir = avoirActuel.setScale(ECHELLE, ARRONDI);
        BigDecimal salaireCoordonne = salaireCoordonne(assure, p);

        for (int annee = anneeDepart; assure.ageDansAnnee(annee) <= ageRetraite; annee++) {
            int age = assure.ageDansAnnee(annee);
            BigDecimal taux = tauxBonification(age);
            BigDecimal bonification = salaireCoordonne.multiply(taux).setScale(ECHELLE, ARRONDI);
            BigDecimal interet = avoir.multiply(p.tauxInteretMinimal()).setScale(ECHELLE, ARRONDI);
            BigDecimal avoirFinal = avoir.add(interet).add(bonification);

            lignes.add(new AnneeProjection(annee, age, avoir, salaireCoordonne,
                    taux, bonification, interet, avoirFinal));
            avoir = avoirFinal;
        }
        return lignes;
    }

    /** Rente annuelle de vieillesse = avoir accumulé × taux de conversion. */
    public BigDecimal renteAnnuelle(BigDecimal avoirALaRetraite, ParametreLpp p) {
        return avoirALaRetraite.multiply(p.tauxConversion()).setScale(ECHELLE, ARRONDI);
    }

    public BigDecimal renteMensuelle(BigDecimal avoirALaRetraite, ParametreLpp p) {
        return renteAnnuelle(avoirALaRetraite, p)
                .divide(BigDecimal.valueOf(12), ECHELLE, ARRONDI);
    }

    private BigDecimal facteurTauxActivite(Assure assure) {
        if (!deductionAdapteeAuTauxActivite) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(assure.tauxActivite())
                .divide(BigDecimal.valueOf(100), 4, ARRONDI);
    }
}
