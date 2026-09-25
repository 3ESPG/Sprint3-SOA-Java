package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.Concessionaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConcessionariaRepository extends JpaRepository<Concessionaria, Long>,
        JpaSpecificationExecutor<Concessionaria> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, Long id);
}
