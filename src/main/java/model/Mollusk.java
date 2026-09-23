package model;

public class Mollusk extends Species {

    public Mollusk(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Molusco (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}