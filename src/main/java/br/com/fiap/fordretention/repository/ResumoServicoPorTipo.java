package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.enums.TipoServico;

import java.math.BigDecimal;

/** Projeção da consulta agregada de serviços pagos por tipo. */
public record ResumoServicoPorTipo(TipoServico tipo, Long quantidade, BigDecimal receita) {
}
