package ch.minilpp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Un assuré affilié à la caisse.
 *
 * <p>Objet immuable ({@code record}) : une fois construit, il ne peut plus être
 * modifié. C'est un choix délibéré — les objets métier immuables sont sûrs à
 * partager entre threads et rendent impossible la modification accidentelle
 * d'un dossier au milieu d'un calcul.</p>
 *
 * <p>Exercice E2.1 : durcir la validation du numéro AVS (clé de contrôle EAN-13).</p>
 */
public record Assure(
        long id,
        String noAvs,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        int tauxActivite,
        BigDecimal salaireAnnuelBrut,
        LocalDate dateEntree) {

    private static final Pattern NO_AVS = Pattern.compile("756\\.\\d{4}\\.\\d{4}\\.\\d{2}");

    /** Constructeur compact : la validation vit ici, donc aucun Assure invalide ne peut exister. */
    public Assure {
        if (noAvs == null || !NO_AVS.matcher(noAvs).matches()) {
            throw new IllegalArgumentException("Numéro AVS invalide : " + noAvs);
        }
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Nom obligatoire");
        }
        if (dateNaissance == null) {
            throw new IllegalArgumentException("Date de naissance obligatoire");
        }
        if (tauxActivite < 1 || tauxActivite > 100) {
            throw new IllegalArgumentException("Taux d'activité hors bornes : " + tauxActivite);
        }
        if (salaireAnnuelBrut == null || salaireAnnuelBrut.signum() < 0) {
            throw new IllegalArgumentException("Salaire annuel brut invalide");
        }
    }

    /**
     * Âge atteint dans l'année civile donnée.
     * En prévoyance professionnelle, l'âge déterminant pour le barème des
     * bonifications est l'âge atteint durant l'année, pas l'âge à la date du jour.
     */
    public int ageDansAnnee(int annee) {
        return annee - dateNaissance.getYear();
    }

    public String nomComplet() {
        return nom + " " + prenom;
    }
}
