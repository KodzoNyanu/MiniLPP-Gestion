package ch.minilpp;

import java.math.BigDecimal;

/**
 * Écriture JSON minimale, sans dépendance externe, pour que le projet démarre
 * avec un simple JDK.
 *
 * <p>Exercice E3.1 : remplacer par Jackson une fois Maven en place. Écrire son
 * propre sérialiseur JSON est une bonne façon de comprendre l'échappement, une
 * mauvaise façon de faire du code de production.</p>
 */
public final class Json {

    private Json() {
    }

    /** Échappe une chaîne pour l'insérer entre guillemets JSON. */
    public static String txt(String valeur) {
        if (valeur == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valeur.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    public static String num(BigDecimal valeur) {
        return valeur == null ? "null" : valeur.toPlainString();
    }
}
