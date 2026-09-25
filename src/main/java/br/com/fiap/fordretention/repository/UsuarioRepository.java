package br.com.fiap.fordretention.repository;

import br.com.fiap.fordretention.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "concessionaria")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByConcessionaria_Id(Long concessionariaId);

    @EntityGraph(attributePaths = "concessionaria")
    Page<Usuario> findByConcessionaria_Id(Long concessionariaId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "concessionaria")
    Page<Usuario> findAll(Pageable pageable);
}
