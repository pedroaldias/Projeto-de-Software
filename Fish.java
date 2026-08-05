public class Fish extends Species {
    private String waterSalinity;

    public Fish(int id, String name, ConservationStatus status, String waterSalinity) {
        super(id, name, status);
        this.waterSalinity = waterSalinity;
    }

    @Override
    public String describeHabitat() {
        return "Peixe (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Salinidade da água do habitat: " + this.waterSalinity;
    }
}