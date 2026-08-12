package model;

public class Insect extends Species {
    private String wingType;

    public Insect(int id, String name, ConservationStatus status, String wingType) {
        super(id, name, status);
        this.wingType = wingType;
    }

    @Override
    public String describeHabitat() {
        return "Inseto (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Tipo de asa: " + this.wingType;
    }
}