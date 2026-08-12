package ch.minilpp.dao;

import ch.minilpp.Db;
import ch.minilpp.model.Assure;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Couche d'accès aux données : elle connaît SQL et ignore tout du métier.
 * Aucune règle de calcul ne doit apparaître ici.
 */
public class AssureDao {

    private static final String COLONNES =
            "id, no_avs, nom, prenom, date_naissance, taux_activite, salaire_annuel_brut, date_entree";

    public List<Assure> lister(String filtreNom) throws SQLException {
        String sql = "SELECT " + COLONNES + " FROM assure WHERE actif = TRUE "
                + "AND (? = '' OR nom LIKE CONCAT('%', ?, '%') OR prenom LIKE CONCAT('%', ?, '%')) "
                + "ORDER BY nom, prenom";
        String f = filtreNom == null ? "" : filtreNom.trim();
        List<Assure> resultat = new ArrayList<>();
        try (Connection c = Db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f);
            ps.setString(2, f);
            ps.setString(3, f);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultat.add(mapper(rs));
                }
            }
        }
        return resultat;
    }

    public Optional<Assure> parId(long id) throws SQLException {
        String sql = "SELECT " + COLONNES + " FROM assure WHERE id = ?";
        try (Connection c = Db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        }
    }

    /** Dernier avoir de vieillesse connu, ou zéro si l'assuré n'a pas encore d'historique. */
    public BigDecimal dernierAvoir(long assureId) throws SQLException {
        String sql = "SELECT avoir_final FROM avoir_vieillesse WHERE assure_id = ? "
                + "ORDER BY annee DESC LIMIT 1";
        try (Connection c = Db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, assureId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private Assure mapper(ResultSet rs) throws SQLException {
        return new Assure(
                rs.getLong("id"),
                rs.getString("no_avs"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getDate("date_naissance").toLocalDate(),
                rs.getInt("taux_activite"),
                rs.getBigDecimal("salaire_annuel_brut"),
                rs.getDate("date_entree").toLocalDate());
    }
}
