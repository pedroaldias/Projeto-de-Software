package model;

import java.util.List;

// Representa uma linha da lista de resultados de busca (ainda não é uma
// Species completa - isso só acontece quando o usuário escolhe um item e
// chamamos fetchSpecies(key) pra pegar os detalhes).
public class SearchResult {

    private final int key;
    private final String scientificName;
    private final String taxonomicClass;
    private final List<String> vernacularNames;

    public SearchResult(int key, String scientificName, String taxonomicClass, List<String> vernacularNames) {
        this.key = key;
        this.scientificName = scientificName;
        this.taxonomicClass = taxonomicClass;
        this.vernacularNames = vernacularNames;
    }

    public int getKey() {
        return key;
    }

    public String getScientificName() {
        return scientificName;
    }

    public String getTaxonomicClass() {
        return taxonomicClass;
    }

    public List<String> getVernacularNames() {
        return vernacularNames;
    }
}