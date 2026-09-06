import api.GBIFApiClient;
import model.Occurrence;
import model.SearchResult;
import model.Species;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final int PAGINA = 20;

    // Quantas espécies (ex.: subespécies) casando com o termo buscado são
    // consideradas ao listar ocorrências. Não há navegação/paginação aqui
    // porque o usuário não escolhe uma espécie específica: todas as
    // correspondências entram no cálculo.
    private static final int LIMITE_ESPECIES_PARA_OCORRENCIAS = 30;

    // Quantas ocorrências brutas são buscadas por espécie individual antes de
    // serem validadas/filtradas.
    private static final int LIMITE_OCORRENCIAS_POR_ESPECIE = 200;

    public static void main(String[] args) {
        configurarConsoleUtf8();

        // Força saída e leitura padrão em UTF-8, independente da code page
        // nativa do console (no Windows, o cmd.exe costuma usar uma code page
        // antiga que não representa acentos/caracteres especiais, fazendo o
        // Java imprimir "?" mesmo quando o dado em memória está correto).
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
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
                    listarOcorrenciasPorNome(scanner, client);
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

    // No Windows, o console (cmd.exe/PowerShell) costuma abrir usando uma code
    // page antiga (ex.: 850), que não representa acentos e caracteres especiais
    // corretamente, mesmo que o Java esteja emitindo os bytes certos em UTF-8.
    // Para o usuário não precisar configurar nada manualmente antes de usar o
    // programa, ajustamos a codificação do console automaticamente aqui, via
    // um subprocesso que herda o console atual (por isso a mudança afeta a
    // janela inteira, e não só esse subprocesso). Em sistemas que já usam
    // UTF-8 nativamente (Linux/macOS), isso simplesmente não é executado.
    private static void configurarConsoleUtf8() {
        String sistemaOperacional = System.getProperty("os.name", "").toLowerCase();
        if (!sistemaOperacional.contains("win")) {
            return;
        }

        try {
            // Testado e confirmado: ajustar a codificação de saída do console
            // via PowerShell resolve o problema de forma mais confiável do que
            // o comando "chcp 65001" isolado no ambiente do usuário. Também
            // precisamos ajustar a ENTRADA (InputEncoding) — sem isso, o texto
            // digitado pelo usuário com acentos chega corrompido no Java, mesmo
            // com a saída já correta.
            new ProcessBuilder("powershell", "-NoProfile", "-Command",
                    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
                            + "[Console]::InputEncoding = [System.Text.Encoding]::UTF8")
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (Exception e) {
            // Se o PowerShell não estiver disponível por algum motivo, tenta o
            // chcp como alternativa antes de desistir.
            try {
                new ProcessBuilder("cmd.exe", "/c", "chcp 65001>nul")
                        .inheritIO()
                        .start()
                        .waitFor();
            } catch (Exception ignored) {
                // Se nada funcionar, seguimos mesmo assim — o pior caso é
                // voltar a ter mojibake, mas o programa continua funcionando.
            }
        }
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

    // Busca uma espécie por nome (popular ou científico), com paginação real:
    // [N]/[P] refazem a consulta à API com um novo offset em vez de só
    // re-exibir a mesma página.
    private static Species resolverEspecie(Scanner scanner, GBIFApiClient client) {
        System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
        String termo = scanner.nextLine().trim();

        if (termo.isEmpty()) {
            System.out.println("Termo de busca não pode ser vazio.\n");
            return null;
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
            return null;
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
                return null;
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
            return client.fetchSpecies(selecionado.getKey());
        }
    }

    // Usado após a consulta geral (opção 1), quando o usuário já escolheu uma
    // espécie específica e pede para ver as ocorrências dela.
    private static void listarOcorrencias(Species especie, GBIFApiClient client, Scanner scanner) {
        List<String> brutos = client.fetchRawOccurrencesByScientificName(especie.getScientificName(), LIMITE_OCORRENCIAS_POR_ESPECIE);
        GBIFApiClient.ResultadoLote resultado = client.importarOcorrencias(brutos, especie);

        exibirOcorrenciasPaginadas(resultado.ocorrencias, resultado.descartados,
                "\"" + especie.getScientificName() + "\"", scanner);
    }

    // Opção "2. Listar ocorrências": o usuário digita um nome (comum ou
    // científico), e TODAS as ocorrências encontradas para esse termo são
    // listadas diretamente — não há passo de "escolha um resultado". Como um
    // termo pode casar com mais de uma espécie (ex.: subespécies de
    // Panthera leo), buscamos ocorrências de cada espécie encontrada e
    // juntamos tudo em uma única lista.
    private static void listarOcorrenciasPorNome(Scanner scanner, GBIFApiClient client) {
        System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
        String termo = scanner.nextLine().trim();

        if (termo.isEmpty()) {
            System.out.println("Termo de busca não pode ser vazio.\n");
            return;
        }

        System.out.println("Buscando ocorrências de \"" + termo + "\" na base do GBIF...");

        // Mesma lógica de fallback da consulta geral: tenta nome popular
        // primeiro, cai para nome científico se não achar nada.
        List<SearchResult> correspondencias = client.searchByVernacular(termo, 0, LIMITE_ESPECIES_PARA_OCORRENCIAS);
        if (correspondencias.isEmpty()) {
            correspondencias = client.searchByScientific(termo, 0, LIMITE_ESPECIES_PARA_OCORRENCIAS);
        }

        if (correspondencias.isEmpty()) {
            System.out.println("Nenhuma espécie encontrada para \"" + termo + "\".\n");
            return;
        }

        List<Occurrence> todasOcorrencias = new ArrayList<>();
        int totalDescartados = 0;

        for (SearchResult correspondencia : correspondencias) {
            Species especie = client.fetchSpecies(correspondencia.getKey());
            if (especie == null) {
                continue;
            }

            List<String> brutos = client.fetchRawOccurrencesByScientificName(
                    especie.getScientificName(), LIMITE_OCORRENCIAS_POR_ESPECIE);
            GBIFApiClient.ResultadoLote resultado = client.importarOcorrencias(brutos, especie);

            todasOcorrencias.addAll(resultado.ocorrencias);
            totalDescartados += resultado.descartados;
        }

        exibirOcorrenciasPaginadas(todasOcorrencias, totalDescartados, "\"" + termo + "\"", scanner);
    }

    // Paginação/exibição compartilhada pelos dois fluxos acima.
    private static void exibirOcorrenciasPaginadas(List<Occurrence> ocorrencias, int descartados,
                                                     String rotuloBusca, Scanner scanner) {
        if (ocorrencias.isEmpty()) {
            System.out.println("\nNenhuma ocorrência válida encontrada para " + rotuloBusca + ".");
            System.out.println(descartados + " registros ignorados por dados inválidos.\n");
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
                + descartados + " registros ignorados por dados inválidos.\n");
    }

    private static void exibirOcorrencias(List<Occurrence> ocorrencias, int offset) {
        int fim = Math.min(offset + PAGINA, ocorrencias.size());
        System.out.println("\n--- Ocorrências (" + (offset + 1) + " a " + fim + " de " + ocorrencias.size() + ") ---");
        for (int i = offset; i < fim; i++) {
            Occurrence o = ocorrencias.get(i);
            System.out.printf("%2d. %-25s | %s @ (%.4f, %.4f)%n", i + 1,
                    o.getSpeciesRef().getScientificName(), o.getDate(), o.getLatitude(), o.getLongitude());
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
