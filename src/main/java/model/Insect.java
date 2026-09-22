package model;

public class Insect extends Species {
    private String wingType;

    public Insect(int id, String name, ConservationStatus status, String wingType, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
        this.wingType = wingType;
    }

    @Override
    public String describeHabitat() {
        return "Inseto (" + getScientificName() + ") | Status: " + getStatus().emPortugues() + 
               " | Envergadura: " + this.wingType + describeCommonTraits();
    }
}