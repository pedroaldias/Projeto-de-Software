package api;

import model.*;
import java.util.List;

public class EnrichedSpeciesDataSource implements SpeciesDataSource {

    private final SpeciesDataSource gbif;
    private final WikiDataClient wikidata;

    public EnrichedSpeciesDataSource(SpeciesDataSource gbif, WikiDataClient wikidata) {
        this.gbif = gbif;
        this.wikidata = wikidata;
    }

    @Override
    public Species fetchSpecies(int speciesId) {
        Species base = gbif.fetchSpecies(speciesId);
        if (base == null) {
            return null;
        }
        WikiDataTraits traits = wikidata.fetchTraits(base.getScientificName());
        return enrich(base, traits);
    }

    // Species é imutável, então enriquecer = reconstruir a mesma subclasse
    // com os traits do Wikidata no lugar dos placeholders "-" do SpeciesFactory.
    private Species enrich(Species base, WikiDataTraits traits) {
        int id = base.getId();
        String name = base.getScientificName();
        ConservationStatus status = base.getStatus();

        String habitat = valorOuPlaceholder(traits.habitat());
        String diet = valorOuPlaceholder(traits.diet());
        String length = valorOuPlaceholder(traits.length());
        String mass = valorOuPlaceholder(traits.mass());

        return switch (base) {
            // Correspondência real: P2050 (wingspan) é o campo específico em si.
            case Insect _ -> new Insect(id, name, status, valorOuPlaceholder(traits.wingspan()), habitat, diet, length, mass);

            // Correspondência real: P9566 (diel cycle) é o campo específico em si.
            case Mammal _ -> new Mammal(id, name, status, valorOuPlaceholder(traits.dielCycle()), habitat, diet, length, mass);

            // Sem trait estruturado equivalente pro campo específico — habitat
            // (P2974) entra como aproximação nesse campo; habitat/diet/length/mass
            // continuam sendo dados reais e comuns, independente dessa aproximação.
            case Bird _ -> new Bird(id, name, status, habitat, habitat, diet, length, mass);
            case Amphibian _ -> new Amphibian(id, name, status, habitat, habitat, diet, length, mass);
            case Crustacean _ -> new Crustacean(id, name, status, habitat, habitat, diet, length, mass);
            case Fish _ -> new Fish(id, name, status, habitat, habitat, diet, length, mass);
            case Mollusk _ -> new Mollusk(id, name, status, habitat, habitat, diet, length, mass);
            case Reptile _ -> new Reptile(id, name, status, habitat, habitat, diet, length, mass);
            case Plant _ -> new Plant(id, name, status, habitat, "-", habitat, diet, length, mass);

            // taxonomicClass não é trait ecológico — preservado como veio do GBIF.
            case GenericSpecies g -> new GenericSpecies(id, name, status, g.getTaxonomicClass(), habitat, diet, length, mass);

            default -> base;
        };
    }


    private String valorOuPlaceholder(String valor) {
        return valor != null ? valor : "-";
    }

    // --- Wikidata não sabe buscar nem paginar — delega 100% ao GBIF ---

    @Override
    public SearchSession searchByVernacular(String termo, int pageSize) {
        return gbif.searchByVernacular(termo, pageSize);
    }

    @Override
    public SearchSession searchByScientific(String termo, int pageSize) {
        return gbif.searchByScientific(termo, pageSize);
    }

    @Override
    public List<String> fetchRawOccurrencesByScientificName(String scientificName, int limit) {
        return gbif.fetchRawOccurrencesByScientificName(scientificName, limit);
    }

    @Override
    public ImportResult importarOcorrencias(List<String> jsonsBrutos, Species especie) {
        return gbif.importarOcorrencias(jsonsBrutos, especie);
    }
}