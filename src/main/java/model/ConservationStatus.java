package model;

public enum ConservationStatus {
    EXTINCT,
    EXTINCT_IN_THE_WILD,
    CRITICALLY_ENDANGERED,
    ENDANGERED,
    VULNERABLE,
    NEAR_THREATENED,
    LEAST_CONCERN,
    DATA_DEFICIENT,
    NOT_EVALUATED;

    public static ConservationStatus normalizeAPI(String rawString) {
        if (rawString == null) return NOT_EVALUATED;
        
        switch (rawString.toUpperCase().trim()) {
            case "EX": 
            case "EXTINCT":
                return EXTINCT;
            case "EW": 
            case "EXTINCT_IN_THE_WILD":
                return EXTINCT_IN_THE_WILD;
            case "CR": 
            case "CRITICALLY_ENDANGERED":
                return CRITICALLY_ENDANGERED;
            case "EN": 
            case "ENDANGERED":
                return ENDANGERED;
            case "VU": 
            case "VULNERABLE":
                return VULNERABLE;
            case "NT": 
            case "NEAR_THREATENED":
                return NEAR_THREATENED;
            case "LC": 
            case "LEAST_CONCERN":
                return LEAST_CONCERN;
            case "DD": 
            case "DATA_DEFICIENT":
                return DATA_DEFICIENT;
            default: 
                return NOT_EVALUATED;
        }
    }

    /**
     * Nome da classificação em português, para exibição ao usuário. O nome
     * interno do enum (em inglês) continua sendo usado como identificador
     * estável em qualquer lugar do código que precise comparar/armazenar o
     * status — esta tradução é só para a camada de apresentação.
     */
    public String emPortugues() {
        switch (this) {
            case EXTINCT:
                return "Extinta";
            case EXTINCT_IN_THE_WILD:
                return "Extinta na Natureza";
            case CRITICALLY_ENDANGERED:
                return "Criticamente em Perigo";
            case ENDANGERED:
                return "Em Perigo";
            case VULNERABLE:
                return "Vulnerável";
            case NEAR_THREATENED:
                return "Quase Ameaçada";
            case LEAST_CONCERN:
                return "Pouco Preocupante";
            case DATA_DEFICIENT:
                return "Dados Insuficientes";
            case NOT_EVALUATED:
            default:
                return "Não Avaliada";
        }
    }
}