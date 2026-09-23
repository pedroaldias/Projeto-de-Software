package model;

public abstract class Species {
    private int id;
    private String scientificName;
    private ConservationStatus status;
    private String habitat;
    private String diet;   // Wikidata P1034 (main food source); "-" quando desconhecido
    private String length;
    private String mass;

    public Species(int id, String scientificName, ConservationStatus status, String habitat, String diet, String length, String mass) {
        if (scientificName == null || scientificName.trim().isEmpty() || scientificName.equalsIgnoreCase("unknown")) {
            throw new IllegalArgumentException("Nome científico inválido ou ausente para a espécie ID: " + id);
        }
        
        this.id = id;
        this.scientificName = scientificName;
        this.status = status;
        this.habitat = habitat;
        this.diet = diet;
        this.length = length;
        this.mass = mass;
    }

    public int getId() { return id; }
    public String getScientificName() { return scientificName; }
    public ConservationStatus getStatus() { return status; }
    public String getHabitat() { return habitat; }
    public String getDiet() { return diet; }
    public String getLength() { return length; }
    public String getMass() { return mass; }

    /**
     * "-" é o marcador interno de "nenhuma das fontes (GBIF/Wikidata/IUCN)
     * tinha esse dado" (ver EnrichedSpeciesDataSource). Ele nunca é
     * exibido ao usuário final: em vez de uma ficha com campos "quebrados"
     * tipo "Dieta: -", a camada de apresentação simplesmente omite o
     * atributo ausente, mostrando uma ficha mais enxuta mas honesta — sem
     * inventar nem aproximar dado que não existe.
     *
     * static e protected para poder ser reaproveitado por qualquer
     * subclasse ao montar seus próprios campos específicos (rota
     * migratória, ciclo de atividade, envergadura, bioma, etc.), sem
     * duplicar essa checagem em cada uma.
     */
    protected static void appendCampo(StringBuilder sb, String rotulo, String valor) {
        if (valor != null && !valor.equals("-")) {
            sb.append(" | ").append(rotulo).append(": ").append(valor);
        }
    }

    protected String describeCommonTraits() {
        StringBuilder sb = new StringBuilder();
        appendCampo(sb, "Habitat", habitat);
        appendCampo(sb, "Hábitos alimentares", diet);
        appendCampo(sb, "Comprimento", length);
        appendCampo(sb, "Massa", mass);
        return sb.toString();
    }

    public abstract String describeHabitat();
}