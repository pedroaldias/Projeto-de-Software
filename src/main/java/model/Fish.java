package model;

public class Fish extends Species {
    private String migrationRoute;

    public Fish(int id, String name, ConservationStatus status, String migrationRoute, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
        this.migrationRoute = migrationRoute;
    }

    @Override
    public String describeHabitat() {
        return "Ave (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Rota migratória: " + this.migrationRoute + describeCommonTraits();
    }
}