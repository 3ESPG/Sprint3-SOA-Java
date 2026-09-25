package br.com.fiap.fordretention.repository.spec;

import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaFiltro;
import br.com.fiap.fordretention.model.Concessionaria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ConcessionariaSpecifications {

    private ConcessionariaSpecifications() {
    }

    /** @param idRestrito se não nulo, limita o resultado a essa concessionária (escopo do token). */
    public static Specification<Concessionaria> filtrar(ConcessionariaFiltro filtro, Long idRestrito) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (idRestrito != null) {
                predicados.add(cb.equal(root.get("id"), idRestrito));
            }
            if (filtro != null) {
                if (SpecUtils.temTexto(filtro.nome())) {
                    predicados.add(cb.like(cb.lower(root.get("nome")), SpecUtils.contem(filtro.nome()), '\\'));
                }
                if (SpecUtils.temTexto(filtro.cidade())) {
                    predicados.add(cb.like(cb.lower(root.get("cidade")), SpecUtils.contem(filtro.cidade()), '\\'));
                }
                if (SpecUtils.temTexto(filtro.estado())) {
                    predicados.add(cb.equal(root.get("estado"), filtro.estado().trim().toUpperCase()));
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
