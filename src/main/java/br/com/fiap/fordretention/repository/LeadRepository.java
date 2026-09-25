package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.enums.StatusLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    boolean existsByVeiculoIdAndStatusIn(Long veiculoId, Collection<StatusLead> status);

    boolean existsByVeiculoIdAndStatusInAndIdNot(Long veiculoId, Collection<StatusLead> status, Long id);

    List<Lead> findByVeiculoIdAndStatusIn(Long veiculoId, Collection<StatusLead> status);

    boolean existsByVeiculoId(Long veiculoId);

    boolean existsByClienteId(Long clienteId);

    boolean existsByConcessionariaId(Long concessionariaId);

    @Override
    @EntityGraph(attributePaths = {"cliente", "veiculo", "concessionaria"})
    Page<Lead> findAll(Specification<Lead> spec, Pageable pageable);
}
