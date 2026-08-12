package ch.minilpp;

import ch.minilpp.dao.AssureDao;
import ch.minilpp.dao.ParametreLppDao;
import ch.minilpp.model.AnneeProjection;
import ch.minilpp.model.Assure;
import ch.minilpp.model.ParametreLpp;
import ch.minilpp.service.PrevoyanceService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serveur HTTP de MiniLPP, bâti sur le serveur intégré au JDK — aucune
 * dépendance à installer pour démarrer.
 *
 * <p>Routes :</p>
 * <pre>
 *   GET /api/assures?nom=Ruedin
 *   GET /api/assures/{id}
 *   GET /api/assures/{id}/projection?ageRetraite=65
 *   GET /                    -> web/index.html
 * </pre>
 *
 * <p>Exercice E3.1 : migrer vers Spring Boot ou Javalin. Ici, l'intérêt est de
 * voir ce qu'un framework fait à ta place.</p>
 */
public class Api {

    private static final int PORT = Integer.parseInt(
            Optional.ofNullable(System.getenv("MINILPP_PORT")).orElse("8080"));
    private static final Path RACINE_WEB = Path.of("web");

    private final AssureDao assureDao = new AssureDao();
    private final ParametreLppDao parametreDao = new ParametreLppDao();
    private final PrevoyanceService service = new PrevoyanceService();

    public static void main(String[] args) throws IOException {
        new Api().demarrer();
    }

    public void demarrer() throws IOException {
        HttpServer serveur = HttpServer.create(new InetSocketAddress(PORT), 0);
        serveur.createContext("/api/assures", this::routeAssures);
        serveur.createContext("/", this::routeStatique);
        serveur.setExecutor(null);
        serveur.start();
        System.out.println("MiniLPP démarré sur http://localhost:" + PORT);
        System.out.println("Base : " + Db.url());
    }

    // ------------------------------------------------------------------ routes

    private void routeAssures(HttpExchange ex) throws IOException {
        try {
            String chemin = ex.getRequestURI().getPath().substring("/api/assures".length());
            Map<String, String> params = parametres(ex);

            if (chemin.isEmpty() || chemin.equals("/")) {
                repondreJson(ex, 200, listeAssuresJson(params.getOrDefault("nom", "")));
                return;
            }

            String[] segments = chemin.substring(1).split("/");
            long id = Long.parseLong(segments[0]);
            Assure assure = assureDao.parId(id).orElse(null);
            if (assure == null) {
                repondreJson(ex, 404, "{\"erreur\":\"assuré introuvable\"}");
                return;
            }

            if (segments.length == 1) {
                repondreJson(ex, 200, assureJson(assure));
            } else if (segments[1].equals("projection")) {
                int ageRetraite = Integer.parseInt(params.getOrDefault("ageRetraite", "65"));
                repondreJson(ex, 200, projectionJson(assure, ageRetraite));
            } else {
                repondreJson(ex, 404, "{\"erreur\":\"route inconnue\"}");
            }
        } catch (NumberFormatException e) {
            repondreJson(ex, 400, "{\"erreur\":\"paramètre numérique invalide\"}");
        } catch (Exception e) {
            // En production : logger la stacktrace, ne jamais la renvoyer au client.
            e.printStackTrace();
            repondreJson(ex, 500, "{\"erreur\":" + Json.txt(String.valueOf(e.getMessage())) + "}");
        }
    }

    private void routeStatique(HttpExchange ex) throws IOException {
        String demande = ex.getRequestURI().getPath();
        if (demande.equals("/")) {
            demande = "/index.html";
        }
        Path fichier = RACINE_WEB.resolve(demande.substring(1)).normalize();
        if (!fichier.startsWith(RACINE_WEB) || !Files.isRegularFile(fichier)) {
            repondre(ex, 404, "text/plain; charset=utf-8", "404".getBytes(StandardCharsets.UTF_8));
            return;
        }
        repondre(ex, 200, typeMime(fichier), Files.readAllBytes(fichier));
    }

    // -------------------------------------------------------------- sérialisation

    private String listeAssuresJson(String filtre) throws Exception {
        int annee = LocalDate.now().getYear();
        ParametreLpp p = parametreDao.pourAnnee(annee);
        List<Assure> assures = assureDao.lister(filtre);

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < assures.size(); i++) {
            Assure a = assures.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{')
              .append("\"id\":").append(a.id()).append(',')
              .append("\"noAvs\":").append(Json.txt(a.noAvs())).append(',')
              .append("\"nom\":").append(Json.txt(a.nom())).append(',')
              .append("\"prenom\":").append(Json.txt(a.prenom())).append(',')
              .append("\"age\":").append(a.ageDansAnnee(annee)).append(',')
              .append("\"tauxActivite\":").append(a.tauxActivite()).append(',')
              .append("\"salaireAnnuelBrut\":").append(Json.num(a.salaireAnnuelBrut())).append(',')
              .append("\"salaireCoordonne\":").append(Json.num(service.salaireCoordonne(a, p)))
              .append('}');
        }
        return sb.append(']').toString();
    }

