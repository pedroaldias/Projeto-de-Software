package api;

import model.SearchResult;

import java.util.List;

/**
 * Representa uma busca paginada em andamento. Quem usa esta interface nunca
 * vê offset/limit: só pede a página atual e pede para avançar/voltar.
 *
 * As páginas são buscadas sob demanda (lazy): nextPage() só chama a API se
 * aquela página ainda não tiver sido buscada antes; previousPage() nunca
 * chama a API, pois as páginas já visitadas ficam em cache dentro da sessão.
 */
public interface SearchSession {
    
    /** Resultados da página atualmente selecionada. */
    List<SearchResult> currentPage();

    /** Índice (0-based) da página atualmente selecionada. */
    int pageIndex();

    /** True se a busca encontrou pelo menos um resultado. */
    boolean hasResults();

    /**
     * Avança para a próxima página, buscando na API somente se essa página
     * ainda não tiver sido buscada. Retorna false (sem mudar de página) se
     * já estamos na última página existente.
     */
    boolean nextPage();

    /**
     * Volta para a página anterior, já em cache — nunca chama a API.
     * Retorna false (sem mudar de página) se já estamos na primeira página.
     */
    boolean previousPage();
}
