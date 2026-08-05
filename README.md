## Arquitetura do Sistema

Abaixo encontra-se o Diagrama de Classes UML do nosso projeto, modelado para integrar os dados do GBIF

```mermaid
classDiagram
    direction TB
    
    class ConservationStatus {
        <<enumeration>>
        EXTINCT
        EXTINCT_IN_THE_WILD
        CRITICALLY_ENDANGERED
        ENDANGERED
        VULNERABLE
        NEAR_THREATENED
        LEAST_CONCERN
        DATA_DEFICIENT
        NOT_EVALUATED
        +normalizeAPI(rawString: String)$ ConservationStatus
    }

    class Bird {
        -migrationRoute : String
        +describeHabitat() String
    }

    class Plant {
        -biome : String
        -floweringSeason : String
        +describeHabitat() String
    }

    class Mammal {
        -behaviorPattern : String
        +describeHabitat() String
    }

    class Amphibian {
        -waterBodyType : String
        +describeHabitat() String
    }

    class Reptile {
        -scaleType : String
        +describeHabitat() String
    }

    class Insect {
        -wingType : String
        +describeHabitat() String
    }

    class Fish {
        -waterSalinity : String
        +describeHabitat() String
    }

    class Mollusk {
        -shellType : String
        +describeHabitat() String
    }

    class Crustacean {
        -depthZone : String
        +describeHabitat() String
    }

    class Occurrence {
        -date : Date
        -latitude : double
        -longitude : double
        +setDate(d: Date) void
        +setCoordinates(lat: double, lon: double) void
    }

    class Species {
        -id : int
        -scientificName : String
        -status : ConservationStatus
        +getId() int
        +getScientificName() String
        +getStatus() ConservationStatus
        +describeHabitat()* String
    }
    <<abstract>> Species

    class SpeciesFactory {
        +createFromAPI(id: int, name: String, taxonomicClass: String)$ Species
    }

    %% INHERITANCE RELATIONSHIPS (RF4 - 9 Categories now)
    Species <|-- Bird
    Species <|-- Plant
    Species <|-- Mammal
    Species <|-- Amphibian
    Species <|-- Reptile
    Species <|-- Insect
    Species <|-- Fish
    Species <|-- Mollusk
    Species <|-- Crustacean
    
    %% ASSOCIATION RELATIONSHIPS
    Species --> ConservationStatus : status
    Occurrence --> Species : species_ref
    
    %% DEPENDENCY RELATIONSHIP
    SpeciesFactory ..> Species : creates