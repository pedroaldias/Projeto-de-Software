package model;

public class GenericSpecies extends Species {
    private String taxonomicClass;

    public GenericSpecies(int id, String name, ConservationStatus status, String taxonomicClass) {
        super(id, name, status);
        this.taxonomicClass = taxonomicClass;
    }

    @Override
    public String describeHabitat() {
        return "Espécie Genérica (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Categoria taxonômica original: " + this.taxonomicClass;
    }
}