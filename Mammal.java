public class Mammal extends Species {
    private String behaviorPattern;

    public Mammal(int id, String name, ConservationStatus status, String behaviorPattern) {
        super(id, name, status);
        this.behaviorPattern = behaviorPattern;
    }

    @Override
    public String describeHabitat() {
        return "Mamífero (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Padrão de comportamento no habitat: " + this.behaviorPattern;
    }
}