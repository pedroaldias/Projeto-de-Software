package model;

public class Mammal extends Species {
    private String behaviorPattern;

    public Mammal(int id, String name, ConservationStatus status, String behaviorPattern, String habitat, String diet, String length, String mass) {
        super(id, name, status, habitat, diet, length, mass);
        this.behaviorPattern = behaviorPattern;
    }

    @Override
    public String describeHabitat() {
        return "Mamífero (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Ciclo de atividade: " + this.behaviorPattern + describeCommonTraits();
    }
}