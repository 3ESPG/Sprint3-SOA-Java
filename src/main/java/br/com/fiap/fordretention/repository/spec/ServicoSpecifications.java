package br.com.fiap.fordretention.repository.spec;

import br.com.fiap.fordretention.dto.servico.ServicoFiltro;
import br.com.fiap.fordretention.model.Servico;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ServicoSpecifications {

    private ServicoSpecifications() {
    }

    /** @param concessionariaId concessionária executora efetiva (já resolvida pelo escopo), pode ser nula. */
    public static Specification<Servico> filtrar(ServicoFiltro filtro, Long concessionariaId) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (concessionariaId != null) {
                predicados.add(cb.equal(root.get("concessionaria").get("id"), concessionariaId));
            }
            if (filtro != null) {
                if (filtro.tipo() != null) {
                    predicados.add(cb.equal(root.get("tipo"), filtro.tipo()));
                }
                if (filtro.pago() != null) {
                    predicados.add(cb.equal(root.get("pago"), filtro.pago()));
                }
                if (filtro.dataInicio() != null) {
                    predicados.add(cb.greaterThanOrEqualTo(root.get("data"), filtro.dataInicio()));
                }
                if (filtro.dataFim() != null) {
                    predicados.add(cb.lessThanOrEqualTo(root.get("data"), filtro.dataFim()));
                }
                if (filtro.veiculoId() != null) {
                    predicados.add(cb.equal(root.get("veiculo").get("id"), filtro.veiculoId()));
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
