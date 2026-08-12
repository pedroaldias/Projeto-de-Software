package model;

public class Reptile extends Species {
    private String scaleType;

    public Reptile(int id, String name, ConservationStatus status, String scaleType) {
        super(id, name, status);
        this.scaleType = scaleType;
    }

    @Override
    public String describeHabitat() {
        return "Réptil (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Tipo de escama (adaptação ao habitat): " + this.scaleType;
    }
}