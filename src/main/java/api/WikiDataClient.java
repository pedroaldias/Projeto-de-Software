package api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca traits estruturados no Wikidata pelo nome científico. Não implementa
 * SpeciesDataSource porque não faz busca nem ocorrências — só enriquece um
 * Species já resolvido pelo GBIF.
 */
public class WikiDataClient {

    private static final String SPARQL_ENDPOINT = "https://query.wikidata.org/sparql";
    private final HttpClient client = HttpClient.newHttpClient();

    public WikiDataTraits fetchTraits(String scientificName) {
        String qid = resolveQid(scientificName);
        if (qid == null) {
            return WikiDataTraits.vazio();
        }

        String query = """
            SELECT ?habitatLabel ?dietLabel ?wingspan ?lengthAmount ?lengthUnitLabel
                   ?massAmount ?massUnitLabel ?dielCycleLabel ?eolId WHERE {
              wd:%s wdt:P225 ?ignore .
              OPTIONAL { wd:%s wdt:P2974 ?habitat . }
              OPTIONAL { wd:%s wdt:P1034 ?diet . }
              OPTIONAL { wd:%s wdt:P2050 ?wingspan . }
              OPTIONAL {
                wd:%s p:P2043 ?lengthStmt .
                ?lengthStmt psv:P2043 ?lengthNode .
                ?lengthNode wikibase:quantityAmount ?lengthAmount .
                ?lengthNode wikibase:quantityUnit ?lengthUnit .
              }
              OPTIONAL {
                wd:%s p:P2067 ?massStmt .
                ?massStmt psv:P2067 ?massNode .
                ?massNode wikibase:quantityAmount ?massAmount .
                ?massNode wikibase:quantityUnit ?massUnit .
              }
              OPTIONAL { wd:%s wdt:P9566 ?dielCycle . }
              OPTIONAL { wd:%s wdt:P830 ?eolId . }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "pt,en". }
            }
            """.formatted(qid, qid, qid, qid, qid, qid, qid, qid);

        String json = executeSparql(query);
        if (json == null) {
            return WikiDataTraits.vazio();
        }

        String habitat = extractSparqlValue(json, "habitatLabel");
        String diet = extractSparqlValue(json, "dietLabel");
        String wingspan = extractSparqlValue(json, "wingspan");
        String dielCycle = extractSparqlValue(json, "dielCycleLabel");
        String eolId = extractSparqlValue(json, "eolId");

        String length = formatQuantity(
                extractSparqlValue(json, "lengthAmount"),
                extractSparqlValue(json, "lengthUnitLabel"));
        String mass = formatQuantity(
                extractSparqlValue(json, "massAmount"),
                extractSparqlValue(json, "massUnitLabel"));

        return new WikiDataTraits(habitat, diet, wingspan, length, mass, dielCycle, eolId);
    }

    // Wikidata devolve quantidades com sinal explícito (ex.: "+1.2"). Junta
    // o número com o nome da unidade já resolvido pelo label service, ex.:
    // "1.2 metre". Se o dado ou a unidade não existirem, devolve null
    // (tratado como "-" no decorator).
    private String formatQuantity(String amount, String unitLabel) {
        if (amount == null) {
            return null;
        }
        String semSinal = amount.startsWith("+") ? amount.substring(1) : amount;
        return unitLabel != null ? semSinal + " " + unitLabel : semSinal;
    }

    private String resolveQid(String scientificName) {
        String query = """
                SELECT ?item WHERE {
                  ?item wdt:P225 "%s" .
                } LIMIT 1
                """.formatted(scientificName);

        String json = executeSparql(query);
        if (json == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\"value\"\\s*:\\s*\"http://www\\.wikidata\\.org/entity/(Q\\d+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    // Extrai o "value" associado a uma variável específica do resultado
    // SPARQL, ex.: procurar "habitatLabel":{...,"value":"floresta"} e
    // devolver "floresta". O endpoint do Wikidata devolve JSON com espaços
    // ao redor dos ":", por isso o \\s* antes e depois de cada um.
    private String extractSparqlValue(String json, String variableName) {
        Pattern pattern = Pattern.compile(
                "\"" + variableName + "\"\\s*:\\s*\\{[^}]*\"value\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String executeSparql(String query) {
        try {
            String url = SPARQL_ENDPOINT + "?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/sparql-results+json")
                    .header("User-Agent", "Projeto-de-Software/1.0 (contato@ufal.br)")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200 ? response.body() : null;

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com o Wikidata: " + e.getMessage());
            return null;
        }
    }
}