package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByConcessionariaPreferida_Id(Long concessionariaId);

    @Override
    @EntityGraph(attributePaths = "concessionariaPreferida")
    Page<Cliente> findAll(Specification<Cliente> spec, Pageable pageable);
}
