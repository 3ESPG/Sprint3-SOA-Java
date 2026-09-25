package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.Servico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ServicoRepository extends JpaRepository<Servico, Long>, JpaSpecificationExecutor<Servico> {

    boolean existsByVeiculoId(Long veiculoId);

    boolean existsByConcessionariaId(Long concessionariaId);

    /**
     * Veículos da base de uma concessionária que tiveram pelo menos um serviço PAGO em qualquer
     * concessionária da rede Ford dentro do período (numerador do Service Share).
     */
    @Query("""
            select distinct s.veiculo.id from Servico s
            where s.veiculo.cliente.concessionariaPreferida.id = :concessionariaId
              and s.pago = true
              and s.data between :inicio and :fim
            """)
    Set<Long> findVeiculosComServicoPago(@Param("concessionariaId") Long concessionariaId,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim);

    /** Quantidade e receita de serviços pagos por tipo, para a base de uma concessionária. */
    @Query("""
            select new br.com.fiap.fordretention.repository.ResumoServicoPorTipo(s.tipo, count(s), sum(s.valor))
            from Servico s
            where s.veiculo.cliente.concessionariaPreferida.id = :concessionariaId
              and s.pago = true
              and s.data between :inicio and :fim
            group by s.tipo
            order by count(s) desc
            """)
    List<ResumoServicoPorTipo> resumirServicosPagosPorTipo(@Param("concessionariaId") Long concessionariaId,
                                                    @Param("inicio") LocalDate inicio,
                                                    @Param("fim") LocalDate fim);

    @Override
    @EntityGraph(attributePaths = {"veiculo", "concessionaria"})
    Page<Servico> findAll(Specification<Servico> spec, Pageable pageable);
}
