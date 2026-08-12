package api;

import model.*;
import factory.SpeciesFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

    public Species searchByName(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.gbif.org/v1/species/search?q=" + encodedQuery + "&limit=1";

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

            String firstResult = extractFirstResultObject(jsonBody);
            if (firstResult == null) {
                System.out.println("Nenhum resultado encontrado para: \"" + query + "\".");
                return null;
            }

            // CORREÇÃO: Busca primeiro pela nubKey (ID canônico). Se não achar, cai para a key genérica.
            String keyStr = extractJsonNumericField(firstResult, "nubKey");
            if (keyStr == null) {
                keyStr = extractJsonNumericField(firstResult, "key");
            }
            
            if (keyStr == null) {
                System.out.println("Não foi possível identificar o ID (taxonKey) do resultado encontrado.");
                return null;
            }

            int speciesId = Integer.parseInt(keyStr);

            return fetchSpecies(speciesId);

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do GBIF: " + e.getMessage());
            return null;
        }
    }

    private String extractFirstResultObject(String json) {
        int resultsIdx = json.indexOf("\"results\"");
        if (resultsIdx == -1) return null;

        int arrayStart = json.indexOf('[', resultsIdx);
        if (arrayStart == -1) return null;

        int objStart = json.indexOf('{', arrayStart);
        if (objStart == -1) return null;

        int depth = 0;
        for (int i = objStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return json.substring(objStart, i + 1);
                }
            }
        }
        return null;
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