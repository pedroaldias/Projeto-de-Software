package model;

public class Bird extends Species {

    public Bird(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Ave (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}