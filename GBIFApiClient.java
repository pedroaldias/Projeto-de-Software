import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GBIFApiClient {

    private final HttpClient client;

    public GBIFApiClient() {
        // Inicializa o cliente HTTP nativo do Java
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Faz uma requisição GET à API do GBIF usando o ID da espécie (Taxon Key).
     * @param speciesId O ID da espécie no GBIF (ex: 2479359 para Arara-Azul)
     * @return Um objeto do tipo Species criado pela fábrica, ou null se falhar.
     */
    public Species fetchSpecies(int speciesId) {
        String url = "https://api.gbif.org/v1/species/" + speciesId;

        try {
            // 1. Constrói a requisição HTTP GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            // 2. Envia a requisição e recebe a resposta em texto (JSON)
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Verifica se a API respondeu com sucesso (Código 200 OK)
            if (response.statusCode() != 200) {
                System.out.println("Erro na requisição à API. Código HTTP: " + response.statusCode());
                return null;
            }

            String jsonBody = response.body();

            // 3. Extrai os campos cruciais do JSON devolvido pelo GBIF
            String name = extractJsonField(jsonBody, "canonicalName");
            if (name == null || name.isEmpty()) {
                name = extractJsonField(jsonBody, "scientificName");
            }

            String taxonomicClass = extractJsonField(jsonBody, "class");

            if (name == null || taxonomicClass == null) {
                System.out.println("Não foi possível extrair a classe taxonómica ou o nome da resposta da API.");
                return null;
            }

            // 4. Delega a criação do objeto especializado à SpeciesFactory
            return SpeciesFactory.createFromAPI(speciesId, name, taxonomicClass);

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do GBIF: " + e.getMessage());
            return null;
        }
    }

    /**
     * Método auxiliar simples para extrair o valor de uma chave específica num JSON simples usando Regex.
     */
    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}