public abstract class Species {
    private int id;
    private String scientificName;
    private ConservationStatus status;

    public Species(int id, String scientificName, ConservationStatus status) {
        if (scientificName == null || scientificName.trim().isEmpty() || scientificName.equalsIgnoreCase("unknown")) {
            throw new IllegalArgumentException("Nome científico inválido ou ausente para a espécie ID: " + id);
        }
        
        this.id = id;
        this.scientificName = scientificName;
        this.status = status;
    }

    public int getId() { return id; }
    public String getScientificName() { return scientificName; }
    public ConservationStatus getStatus() { return status; }

    public abstract String describeHabitat();
}