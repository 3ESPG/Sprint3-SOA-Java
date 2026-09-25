package br.com.fiap.fordretention.repository.spec;

import br.com.fiap.fordretention.dto.cliente.ClienteFiltro;
import br.com.fiap.fordretention.model.Cliente;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ClienteSpecifications {

    private ClienteSpecifications() {
    }

    /** @param concessionariaId concessionária efetiva (já resolvida pelo escopo do usuário), pode ser nula. */
    public static Specification<Cliente> filtrar(ClienteFiltro filtro, Long concessionariaId) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (concessionariaId != null) {
                predicados.add(cb.equal(root.get("concessionariaPreferida").get("id"), concessionariaId));
            }
            if (filtro != null) {
                if (SpecUtils.temTexto(filtro.nome())) {
                    predicados.add(cb.like(cb.lower(root.get("nome")), SpecUtils.contem(filtro.nome()), '\\'));
                }
                if (filtro.perfil() != null) {
                    predicados.add(cb.equal(root.get("perfil"), filtro.perfil()));
                }
                if (filtro.scoreMin() != null) {
                    predicados.add(cb.greaterThanOrEqualTo(root.get("scoreRisco"), filtro.scoreMin()));
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
