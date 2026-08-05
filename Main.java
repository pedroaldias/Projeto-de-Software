public class Main {
    public static void main(String[] args) {
        System.out.println("=== Teste de Conexão Real com a API do GBIF ===\n");

        GBIFApiClient client = new GBIFApiClient();

        // Teste 1: Arara-Azul-Grande (ID GBIF: 2479359 -> Classe: Aves)
        System.out.println("1. A consultar ID 2479359 no GBIF...");
        Species ave = client.fetchSpecies(2479359);
        if (ave != null) {
            System.out.println("Objeto criado: " + ave.getClass().getSimpleName());
            System.out.println("Resultado describeHabitat(): " + ave.describeHabitat());
        }

        System.out.println("\n-----------------------------------\n");

        // Teste 2: Leão (ID GBIF: 5219404 -> Classe: Mammalia)
        System.out.println("2. A consultar ID 5219404 no GBIF...");
        Species mamifero = client.fetchSpecies(5219404);
        if (mamifero != null) {
            System.out.println("Objeto criado: " + mamifero.getClass().getSimpleName());
            System.out.println("Resultado describeHabitat(): " + mamifero.describeHabitat());
        }
        
        System.out.println("\n-----------------------------------\n");

        // Teste 3: Rã (ID GBIF: 2427092 -> Classe: Amphibia)
        System.out.println("3. A consultar ID 2427092 no GBIF...");
        Species anfibio = client.fetchSpecies(2427092);
        if (anfibio != null) {
            System.out.println("Objeto criado: " + anfibio.getClass().getSimpleName());
            System.out.println("Resultado describeHabitat(): " + anfibio.describeHabitat());
        }
    }
}