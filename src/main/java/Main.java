import api.GBIFApiClient;
import model.SearchResult;
import model.Species;

import java.util.List;
import java.util.Scanner;

public class Main {

    private static final int PAGINA = 20;

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

        // 1) Tenta primeiro por nome popular (vernacular). Se não achar nada,
        //    cai para a busca geral (nome científico ou texto livre).
        boolean usandoNomePopular = true;
        int offset = 0;
        List<SearchResult> pagina = client.searchByVernacular(termo, offset, PAGINA);

        if (pagina.isEmpty()) {
            usandoNomePopular = false;
            pagina = client.searchByScientific(termo, offset, PAGINA);
        }

        if (pagina.isEmpty()) {
            System.out.println("Nenhum resultado encontrado para \"" + termo + "\".\n");
            return;
        }

        while (true) {
            exibirResultados(pagina, offset);
            System.out.print("\nDigite o número do resultado, [N] próxima página, "
                    + "[P] página anterior ou [0] cancelar: ");
            String escolha = scanner.nextLine().trim();

            if (escolha.equalsIgnoreCase("N")) {
                int novoOffset = offset + PAGINA;
                List<SearchResult> proxima = usandoNomePopular
                        ? client.searchByVernacular(termo, novoOffset, PAGINA)
                        : client.searchByScientific(termo, novoOffset, PAGINA);

                if (proxima.isEmpty()) {
                    System.out.println("Não há mais resultados.\n");
                } else {
                    offset = novoOffset;
                    pagina = proxima;
                }
                continue;
            }

            if (escolha.equalsIgnoreCase("P")) {
                if (offset == 0) {
                    System.out.println("Você já está na primeira página.\n");
                    continue;
                }
                int novoOffset = Math.max(0, offset - PAGINA);
                List<SearchResult> anterior = usandoNomePopular
                        ? client.searchByVernacular(termo, novoOffset, PAGINA)
                        : client.searchByScientific(termo, novoOffset, PAGINA);
                offset = novoOffset;
                pagina = anterior;
                continue;
            }

            if (escolha.equals("0")) {
                System.out.println("Busca cancelada.\n");
                return;
            }

            int indice;
            try {
                indice = Integer.parseInt(escolha);
            } catch (NumberFormatException e) {
                System.out.println("Opção inválida.\n");
                continue;
            }

            if (indice < 1 || indice > pagina.size()) {
                System.out.println("Opção inválida.\n");
                continue;
            }

            SearchResult selecionado = pagina.get(indice - 1);
            Species especie = client.fetchSpecies(selecionado.getKey());

            if (especie == null) {
                System.out.println("Não foi possível carregar os detalhes dessa espécie.\n");
                return;
            }

            System.out.println("\n--- Resultado ---");
            System.out.println("ID (taxonKey): " + especie.getId());
            System.out.println("Nome científico: " + especie.getScientificName());
            System.out.println("Categoria: " + especie.getClass().getSimpleName());
            System.out.println("Detalhes: " + especie.describeHabitat());
            System.out.println();
            return;
        }
    }

    private static void exibirResultados(List<SearchResult> pagina, int offset) {
        System.out.println("\n--- Resultados (" + (offset + 1) + " a " + (offset + pagina.size()) + ") ---");
        for (int i = 0; i < pagina.size(); i++) {
            SearchResult r = pagina.get(i);

            String nomesPopulares;
            if (r.getVernacularNames().isEmpty()) {
                nomesPopulares = "-";
            } else {
                int qtd = Math.min(3, r.getVernacularNames().size());
                nomesPopulares = String.join(", ", r.getVernacularNames().subList(0, qtd));
            }

            String classe = r.getTaxonomicClass() == null ? "-" : r.getTaxonomicClass();

            System.out.printf("%2d. %-35s | Popular: %-30s | Classe: %s%n",
                    i + 1, r.getScientificName(), nomesPopulares, classe);
        }
    }
}