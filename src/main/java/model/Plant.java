package model;

public class Plant extends Species {

    public Plant(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Planta (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}