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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class GBIFApiClient {

    // Código do idioma (ISO 639-2/T) usado para filtrar os nomes populares
    // retornados pela API do GBIF. "por" = português.
    private static final String IDIOMA_PADRAO = "por";

    // Quantidade de nomes populares buscados por espécie antes do filtro por
    // idioma (ver comentário em fetchVernacularNamesByLanguage).
    private static final int LIMITE_NOMES_POPULARES = 300;

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

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                return extractJsonField(response.body(), "category");
            }
        } catch (Exception e) {
            // Ignorado, apenas retorna null
        }

        return null;
    }

    // Busca ocorrências reais de uma espécie pelo nome científico.
    public List<String> fetchRawOccurrencesByScientificName(String scientificName, int limit) {
        String encoded = URLEncoder.encode(scientificName, StandardCharsets.UTF_8);
        String url = "https://api.gbif.org/v1/occurrence/search?scientificName=" + encoded
                + "&limit=" + limit;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                System.out.println("Erro na requisição de ocorrências. Código HTTP: " + response.statusCode());
                return new ArrayList<>();
            }

            return extractResultObjects(response.body());
        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do GBIF: " + e.getMessage());
            return new ArrayList<>();
        }
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

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

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
                int taxonKey = Integer.parseInt(keyStr);
                List<String> vernacularNames = fetchVernacularNamesByLanguage(taxonKey, IDIOMA_PADRAO);

                parsed.add(new SearchResult(taxonKey, name, taxonomicClass, vernacularNames));
            }

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do GBIF: " + e.getMessage());
        }

        return deduplicateByScientificName(parsed);
    }

    // Remove nomes científicos repetidos dentro da mesma página de resultados.
    // Quando há mais de um resultado com o mesmo nome científico, mantém apenas
    // aquele que possui o maior número de nomes populares associados.
    private List<SearchResult> deduplicateByScientificName(List<SearchResult> results) {
        Map<String, SearchResult> melhoresPorNome = new LinkedHashMap<>();

        for (SearchResult atual : results) {
            String nomeCientifico = atual.getScientificName();
            SearchResult existente = melhoresPorNome.get(nomeCientifico);

            if (existente == null
                    || atual.getVernacularNames().size() > existente.getVernacularNames().size()) {
                melhoresPorNome.put(nomeCientifico, atual);
            }
        }

        return new ArrayList<>(melhoresPorNome.values());
    }

    // Busca os nomes populares de uma espécie e filtra por idioma no lado do
    // cliente. O parâmetro "language" da API do GBIF nesse endpoint não filtra
    // de fato os resultados (comportamento confirmado empiricamente — a API
    // devolve a mesma lista completa independente do valor enviado), então
    // buscamos um limite alto de nomes e filtramos aqui pelo campo "language"
    // de cada item, comparando com o código ISO 639-2 esperado (ex.: "por").
    private List<String> fetchVernacularNamesByLanguage(int taxonKey, String language) {
        List<String> names = new ArrayList<>();
        String url = "https://api.gbif.org/v1/species/" + taxonKey
                + "/vernacularNames?limit=" + LIMITE_NOMES_POPULARES;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() == 200) {
                for (String raw : extractResultObjects(response.body())) {
                    String idiomaDoItem = extractJsonField(raw, "language");
                    if (idiomaDoItem == null || !idiomaDoItem.equals(language)) {
                        continue;
                    }

                    String nome = extractJsonField(raw, "vernacularName");
                    if (isNomePopularValido(nome) && !names.contains(nome)) {
                        names.add(nome);
                    }
                }
            }
        } catch (Exception e) {
            // Ignorado — em caso de falha, retorna lista vazia para essa espécie
            // sem interromper a listagem dos demais resultados.
        }

        return names;
    }

    // Alguns nomes populares vêm da API do GBIF corrompidos por problema de
    // encoding/charset, aparecendo como sequências de "?" (ex.: "????????").
    // Esse método descarta esses casos para que não poluam a listagem nem
    // interfiram na escolha do resultado com mais nomes populares.
    private boolean isNomePopularValido(String nome) {
        if (nome == null) {
            return false;
        }
        String semEspacos = nome.trim();
        if (semEspacos.isEmpty()) {
            return false;
        }
        for (int i = 0; i < semEspacos.length(); i++) {
            if (semEspacos.charAt(i) != '?') {
                return true;
            }
        }
        // Chegou aqui, significa que a string é composta só por '?' (e/ou espaços).
        return false;
    }

    private Date parseData(String texto) {
        if (texto == null || texto.isEmpty()) {
            return null;
        }

        // eventDate às vezes vem como intervalo ("2023-05-12/2023-05-13"); usamos a data inicial.
        String primeira = texto.split("/")[0].trim();

        try {
            if (primeira.length() > 10) {
                // Tem componente de hora junto (ex.: 2023-05-12T14:30:00)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                return sdf.parse(primeira.substring(0, Math.min(19, primeira.length())));
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(primeira);
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Data de ocorrência em formato inválido: " + texto);
        }
    }

    public static class ResultadoLote {
        public final List<Occurrence> ocorrencias = new ArrayList<>();
        public int descartados = 0;
    }

    public ResultadoLote importarOcorrencias(List<String> jsonsBrutos, Species especie) {
        ResultadoLote resultado = new ResultadoLote();

        for (String raw : jsonsBrutos) {
            try {
                String latStr = extractJsonNumericField(raw, "decimalLatitude");
                String lonStr = extractJsonNumericField(raw, "decimalLongitude");
                String dataStr = extractJsonField(raw, "eventDate");

                if (latStr == null || lonStr == null) {
                    throw new IllegalArgumentException("Ocorrência sem coordenadas.");
                }

                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                Date data = (dataStr != null) ? parseData(dataStr) : null;

                Occurrence occ = new Occurrence(data, lat, lon, especie);
                resultado.ocorrencias.add(occ);

            } catch (IllegalArgumentException e) {
                resultado.descartados++;
            }
        }

        return resultado;
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
