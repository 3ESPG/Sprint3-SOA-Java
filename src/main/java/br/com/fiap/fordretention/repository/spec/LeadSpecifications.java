package br.com.fiap.fordretention.repository.spec;

import br.com.fiap.fordretention.dto.lead.LeadFiltro;
import br.com.fiap.fordretention.model.Lead;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class LeadSpecifications {

    private LeadSpecifications() {
    }

    /** @param concessionariaId concessionária efetiva (já resolvida pelo escopo do usuário), pode ser nula. */
    public static Specification<Lead> filtrar(LeadFiltro filtro, Long concessionariaId) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (concessionariaId != null) {
                predicados.add(cb.equal(root.get("concessionaria").get("id"), concessionariaId));
            }
            if (filtro != null) {
                if (filtro.status() != null) {
                    predicados.add(cb.equal(root.get("status"), filtro.status()));
                }
                if (filtro.perfil() != null) {
                    predicados.add(cb.equal(root.get("cliente").get("perfil"), filtro.perfil()));
                }
                if (filtro.prioridade() != null) {
                    predicados.add(cb.equal(root.get("prioridade"), filtro.prioridade()));
                }
                if (filtro.origem() != null) {
                    predicados.add(cb.equal(root.get("origem"), filtro.origem()));
                }
                if (filtro.clienteId() != null) {
                    predicados.add(cb.equal(root.get("cliente").get("id"), filtro.clienteId()));
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
