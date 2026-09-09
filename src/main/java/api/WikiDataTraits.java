package api;

public record WikiDataTraits(String habitat, String diet, String wingspan, String length, String mass, String dielCycle, String eolId) {

    public static WikiDataTraits vazio() {
        return new WikiDataTraits(null, null, null, null, null, null, null);
    }
}