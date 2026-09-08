package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF6 [P] — Relatórios agnósticos a tipo.
 *
 * Agrupa uma coleção mista de Species (aves, mamíferos, plantas, etc.) por
 * ConservationStatus em uma única passada, sem nenhuma lógica específica de
 * subtipo (nenhum "instanceof Bird", nenhum switch por categoria). O
 * agrupamento usa apenas Species.getStatus(), que é herdado por todas as
 * subclasses hoje existentes — e por qualquer categoria taxonômica que vier
 * a ser adicionada no futuro, sem precisar tocar nesta classe.
 */
public class ConservationReport {

    // Impede instanciação — a classe só expõe métodos estáticos utilitários.
    private ConservationReport() {
    }

    /**
     * Agrupa as espécies por status de conservação. Todas as categorias do
     * enum aparecem como chave (mesmo sem nenhuma espécie), na ordem
     * declarada em ConservationStatus (do mais crítico ao menos crítico), e
     * cada grupo preserva a ordem de chegada das espécies na lista original.
     */
    public static Map<ConservationStatus, List<Species>> agruparPorStatus(List<Species> especies) {
        Map<ConservationStatus, List<Species>> grupos = new LinkedHashMap<>();

        for (ConservationStatus status : ConservationStatus.values()) {
            grupos.put(status, new ArrayList<>());
        }

        for (Species especie : especies) {
            grupos.get(especie.getStatus()).add(especie);
        }

        return grupos;
    }

    /**
     * Monta a versão textual do relatório, pronta para impressão. Só inclui
     * categorias de status que tiverem pelo menos uma espécie associada.
     */
    public static String formatar(List<Species> especies) {
        Map<ConservationStatus, List<Species>> grupos = agruparPorStatus(especies);

        StringBuilder sb = new StringBuilder();
        sb.append("--- Relatório de Conservação (").append(especies.size()).append(" espécies) ---\n");

        for (Map.Entry<ConservationStatus, List<Species>> entrada : grupos.entrySet()) {
            List<Species> grupo = entrada.getValue();
            if (grupo.isEmpty()) {
                continue;
            }

            sb.append("\n").append(entrada.getKey()).append(" (").append(grupo.size()).append("):\n");
            for (Species especie : grupo) {
                sb.append("  - ").append(especie.getScientificName())
                        .append(" [").append(especie.getClass().getSimpleName()).append("]\n");
            }
        }

        return sb.toString();
    }
}