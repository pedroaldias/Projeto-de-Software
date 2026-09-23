package model;

public class Fish extends Species {

    public Fish(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Peixe (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}