import api.GBIFApiClient;
import model.Species;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GBIFApiClient client = new GBIFApiClient();
        boolean running = true;

        while (running) {
            exibirMenu();
            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1":
                    consultaGeral(scanner, client);
                    break;
                case "2":
                    System.out.println("\n[Listagem de ocorrências ainda será implementada]\n");
                    break;
                case "0":
                    running = false;
                    System.out.println("Encerrando...");
                    break;
                default:
                    System.out.println("Opção inválida. Tente novamente.\n");
            }
        }

        scanner.close();
    }

    private static void exibirMenu() {
        System.out.println("=========================================");
        System.out.println(" SISTEMA DE CONSULTA DE ESPÉCIES - GBIF");
        System.out.println("=========================================");
        System.out.println("1. Consulta geral (buscar espécie por nome)");
        System.out.println("2. Listar ocorrências");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void consultaGeral(Scanner scanner, GBIFApiClient client) {
        System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
        String termo = scanner.nextLine().trim();

        if (termo.isEmpty()) {
            System.out.println("Termo de busca não pode ser vazio.\n");
            return;
        }

        System.out.println("Buscando \"" + termo + "\" na base do GBIF...");
        Species especie = client.searchByName(termo);

        if (especie == null) {
            System.out.println("Nenhuma espécie encontrada para \"" + termo + "\".\n");
            return;
        }

        System.out.println("\n--- Resultado ---");
        System.out.println("ID (taxonKey): " + especie.getId());
        System.out.println("Nome científico: " + especie.getScientificName());
        System.out.println("Categoria: " + especie.getClass().getSimpleName());
        System.out.println("Detalhes: " + especie.describeHabitat());
        System.out.println();
    }
}