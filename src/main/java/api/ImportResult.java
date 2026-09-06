package api;
 
import model.Occurrence;
 
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado da importação de ocorrências brutas: os registros válidos já
 * convertidos, e a contagem de quantos foram descartados por dados
 * inválidos. Antes era uma classe aninhada em GBIFApiClient
 * (ResultadoLote); foi movida para cá porque agora faz parte do contrato
 * de SpeciesDataSource, não de uma implementação específica.
 */
public class ImportResult {
    public final List<Occurrence> ocorrencias = new ArrayList<>();
    public int descartados = 0;
}
