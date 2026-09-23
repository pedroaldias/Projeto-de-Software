package model;

public class Reptile extends Species {

    public Reptile(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Réptil (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}