package api;
 
import model.SearchResult;
 
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Implementação de SearchSession usada pelo GBIFApiClient. Guarda em cache
 * cada página já buscada, então voltar páginas (previousPage) nunca gera
 * uma nova chamada à API — só avançar para uma página inédita (nextPage)
 * gera.
 */
class GBIFSearchSession implements SearchSession {

    private final IntFunction<List<SearchResult>> pageFetcher;
    private final int pageSize;
    private final List<List<SearchResult>> cachedPages = new ArrayList<>();

    private int currentIndex = -1;
    private boolean maybeHasMorePages = true;

    /**
     * @param pageFetcher função que, dado um offset, busca aquela página na
     *                    API (implementada pelo GBIFApiClient, que é o único
     *                    lugar que sabe como montar essa chamada)
     * @param pageSize    quantos resultados por página; decidido por quem
     *                    chama (ex.: Main, de acordo com o que faz sentido
     *                    mostrar na tela ou processar de uma vez)
     */
    GBIFSearchSession(IntFunction<List<SearchResult>> pageFetcher, int pageSize) {
        this.pageFetcher = pageFetcher;
        this.pageSize = pageSize;
        if (fetchNextPageIfPossible()) {
            currentIndex = 0;
        }
    }

    @Override
    public List<SearchResult> currentPage() {
        if(currentIndex < 0 || currentIndex >= cachedPages.size()) {
            return List.of();
        }
        return cachedPages.get(currentIndex);
    }

    @Override
    public int pageIndex() {
        return Math.max(currentIndex, 0);
    }

    @Override
    public boolean hasResults() {
        return !cachedPages.isEmpty();
    }

    @Override
    public boolean nextPage() {
        if (currentIndex + 1 < cachedPages.size()) {
            currentIndex++;
            return true;
        }
        if (!maybeHasMorePages) {
            return false;
        }
        if (fetchNextPageIfPossible()) {
            currentIndex++;
            return true;
        }
        return false;
    }

    @Override
    public boolean previousPage() {
        if (currentIndex <= 0) {
            return false;
        }
        currentIndex--;
        return true;
    }

    // Busca a próxima página ainda não cacheada, se ainda não soubermos que
    // os resultados acabaram. Retorna true se uma página com conteúdo foi
    // adicionada ao cache.
    private boolean fetchNextPageIfPossible() {
        int offset = cachedPages.size() * pageSize;
        List<SearchResult> pagina = pageFetcher.apply(offset);

        if(pagina.isEmpty()) {
            maybeHasMorePages = false;
            return false;
        }

        cachedPages.add(pagina);
        maybeHasMorePages = pagina.size() == pageSize;
        return true;
    }
}