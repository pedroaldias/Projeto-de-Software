package model;

public class Amphibian extends Species {
    private String waterBodyType;

    public Amphibian(int id, String name, ConservationStatus status, String waterBodyType) {
        super(id, name, status);
        this.waterBodyType = waterBodyType;
    }

    @Override
    public String describeHabitat() {
        return "Anfíbio (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Tipo de corpo de água preferencial: " + this.waterBodyType;
    }
}