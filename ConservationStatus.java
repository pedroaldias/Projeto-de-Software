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
            case "EX": return EXTINCT;
            case "EW": return EXTINCT_IN_THE_WILD;
            case "CR": return CRITICALLY_ENDANGERED;
            case "EN": return ENDANGERED;
            case "VU": return VULNERABLE;
            case "NT": return NEAR_THREATENED;
            case "LC": return LEAST_CONCERN;
            case "DD": return DATA_DEFICIENT;
            default: return NOT_EVALUATED;
        }
    }
}