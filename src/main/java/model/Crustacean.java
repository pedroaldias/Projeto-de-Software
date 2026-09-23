package model;

public class Crustacean extends Species {

    public Crustacean(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Crustáceo (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}