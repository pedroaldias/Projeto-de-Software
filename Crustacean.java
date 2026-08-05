public class Crustacean extends Species {
    private String depthZone;

    public Crustacean(int id, String name, ConservationStatus status, String depthZone) {
        super(id, name, status);
        this.depthZone = depthZone;
    }

    @Override
    public String describeHabitat() {
        return "Crustáceo (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Zona de profundidade: " + this.depthZone;
    }
}