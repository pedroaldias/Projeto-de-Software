import api.GBIFApiClient;
import api.EnrichedSpeciesDataSource;
import api.WikiDataClient; 
import api.ImportResult;
import api.SearchSession;
import api.SpeciesDataSource;
import model.ConservationReport;
import model.ConservationStatus;
import model.Occurrence;
import model.SearchResult;
import model.Species;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final int PAGINA = 20;

    // por que
    // Tamanho de página usado quando precisamos buscar TODAS as correspondências
    // de uma vez (fluxo de ocorrências, que agrega dados de todas as espécies
    // encontradas). Maior que PAGINA porque, nesse caso, sabemos de antemão que
    // vamos consumir a lista inteira — não faz sentido usar páginas pequenas
    // pensadas para exibição em tela.
    private static final int PAGINA_LOTE = 50;

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
        // RF7 [IF]: Main só conhece a interface, nunca a implementação concreta.
        SpeciesDataSource client = new EnrichedSpeciesDataSource(new GBIFApiClient(), new WikiDataClient());
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
                case "3":
                    relatorioConservacaoPorNome(scanner, client);
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
        System.out.println("3. Gerar relatório de conservação");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    // Busca uma espécie por nome (popular ou científico).
    //
    // RF7 [A] / Corrigir: Main não gerencia mais offset/limit da API — quem
    // faz isso é a SearchSession. [N] só busca uma página nova na API se o
    // usuário realmente pedir para avançar além do que já foi visto; [P]
    // nunca busca nada novo, só volta no cache da própria sessão.
    private static Species resolverEspecie(Scanner scanner, SpeciesDataSource client) {
        System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
        String termo = scanner.nextLine().trim();

        if (termo.isEmpty()) {
            System.out.println("Termo de busca não pode ser vazio.\n");
            return null;
        }

        System.out.println("Buscando \"" + termo + "\" na base do GBIF...");

        // 1) Tenta primeiro por nome popular (vernacular). Se não achar nada,
        //    cai para a busca geral (nome científico ou texto livre).
        SearchSession session = client.searchByVernacular(termo, PAGINA);
        if(!session.hasResults()) {
            session = client.searchByScientific(termo, PAGINA);
        }

        if(!session.hasResults()) {
            System.out.println("Nenhum resultado encontrado para \"" + termo + "\".\n");
            return null;
        }

        while (true) {
            List<SearchResult> pagina = session.currentPage();
            exibirResultados(pagina, session.pageIndex() * PAGINA);
            System.out.print("\nDigite o número do resultado, [N] próxima página, "
                    + "[P] página anterior ou [0] cancelar: ");
            String escolha = scanner.nextLine().trim();

            if (escolha.equalsIgnoreCase("N")) {
                if (!session.nextPage()) {
                    System.out.println("Não há mais resultados.\n");
                }
                continue;
            }

            if (escolha.equalsIgnoreCase("P")) {
                if (!session.previousPage()) {
                    System.out.println("Você já está na primeira página.\n");
                }
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

    // Variante de resolverEspecie que permite selecionar MAIS DE UM resultado
    // por vez, digitando os números separados por vírgula (ex.: "1,3,5").
    // Usada apenas pelo relatório de conservação — a consulta geral (opção 1)
    // continua usando resolverEspecie, que seleciona uma única espécie.
    //
    // Guarda também o nome popular de cada seleção (vindo do próprio
    // SearchResult da busca), já que Species não carrega essa informação —
    // isso é devolvido junto no par (Species, nome popular).
    private static List<EspecieSelecionada> resolverEspecies(Scanner scanner, SpeciesDataSource client) {
        List<EspecieSelecionada> selecionadas = new ArrayList<>();

        System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
        String termo = scanner.nextLine().trim();

        if (termo.isEmpty()) {
            System.out.println("Termo de busca não pode ser vazio.\n");
            return selecionadas;
        }

        System.out.println("Buscando \"" + termo + "\" na base do GBIF...");

        SearchSession session = client.searchByVernacular(termo, PAGINA);
        if (!session.hasResults()) {
            session = client.searchByScientific(termo, PAGINA);
        }

        if (!session.hasResults()) {
            System.out.println("Nenhum resultado encontrado para \"" + termo + "\".\n");
            return selecionadas;
        }

        while (true) {
            List<SearchResult> pagina = session.currentPage();
            exibirResultados(pagina, session.pageIndex() * PAGINA);
            System.out.print("\nDigite o(s) número(s) do(s) resultado(s) separados por vírgula (ex.: 1,3,5), "
                    + "[N] próxima página, [P] página anterior ou [0] concluir seleção: ");
            String escolha = scanner.nextLine().trim();

            if (escolha.equalsIgnoreCase("N")) {
                if (!session.nextPage()) {
                    System.out.println("Não há mais resultados.\n");
                }
                continue;
            }

            if (escolha.equalsIgnoreCase("P")) {
                if (!session.previousPage()) {
                    System.out.println("Você já está na primeira página.\n");
                }
                continue;
            }

            if (escolha.equals("0")) {
                return selecionadas;
            }

            boolean algumaValida = false;
            for (String parte : escolha.split(",")) {
                String limpo = parte.trim();
                if (limpo.isEmpty()) {
                    continue;
                }

                int indice;
                try {
                    indice = Integer.parseInt(limpo);
                } catch (NumberFormatException e) {
                    System.out.println("Ignorado: \"" + limpo + "\" não é um número válido.");
                    continue;
                }

                if (indice < 1 || indice > pagina.size()) {
                    System.out.println("Ignorado: número " + indice + " fora do intervalo desta página.");
                    continue;
                }

                SearchResult selecionado = pagina.get(indice - 1);
                Species especie = client.fetchSpecies(selecionado.getKey());
                if (especie == null) {
                    System.out.println("Não foi possível carregar os detalhes de \""
                            + selecionado.getScientificName() + "\".");
                    continue;
                }

                String nomePopular = selecionado.getVernacularNames().isEmpty()
                        ? null
                        : selecionado.getVernacularNames().get(0);

                selecionadas.add(new EspecieSelecionada(especie, nomePopular));
                algumaValida = true;
                System.out.println("\"" + especie.getScientificName() + "\" adicionada.");
            }

            if (algumaValida) {
                return selecionadas;
            }
            System.out.println("Nenhuma seleção válida. Tente novamente.\n");
        }
    }

    // Par simples (espécie + nome popular capturado no momento da busca).
    // Existe só para o relatório de conservação não perder o nome popular ao
    // converter SearchResult -> Species (Species não guarda essa informação).
    private static class EspecieSelecionada {
        final Species especie;
        final String nomePopular;

        EspecieSelecionada(Species especie, String nomePopular) {
            this.especie = especie;
            this.nomePopular = nomePopular;
        }
    }

    // Usado quando precisamos de TODAS as correspondências de uma busca, não
    // só da página que está sendo exibida (ex.: fluxo de ocorrências, que
    // agrega dados de todas as espécies encontradas). Percorre a sessão até
    // o fim, reaproveitando a mesma lógica de paginação sob demanda.
    private static List<SearchResult> collectAllPages(SearchSession session) {
        List<SearchResult> todos = new ArrayList<>();

        if(!session.hasResults()) {
            return todos;
        }

        todos.addAll(session.currentPage());
        while (session.nextPage()) {
            todos.addAll(session.currentPage());
        }

        return todos;
    }

    // Usado após a consulta geral (opção 1), quando o usuário já escolheu uma
    // espécie específica e pede para ver as ocorrências dela.
    private static void listarOcorrencias(Species especie, SpeciesDataSource client, Scanner scanner) {
        List<String> brutos = client.fetchRawOccurrencesByScientificName(especie.getScientificName(), LIMITE_OCORRENCIAS_POR_ESPECIE);
        ImportResult resultado = client.importarOcorrencias(brutos, especie);

        exibirOcorrenciasPaginadas(resultado.ocorrencias, resultado.descartados, "\"" + especie.getScientificName() + "\"", scanner);
    }

    // Opção "2. Listar ocorrências": o usuário digita um nome (comum ou
    // científico), e TODAS as ocorrências encontradas para esse termo são
    // listadas diretamente — não há passo de "escolha um resultado". Como um
    // termo pode casar com mais de uma espécie (ex.: subespécies de
    // Panthera leo), buscamos ocorrências de cada espécie encontrada e
    // juntamos tudo em uma única lista.
    private static void listarOcorrenciasPorNome(Scanner scanner, SpeciesDataSource client) {
        System.out.print("\nDigite o nome (comum ou científico) da espécie: ");
        String termo = scanner.nextLine().trim();

        if (termo.isEmpty()) {
            System.out.println("Termo de busca não pode ser vazio.\n");
            return;
        }

        System.out.println("Buscando ocorrências de \"" + termo + "\" na base do GBIF...");

        // Mesma lógica de fallback da consulta geral: tenta nome popular
        // primeiro, cai para nome científico se não achar nada. Aqui
        // precisamos de TODAS as correspondências (para agregar ocorrências
        // de cada uma), então percorremos a sessão até o fim.
        List<SearchResult> correspondencias = collectAllPages(client.searchByVernacular(termo, PAGINA_LOTE));
        if (correspondencias.isEmpty()) {
            correspondencias = collectAllPages(client.searchByScientific(termo, PAGINA_LOTE));
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
            ImportResult resultado = client.importarOcorrencias(brutos, especie);

            todasOcorrencias.addAll(resultado.ocorrencias);
            totalDescartados += resultado.descartados;
        }

        exibirOcorrenciasPaginadas(todasOcorrencias, totalDescartados, "\"" + termo + "\"", scanner);
    }

    // Opção "3. Relatório de conservação por status": o usuário adiciona
    // espécies uma a uma (reaproveitando resolverEspecie, a mesma busca com
    // fallback popular->científico da consulta geral) até dizer que não quer
    // mais adicionar. Só então o relatório é montado, agrupando por
    // ConservationStatus via ConservationReport (RF6 [P] — nenhuma lógica
    // específica de subtipo aqui ou lá).
    private static void relatorioConservacaoPorNome(Scanner scanner, SpeciesDataSource client) {
        List<Species> especies = new ArrayList<>();
        Map<Species, String> nomesPopulares = new IdentityHashMap<>();

        System.out.println("\n--- Gerar relatório de conservação ---");
        System.out.println("Adicione as espécies que deseja incluir no relatório.");

        while (true) {
            List<EspecieSelecionada> novas = resolverEspecies(scanner, client);
            for (EspecieSelecionada selecionada : novas) {
                especies.add(selecionada.especie);
                nomesPopulares.put(selecionada.especie, selecionada.nomePopular);
            }

            if (!novas.isEmpty()) {
                System.out.println("\n" + especies.size() + " espécie(s) no relatório até agora.");
            }

            System.out.print("\nDeseja adicionar mais espécies ao relatório? (S/N): ");
            String continuar = scanner.nextLine().trim();
            if (!continuar.equalsIgnoreCase("S")) {
                break;
            }
        }

        if (especies.isEmpty()) {
            System.out.println("\nNenhuma espécie adicionada. Relatório cancelado.\n");
            return;
        }

        Map<ConservationStatus, List<Species>> grupos = ConservationReport.agruparPorStatus(especies);
        exibirCategoriasDeRisco(grupos, nomesPopulares, scanner);
    }

    // Menu de navegação por categoria de risco: mostra só as classificações
    // que de fato têm espécie associada (com a contagem de cada uma, e o
    // nome traduzido para português via ConservationStatus.emPortugues()), e
    // deixa o usuário escolher qual quer detalhar, até apertar [0] para
    // voltar.
    private static void exibirCategoriasDeRisco(Map<ConservationStatus, List<Species>> grupos,
                                                  Map<Species, String> nomesPopulares, Scanner scanner) {
        while (true) {
            List<ConservationStatus> statusComEspecies = new ArrayList<>();
            for (Map.Entry<ConservationStatus, List<Species>> entrada : grupos.entrySet()) {
                if (!entrada.getValue().isEmpty()) {
                    statusComEspecies.add(entrada.getKey());
                }
            }

            System.out.println("\n--- Classificações de risco no relatório ---");
            for (int i = 0; i < statusComEspecies.size(); i++) {
                ConservationStatus status = statusComEspecies.get(i);
                System.out.printf("%2d. %-25s (%d espécies)%n",
                        i + 1, status.emPortugues(), grupos.get(status).size());
            }
            System.out.print("\nDigite o número da classificação para detalhar ou [0] voltar: ");
            String escolha = scanner.nextLine().trim();

            if (escolha.equals("0")) {
                System.out.println();
                return;
            }

            int indice;
            try {
                indice = Integer.parseInt(escolha);
            } catch (NumberFormatException e) {
                System.out.println("Opção inválida.\n");
                continue;
            }

            if (indice < 1 || indice > statusComEspecies.size()) {
                System.out.println("Opção inválida.\n");
                continue;
            }

            ConservationStatus escolhido = statusComEspecies.get(indice - 1);
            List<Species> grupo = grupos.get(escolhido);

            System.out.println("\n--- " + escolhido.emPortugues() + " (" + grupo.size() + " espécies) ---");
            for (Species especie : grupo) {
                String nomePopular = nomesPopulares.get(especie);
                String sufixoNomePopular = (nomePopular == null || nomePopular.isEmpty())
                        ? ""
                        : " (" + nomePopular + ")";

                System.out.println("  - " + especie.getScientificName() + sufixoNomePopular
                        + " [" + especie.getClass().getSimpleName() + "]");
            }
            System.out.println();
        }
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

    private static void consultaGeral(Scanner scanner, SpeciesDataSource client) {
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