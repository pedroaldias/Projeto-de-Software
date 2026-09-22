package model;

public class Mollusk extends Species {
    private String migrationRoute;

    public Mollusk(int id, String name, ConservationStatus status, String migrationRoute, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
        this.migrationRoute = migrationRoute;
    }

    @Override
    public String describeHabitat() {
        return "Molusco (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + 
               " | Rota migratória: " + this.migrationRoute + describeCommonTraits();
    }
}