package model;

public class GenericSpecies extends Species {
    private String taxonomicClass;

    public GenericSpecies(int id, String name, ConservationStatus status, String taxonomicClass, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
        this.taxonomicClass = taxonomicClass;
    }

    public String getTaxonomicClass() {
        return taxonomicClass;
    }

    @Override
    public String describeHabitat() {
        return "Espécie Genérica (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Categoria taxonômica original: " + this.taxonomicClass + describeCommonTraits();
    }
}