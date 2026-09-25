package br.com.fiap.fordretention.service;

import br.com.fiap.fordretention.dto.concessionaria.ServiceShareResponse;
import br.com.fiap.fordretention.dto.concessionaria.ServiceShareResponse.ResumoTipoServico;
import br.com.fiap.fordretention.dto.concessionaria.ServiceShareResponse.Segmento;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service Share = % dos clientes da base da concessionária (clientes com veículo cuja concessionária
 * preferida é ela) que fizeram ao menos um serviço PAGO em qualquer concessionária Ford no período.
 * Os recortes por idade do veículo e por modelo são calculados por VIN (VIN Share).
 */
@Service
public class ServiceShareService {

    static final String FAIXA_NOVOS = "0-3 anos";
    static final String FAIXA_INTERMEDIARIOS = "4-6 anos";
    static final String FAIXA_ANTIGOS = "7+ anos";

    private final ConcessionariaService concessionariaService;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final Clock clock;

    public ServiceShareService(ConcessionariaService concessionariaService, VeiculoRepository veiculoRepository,
                               ServicoRepository servicoRepository, Clock clock) {
        this.concessionariaService = concessionariaService;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ServiceShareResponse calcular(Long concessionariaId, int meses) {
        Concessionaria concessionaria = concessionariaService.buscarNoEscopo(concessionariaId);
        LocalDate fim = LocalDate.now(clock);
        LocalDate inicio = fim.minusMonths(meses).plusDays(1);
        int anoAtual = fim.getYear();

        List<Veiculo> parque = veiculoRepository.findByCliente_ConcessionariaPreferida_Id(concessionariaId);
        Set<Long> veiculosAtivos = servicoRepository.findVeiculosComServicoPago(concessionariaId, inicio, fim);

        // Cliente "ativo" = pelo menos um de seus veículos teve serviço pago no período
        Map<Long, Boolean> clienteAtivo = parque.stream().collect(Collectors.toMap(
                v -> v.getCliente().getId(),
                v -> veiculosAtivos.contains(v.getId()),
                Boolean::logicalOr));
        long totalClientes = clienteAtivo.size();
        long clientesAtivos = clienteAtivo.values().stream().filter(Boolean::booleanValue).count();
        long veiculosComServico = parque.stream().filter(v -> veiculosAtivos.contains(v.getId())).count();

        List<Segmento> porIdade = segmentar(parque, veiculosAtivos, v -> faixaIdade(v.idade(anoAtual)),
                List.of(FAIXA_NOVOS, FAIXA_INTERMEDIARIOS, FAIXA_ANTIGOS));
        List<Segmento> porModelo = segmentar(parque, veiculosAtivos, Veiculo::getModelo, List.of()).stream()
                .sorted(Comparator.comparingLong(Segmento::total).reversed().thenComparing(Segmento::segmento))
                .toList();
        List<ResumoTipoServico> porTipo = servicoRepository
                .resumirServicosPagosPorTipo(concessionariaId, inicio, fim).stream()
                .map(r -> new ResumoTipoServico(r.tipo(), r.quantidade(), r.receita().setScale(2, RoundingMode.HALF_UP)))
                .toList();

        return new ServiceShareResponse(
                concessionaria.getId(),
                concessionaria.getNome(),
                inicio,
                fim,
                totalClientes,
                clientesAtivos,
                percentual(clientesAtivos, totalClientes),
                parque.size(),
                veiculosComServico,
                percentual(veiculosComServico, parque.size()),
                porIdade,
                porModelo,
                porTipo);
    }

    static String faixaIdade(int idade) {
        if (idade <= 3) {
            return FAIXA_NOVOS;
        }
        return idade <= 6 ? FAIXA_INTERMEDIARIOS : FAIXA_ANTIGOS;
    }

    static BigDecimal percentual(long parte, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(parte * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /**
     * Agrupa o parque por uma chave. Chaves listadas em {@code segmentosFixos} aparecem sempre (mesmo com zero),
     * na ordem informada — útil para as faixas de idade do dashboard.
     */
    private static List<Segmento> segmentar(List<Veiculo> parque, Set<Long> ativos,
                                            Function<Veiculo, String> chave, List<String> segmentosFixos) {
        Map<String, long[]> contagem = new LinkedHashMap<>();
        segmentosFixos.forEach(s -> contagem.put(s, new long[2]));
        for (Veiculo v : parque) {
            long[] c = contagem.computeIfAbsent(chave.apply(v), k -> new long[2]);
            c[0]++;
            if (ativos.contains(v.getId())) {
                c[1]++;
            }
        }
        return contagem.entrySet().stream()
                .map(e -> new Segmento(e.getKey(), e.getValue()[0], e.getValue()[1],
                        percentual(e.getValue()[1], e.getValue()[0])))
                .toList();
    }
}
