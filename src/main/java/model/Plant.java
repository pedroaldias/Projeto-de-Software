package model;

public class Plant extends Species {
    private String biome;
    private String floweringSeason;

    public Plant(int id, String name, ConservationStatus status, String biome, String floweringSeason) {
        super(id, name, status);
        this.biome = biome;
        this.floweringSeason = floweringSeason;
    }

    @Override
    public String describeHabitat() {
        return "Planta (" + getScientificName() + ") | Status: " + getStatus() + 
               " | Bioma: " + this.biome + " | Época de floração: " + this.floweringSeason;
    }
}