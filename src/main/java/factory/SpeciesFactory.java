package factory;

import model.*;

public class SpeciesFactory {
    
    public static Species createFromAPI(int id, String name, String taxonomicClass, String rawStatus) {
        
        ConservationStatus status = ConservationStatus.normalizeAPI(rawStatus);

        if (taxonomicClass == null) {
            taxonomicClass = "Desconhecida";
        }

        switch (taxonomicClass) {
            case "Aves":
                return new Bird(id, name, status, "-", "-", "-", "-", "-");
            case "Magnoliopsida":
                return new Plant(id, name, status, "-", "-", "-", "-", "-", "-");
            case "Mammalia":
                return new Mammal(id, name, status, "-", "-", "-", "-", "-");
            case "Amphibia":
                return new Amphibian(id, name, status, "-", "-", "-", "-", "-");
            case "Reptilia":
                return new Reptile(id, name, status, "-", "-", "-", "-", "-");
            case "Insecta":
                return new Insect(id, name, status, "-", "-", "-", "-", "-");
            case "Actinopterygii":
                return new Fish(id, name, status, "-", "-", "-", "-", "-");
            case "Gastropoda":
                return new Mollusk(id, name, status, "-", "-", "-", "-", "-");
            case "Malacostraca":
                return new Crustacean(id, name, status, "-", "-", "-", "-", "-");
            default:
                return new GenericSpecies(id, name, status, taxonomicClass, "-", "-", "-", "-");
        }
    }
}