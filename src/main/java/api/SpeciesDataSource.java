package api;

import model.Species;

import java.util.List;

/**
 * Contrato para qualquer fonte de dados de espécies (GBIF, EOL, ou uma
 * combinação de várias). O restante do sistema (Main, futuros serviços)
 * depende apenas desta interface, nunca de uma implementação concreta.
 *
 * Nenhum método aqui expõe offset/limit de paginação: buscas de resultados
 * paginados devolvem uma SearchSession, que resolve isso internamente,
 * buscando cada página sob demanda.
 */
public interface SpeciesDataSource {

    /**
     * Busca os detalhes completos de uma espécie a partir do seu id (taxonKey).
     */
    Species fetchSpecies(int SpeciesId);

    /**
     * Inicia uma busca por nome popular. Busca apenas a primeira página
     * (tamanho pageSize) imediatamente; páginas seguintes só são buscadas na
     * API quando SearchSession.nextPage() for chamado.
     */
    SearchSession searchByVernacular(String termo, int pageSize);

    /**
     * Mesma ideia de searchByVernacular, mas por nome científico / texto livre.
     */
    SearchSession searchByScientific(String termo, int pageSize);

    /**
     * Retorna ocorrências brutas (JSON) de uma espécie pelo nome científico,
     * até o limite informado.
     */
    List<String> fetchRawOccurrencesByScientificName(String ScientificName, int limit);

    /**
     * Converte ocorrências brutas em objetos de domínio, descartando
     * registros inválidos.
     */
    ImportResult importarOcorrencias(List<String> jsonsBrutos, Species especie);
}