    private String assureJson(Assure a) throws Exception {
        int annee = LocalDate.now().getYear();
        ParametreLpp p = parametreDao.pourAnnee(annee);
        BigDecimal avoir = assureDao.dernierAvoir(a.id());
        return "{"
                + "\"id\":" + a.id() + ","
                + "\"noAvs\":" + Json.txt(a.noAvs()) + ","
                + "\"nom\":" + Json.txt(a.nom()) + ","
                + "\"prenom\":" + Json.txt(a.prenom()) + ","
                + "\"dateNaissance\":" + Json.txt(a.dateNaissance().toString()) + ","
                + "\"age\":" + a.ageDansAnnee(annee) + ","
                + "\"tauxActivite\":" + a.tauxActivite() + ","
                + "\"salaireAnnuelBrut\":" + Json.num(a.salaireAnnuelBrut()) + ","
                + "\"salaireCoordonne\":" + Json.num(service.salaireCoordonne(a, p)) + ","
                + "\"bonification\":" + Json.num(service.bonificationAnnuelle(a, p, annee)) + ","
                + "\"avoirActuel\":" + Json.num(avoir)
                + "}";
    }

    private String projectionJson(Assure a, int ageRetraite) throws Exception {
        int annee = LocalDate.now().getYear();
        ParametreLpp p = parametreDao.pourAnnee(annee);
        BigDecimal avoir = assureDao.dernierAvoir(a.id());
        List<AnneeProjection> lignes = service.projection(a, p, avoir, annee, ageRetraite);
        BigDecimal avoirFinal = lignes.isEmpty() ? avoir : lignes.get(lignes.size() - 1).avoirFinal();

        StringBuilder sb = new StringBuilder("{\"assure\":")
                .append(Json.txt(a.nomComplet()))
                .append(",\"anneeParametres\":").append(p.annee())
                .append(",\"ageRetraite\":").append(ageRetraite)
                .append(",\"avoirALaRetraite\":").append(Json.num(avoirFinal))
                .append(",\"renteAnnuelle\":").append(Json.num(service.renteAnnuelle(avoirFinal, p)))
                .append(",\"renteMensuelle\":").append(Json.num(service.renteMensuelle(avoirFinal, p)))
                .append(",\"lignes\":[");
        for (int i = 0; i < lignes.size(); i++) {
            AnneeProjection l = lignes.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{')
              .append("\"annee\":").append(l.annee()).append(',')
              .append("\"age\":").append(l.age()).append(',')
              .append("\"avoirInitial\":").append(Json.num(l.avoirInitial())).append(',')
              .append("\"salaireCoordonne\":").append(Json.num(l.salaireCoordonne())).append(',')
              .append("\"tauxBonification\":").append(Json.num(l.tauxBonification())).append(',')
              .append("\"bonification\":").append(Json.num(l.bonification())).append(',')
              .append("\"interet\":").append(Json.num(l.interet())).append(',')
              .append("\"avoirFinal\":").append(Json.num(l.avoirFinal()))
              .append('}');
        }
        return sb.append("]}").toString();
    }

    // ------------------------------------------------------------------ plomberie

    private Map<String, String> parametres(HttpExchange ex) {
        Map<String, String> params = new HashMap<>();
        String query = ex.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String paire : query.split("&")) {
            int i = paire.indexOf('=');
            if (i > 0) {
                params.put(java.net.URLDecoder.decode(paire.substring(0, i), StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(paire.substring(i + 1), StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    private void repondreJson(HttpExchange ex, int code, String corps) throws IOException {
        repondre(ex, code, "application/json; charset=utf-8", corps.getBytes(StandardCharsets.UTF_8));
    }

    private void repondre(HttpExchange ex, int code, String typeMime, byte[] corps) throws IOException {
        ex.getResponseHeaders().set("Content-Type", typeMime);
        ex.sendResponseHeaders(code, corps.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(corps);
        }
    }

    private String typeMime(Path fichier) {
        String nom = fichier.getFileName().toString();
        if (nom.endsWith(".html")) return "text/html; charset=utf-8";
        if (nom.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (nom.endsWith(".css")) return "text/css; charset=utf-8";
        return "application/octet-stream";
    }
}
