package br.com.fiap.fordretention.dto.concessionaria;

import br.com.fiap.fordretention.model.enums.TipoServico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service Share da base de clientes de uma concessionária.
 * serviceShare = clientes com ≥1 serviço pago no período / clientes da base com veículo (%).
 * Os recortes por idade e modelo são calculados por veículo (VIN Share).
 */
public record ServiceShareResponse(
        Long concessionariaId,
        String concessionariaNome,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        long totalClientes,
        long clientesComServicoPago,
        BigDecimal serviceShare,
        long totalVeiculos,
        long veiculosComServicoPago,
        BigDecimal vinShare,
        List<Segmento> porIdadeVeiculo,
        List<Segmento> porModelo,
        List<ResumoTipoServico> porTipoServico
) {
    public record Segmento(String segmento, long total, long comServicoPago, BigDecimal share) {
    }

    public record ResumoTipoServico(TipoServico tipo, long quantidade, BigDecimal receita) {
    }
}
