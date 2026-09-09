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

    protected String describeCommonTraits() {
        return " | Habitat: " + habitat + " | Hábitos alimentares: " + diet + 
               " | Comprimento: " + length + " | Massa: " + mass;
    }

    public abstract String describeHabitat();
}