package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.Veiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long>, JpaSpecificationExecutor<Veiculo> {

    boolean existsByVin(String vin);

    boolean existsByVinAndIdNot(String vin, Long id);

    boolean existsByClienteId(Long clienteId);

    List<Veiculo> findByClienteId(Long clienteId);

    /** Parque de veículos da base de clientes de uma concessionária (denominador do Service Share). */
    @EntityGraph(attributePaths = "cliente")
    List<Veiculo> findByCliente_ConcessionariaPreferida_Id(Long concessionariaId);

    @Override
    @EntityGraph(attributePaths = {"cliente", "cliente.concessionariaPreferida"})
    Page<Veiculo> findAll(Specification<Veiculo> spec, Pageable pageable);
}
