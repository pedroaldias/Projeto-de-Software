package model;

public class Amphibian extends Species {

    public Amphibian(int id, String name, ConservationStatus status, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
    }

    @Override
    public String describeHabitat() {
        return "Anfíbio (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + describeCommonTraits();
    }
}