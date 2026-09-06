import api.GBIFApiClient;
import model.Occurrence;
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
                    Species especie = resolverEspecie(scanner, client);
                    if (especie != null) {
                        listarOcorrencias(especie, client, scanner);
                    }
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

    private static Species resolverEspecie(Scanner scanner, GBIFApiClient client) {
    System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
    String termo = scanner.nextLine().trim();

    if (termo.isEmpty()) {
        System.out.println("Termo de busca não pode ser vazio.\n");
        return null;
    }

    boolean usandoNomePopular = true;
    int offset = 0;
    List<SearchResult> pagina = client.searchByVernacular(termo, offset, PAGINA);

    if (pagina.isEmpty()) {
        usandoNomePopular = false;
        pagina = client.searchByScientific(termo, offset, PAGINA);
    }

    if (pagina.isEmpty()) {
        System.out.println("Nenhum resultado encontrado para \"" + termo + "\".\n");
        return null;
    }

    while (true) {
        exibirResultados(pagina, offset);
        System.out.print("\nDigite o número do resultado, [N] próxima página, "
                + "[P] página anterior ou [0] cancelar: ");
        String escolha = scanner.nextLine().trim();

        // ... (mesmo bloco de N / P / 0 que já existe hoje em consultaGeral) ...

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
        return client.fetchSpecies(selecionado.getKey());
    }
}

private static void listarOcorrencias(Species especie, GBIFApiClient client, Scanner scanner) {
    List<String> brutos = client.fetchRawOccurrencesByScientificName(especie.getScientificName(), 200);
    GBIFApiClient.ResultadoLote resultado = client.importarOcorrencias(brutos, especie);
    List<Occurrence> ocorrencias = resultado.ocorrencias;

    if (ocorrencias.isEmpty()) {
        System.out.println("\nNenhuma ocorrência válida encontrada para \"" + especie.getScientificName() + "\".");
        System.out.println(resultado.descartados + " registros ignorados por dados inválidos.\n");
        return;
    }

    int offset = 0;
    while (true) {
        exibirOcorrencias(ocorrencias, offset);

        boolean temProxima = offset + PAGINA < ocorrencias.size();
        boolean temAnterior = offset > 0;

        System.out.print("\n" + (temProxima ? "[N] próxima página, " : "")
                + (temAnterior ? "[P] página anterior, " : "") + "[0] voltar: ");
        String escolha = scanner.nextLine().trim();

        if (escolha.equalsIgnoreCase("N") && temProxima) {
            offset += PAGINA;
        } else if (escolha.equalsIgnoreCase("P") && temAnterior) {
            offset = Math.max(0, offset - PAGINA);
        } else if (escolha.equals("0")) {
            break;
        } else {
            System.out.println("Opção inválida.");
        }
    }

    System.out.println("\n" + ocorrencias.size() + " ocorrências processadas, "
            + resultado.descartados + " registros ignorados por dados inválidos.\n");
}

private static void exibirOcorrencias(List<Occurrence> ocorrencias, int offset) {
    int fim = Math.min(offset + PAGINA, ocorrencias.size());
    System.out.println("\n--- Ocorrências (" + (offset + 1) + " a " + fim + " de " + ocorrencias.size() + ") ---");
    for (int i = offset; i < fim; i++) {
        Occurrence o = ocorrencias.get(i);
        System.out.printf("%2d. %s @ (%.4f, %.4f)%n", i + 1, o.getDate(), o.getLatitude(), o.getLongitude());
    }
}

private static void consultaGeral(Scanner scanner, GBIFApiClient client) {
    Species especie = resolverEspecie(scanner, client);

    if (especie == null) {
        return;
    }

    System.out.println("\n--- Resultado ---");
    System.out.println("ID (taxonKey): " + especie.getId());
    System.out.println("Nome científico: " + especie.getScientificName());
    System.out.println("Categoria: " + especie.getClass().getSimpleName());
    System.out.println("Detalhes: " + especie.describeHabitat());

    System.out.print("\nDeseja ver as ocorrências desta espécie? (S/N): ");
    String verOcorrencias = scanner.nextLine().trim();

    if (verOcorrencias.equalsIgnoreCase("S")) {
        listarOcorrencias(especie, client, scanner);
    } else {
        System.out.println();
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