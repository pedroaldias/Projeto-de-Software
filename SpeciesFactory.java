public class SpeciesFactory {
    
    // Método estático exatamente como no diagrama
    public static Species createFromAPI(int id, String name, String taxonomicClass) {
        
        // Padrão adotado já que o status não foi passado como parâmetro na Factory do UML
        ConservationStatus defaultStatus = ConservationStatus.NOT_EVALUATED;

        switch (taxonomicClass) {
            case "Aves":
                return new Bird(id, name, defaultStatus, "-");
            case "Magnoliopsida":
                return new Plant(id, name, defaultStatus, "-", "-");
            case "Mammalia":
                return new Mammal(id, name, defaultStatus, "-");
            case "Amphibia":
                return new Amphibian(id, name, defaultStatus, "-");
            case "Reptilia":
                return new Reptile(id, name, defaultStatus, "-");
            case "Insecta":
                return new Insect(id, name, defaultStatus, "-");
            case "Actinopterygii":
                return new Fish(id, name, defaultStatus, "-");
            case "Gastropoda":
                return new Mollusk(id, name, defaultStatus, "-");
            case "Malacostraca":
                return new Crustacean(id, name, defaultStatus, "-");
            default:
                throw new IllegalArgumentException("Categoria taxonômica não mapeada: " + taxonomicClass);
        }
    }
}