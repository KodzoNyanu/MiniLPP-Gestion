package ch.minilpp.dao;

import ch.minilpp.Db;
import ch.minilpp.model.ParametreLpp;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ParametreLppDao {

    /** Barème des bonifications, chargé une fois : tranche d'âge -> taux. */
    public Map<int[], BigDecimal> bareme() throws SQLException {
        Map<int[], BigDecimal> bareme = new LinkedHashMap<>();
        String sql = "SELECT age_min, age_max, taux FROM bareme_bonification ORDER BY age_min";
        try (Connection c = Db.open();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                bareme.put(new int[]{rs.getInt("age_min"), rs.getInt("age_max")},
                        rs.getBigDecimal("taux"));
            }
        }
        return bareme;
    }

    /**
     * Paramètres d'une année. Si l'année demandée n'existe pas encore en base
     * (cas réel : on projette 2045), on reprend la dernière année connue —
     * en le documentant, car c'est une hypothèse de calcul, pas une vérité légale.
     */
    public ParametreLpp pourAnnee(int annee) throws SQLException {
        String sql = "SELECT annee, seuil_entree, deduction_coordination, salaire_max_lpp, "
                + "salaire_coordonne_min, taux_interet_minimal, taux_conversion "
                + "FROM parametre_lpp WHERE annee <= ? ORDER BY annee DESC LIMIT 1";
        try (Connection c = Db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, annee);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new NoSuchElementException("Aucun paramètre LPP pour l'année " + annee);
                }
                return new ParametreLpp(
                        rs.getInt("annee"),
                        rs.getBigDecimal("seuil_entree"),
                        rs.getBigDecimal("deduction_coordination"),
                        rs.getBigDecimal("salaire_max_lpp"),
                        rs.getBigDecimal("salaire_coordonne_min"),
                        rs.getBigDecimal("taux_interet_minimal"),
                        rs.getBigDecimal("taux_conversion"));
            }
        }
    }
}
