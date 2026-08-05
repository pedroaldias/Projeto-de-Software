public class Mollusk extends Species {
    private String shellType;

    public Mollusk(int id, String name, ConservationStatus status, String shellType) {
        super(id, name, status);
        this.shellType = shellType;
    }

    @Override
    public String describeHabitat() {
        return "Molusco (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Tipo de concha: " + this.shellType;
    }
}