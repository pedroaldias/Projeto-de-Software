package api;

import model.*;
import factory.SpeciesFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GBIFApiClient {

    private final HttpClient client;

    public GBIFApiClient() {
        this.client = HttpClient.newHttpClient();
    }

    public Species fetchSpecies(int speciesId) {
        String url = "https://api.gbif.org/v1/species/" + speciesId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Erro na requisição à API. Código HTTP: " + response.statusCode());
                return null;
            }

            String jsonBody = response.body();

            String name = extractJsonField(jsonBody, "canonicalName");
            if (name == null || name.isEmpty()) {
                name = extractJsonField(jsonBody, "scientificName");
            }

            String taxonomicClass = extractJsonField(jsonBody, "class");

            if (name == null || taxonomicClass == null) {
                System.out.println("Não foi possível extrair a classe taxonômica ou o nome da resposta da API.");
                return null;
            }

            String rawStatus = fetchConservationStatus(speciesId);

            return SpeciesFactory.createFromAPI(speciesId, name, taxonomicClass, rawStatus);

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do GBIF: " + e.getMessage());
            return null;
        }
    }

    private String fetchConservationStatus(int speciesId) {
        String url = "https://api.gbif.org/v1/species/" + speciesId + "/iucnRedListCategory";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractJsonField(response.body(), "category");
            }
        } catch (Exception e) {
            // Ignorado, apenas retorna null
        }

        return null;
    }

    // Busca restrita a nome popular/vernacular (qField=VERNACULAR).
    public List<SearchResult> searchByVernacular(String query, int offset, int limit) {
        return search(query, "VERNACULAR", offset, limit);
    }

    // Busca geral (nome científico ou texto livre, sem restringir campo).
    public List<SearchResult> searchByScientific(String query, int offset, int limit) {
        return search(query, null, offset, limit);
    }

    private List<SearchResult> search(String query, String qField, int offset, int limit) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.gbif.org/v1/species/search?q=" + encodedQuery
                + "&limit=" + limit + "&offset=" + offset;
        if (qField != null) {
            url += "&qField=" + qField;
        }

        List<SearchResult> parsed = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Erro na requisição à API. Código HTTP: " + response.statusCode());
                return parsed;
            }

            for (String raw : extractResultObjects(response.body())) {
                String keyStr = extractJsonNumericField(raw, "nubKey");
                if (keyStr == null) {
                    keyStr = extractJsonNumericField(raw, "key");
                }
                if (keyStr == null) {
                    continue;
                }

                String name = extractJsonField(raw, "canonicalName");
                if (name == null || name.isEmpty()) {
                    name = extractJsonField(raw, "scientificName");
                }
                if (name == null) {
                    continue;
                }

                String taxonomicClass = extractJsonField(raw, "class");
                List<String> vernacularNames = extractVernacularNames(raw);

                parsed.add(new SearchResult(Integer.parseInt(keyStr), name, taxonomicClass, vernacularNames));
            }

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do GBIF: " + e.getMessage());
        }

        return parsed;
    }

    private List<String> extractVernacularNames(String resultObject) {
        List<String> names = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"vernacularName\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(resultObject);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    // Extrai cada objeto {...} de dentro do array "results": [...] do JSON,
    // já que não estamos usando uma lib de JSON de verdade.
    private List<String> extractResultObjects(String json) {
        List<String> objects = new ArrayList<>();

        int resultsIdx = json.indexOf("\"results\"");
        if (resultsIdx == -1) return objects;

        int arrayStart = json.indexOf('[', resultsIdx);
        if (arrayStart == -1) return objects;

        int i = arrayStart + 1;
        while (i < json.length()) {
            while (i < json.length() && json.charAt(i) != '{' && json.charAt(i) != ']') {
                i++;
            }
            if (i >= json.length() || json.charAt(i) == ']') break;

            int objStart = i;
            int depth = 0;
            for (; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        i++;
                        break;
                    }
                }
            }
            objects.add(json.substring(objStart, i));
        }

        return objects;
    }

    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractJsonNumericField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\":\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}