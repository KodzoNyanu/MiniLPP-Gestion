package ch.minilpp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Accès à la base MariaDB.
 *
 * <p>Volontairement minimal : une fabrique de connexions.</p>
 *
 * <p>TODO : remplacer par un {@code DataSource} HikariCP injecté dans les DAO,
 * au lieu d'un accès statique. C'est le « D » de SOLID.</p>
 */
public final class Db {

    private static final String URL =
            env("MINILPP_DB_URL", "jdbc:mariadb://127.0.0.1:3307/minilpp");
    private static final String USER = env("MINILPP_DB_USER", "minilpp");
    private static final String PASSWORD = env("MINILPP_DB_PASSWORD", "minilpp");

    private Db() {
    }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static String url() {
        return URL;
    }

    private static String env(String nom, String valeurParDefaut) {
        String v = System.getenv(nom);
        return (v == null || v.isBlank()) ? valeurParDefaut : v;
    }
}
