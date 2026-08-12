package ch.minilpp;

import ch.minilpp.model.AnneeProjection;
import ch.minilpp.model.Assure;
import ch.minilpp.model.ParametreLpp;
import ch.minilpp.service.PrevoyanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tests sans dépendance : un {@code main}, des assertions maison.
 *
 * <p>C'est volontairement primitif : aucune dépendance n'est nécessaire pour
 * lancer la suite, un JDK suffit.</p>
 *
 * <p>TODO : remplacer tout ce fichier par du JUnit 5 une fois Maven en place.</p>
 */
public class Tests {

    private static int reussis = 0;
    private static int echoues = 0;

    private static final ParametreLpp P2025 = new ParametreLpp(
            2025,
            new BigDecimal("22680.00"),   // seuil d'entrée
            new BigDecimal("26460.00"),   // déduction de coordination
            new BigDecimal("90720.00"),   // salaire max LPP
            new BigDecimal("3780.00"),    // salaire coordonné minimal
            new BigDecimal("0.0125"),     // taux d'intérêt minimal
            new BigDecimal("0.0680"));    // taux de conversion

    public static void main(String[] args) {
        PrevoyanceService s = new PrevoyanceService();

        Assure claire = assure("756.1234.5678.97", "Ruedin", "Claire", "1984-03-12", 80, "84000.00");
        Assure marc = assure("756.2345.6789.01", "Perrin", "Marc", "1970-11-02", 100, "112000.00");
        Assure luca = assure("756.6789.0123.45", "Zbinden", "Luca", "2004-09-09", 50, "21000.00");
        Assure petit = assure("756.9999.9999.99", "Test", "Petit", "1990-01-01", 40, "28000.00");

        // --- salaire coordonné -------------------------------------------------
        egal("salaire coordonné standard", "57540.00", s.salaireCoordonne(claire, P2025));
        egal("salaire plafonné au max LPP", "64260.00", s.salaireCoordonne(marc, P2025));
        egal("sous le seuil d'entrée -> non soumis", "0.00", s.salaireCoordonne(luca, P2025));
        egal("plancher du salaire coordonné", "3780.00", s.salaireCoordonne(petit, P2025));

        // --- barème des bonifications -----------------------------------------
        egal("taux à 24 ans", "0.00", s.tauxBonification(24));
        egal("taux à 25 ans", "0.07", s.tauxBonification(25));
        egal("taux à 42 ans", "0.10", s.tauxBonification(42));
        egal("taux à 54 ans", "0.15", s.tauxBonification(54));
        egal("taux à 55 ans", "0.18", s.tauxBonification(55));
        egal("taux à 65 ans", "0.18", s.tauxBonification(65));

        // --- bonification annuelle --------------------------------------------
        egal("bonification 2026 de Claire (42 ans, 10 %)", "5754.00",
                s.bonificationAnnuelle(claire, P2025, 2026));

        // --- projection --------------------------------------------------------
        List<AnneeProjection> proj = s.projection(claire, P2025, BigDecimal.ZERO, 2026, 65);
        vrai("la projection couvre 42 -> 65 ans (24 lignes)", proj.size() == 24);
        egal("1re année : pas d'intérêt sur un avoir nul", "0.00", proj.get(0).interet());
        egal("1re année : avoir final = bonification", "5754.00", proj.get(0).avoirFinal());
        egal("2e année : intérêt sur 5754.00 à 1,25 %", "71.93", proj.get(1).interet());
        egal("2e année : avoir final", "11579.93", proj.get(1).avoirFinal());

        // --- rente -------------------------------------------------------------
        BigDecimal avoir = new BigDecimal("226862.74");
        egal("rente annuelle au taux de conversion 6,8 %", "15426.67", s.renteAnnuelle(avoir, P2025));
        egal("rente mensuelle", "1285.56", s.renteMensuelle(avoir, P2025));

        // --- variante : déduction adaptée au taux d'activité ---------------------
        PrevoyanceService adapte = new PrevoyanceService(true);
        egal("Claire à 80 % avec déduction adaptée", "51408.00", adapte.salaireCoordonne(claire, P2025));

        // --- validation du modèle ------------------------------------------------
        leve("un numéro AVS invalide est refusé",
                () -> assure("123.4567.8901.23", "Faux", "Numéro", "1990-01-01", 100, "50000.00"));
        leve("un taux d'activité de 0 % est refusé",
                () -> assure("756.1111.2222.33", "Faux", "Taux", "1990-01-01", 0, "50000.00"));

        // --- pourquoi jamais de double ---------------------------------------------
        vrai("0.1 + 0.2 != 0.3 en double (raison d'être de BigDecimal)",
                0.1 + 0.2 != 0.3);
        vrai("0.1 + 0.2 == 0.3 en BigDecimal",
                new BigDecimal("0.1").add(new BigDecimal("0.2"))
                        .compareTo(new BigDecimal("0.3")) == 0);

        System.out.printf("%n%d réussis, %d échoués%n", reussis, echoues);
        if (echoues > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ outillage

    private static Assure assure(String noAvs, String nom, String prenom,
                                 String naissance, int taux, String salaire) {
        return new Assure(0, noAvs, nom, prenom, LocalDate.parse(naissance), taux,
                new BigDecimal(salaire), LocalDate.parse("2020-01-01"));
    }

    private static void egal(String libelle, String attendu, BigDecimal obtenu) {
        boolean ok = obtenu != null && new BigDecimal(attendu).compareTo(obtenu) == 0;
        rapporter(libelle + (ok ? "" : " (attendu " + attendu + ", obtenu " + obtenu + ")"), ok);
    }

    private static void vrai(String libelle, boolean condition) {
        rapporter(libelle, condition);
    }

    private static void leve(String libelle, Runnable action) {
        boolean ok;
        try {
            action.run();
            ok = false;
        } catch (IllegalArgumentException e) {
            ok = true;
        }
        rapporter(libelle, ok);
    }

    private static void rapporter(String libelle, boolean ok) {
        System.out.println((ok ? "  OK   " : "  ECHEC") + "  " + libelle);
        if (ok) {
            reussis++;
        } else {
            echoues++;
        }
    }
}
