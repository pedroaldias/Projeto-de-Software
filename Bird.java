public class Bird extends Species {
    private String migrationRoute;

    public Bird(int id, String name, ConservationStatus status, String migrationRoute) {
        super(id, name, status);
        this.migrationRoute = migrationRoute;
    }

    @Override
    public String describeHabitat() {
        return "Ave (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Rota migratória: " + this.migrationRoute;
    }
}