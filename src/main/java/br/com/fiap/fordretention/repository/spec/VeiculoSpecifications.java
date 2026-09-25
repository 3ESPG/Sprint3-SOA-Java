package br.com.fiap.fordretention.repository.spec;

import br.com.fiap.fordretention.dto.veiculo.VeiculoFiltro;
import br.com.fiap.fordretention.model.Veiculo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class VeiculoSpecifications {

    private VeiculoSpecifications() {
    }

    /**
     * idadeMin=4 ⇒ ano ≤ anoAtual − 4; idadeMax=3 ⇒ ano ≥ anoAtual − 3.
     *
     * @param concessionariaId concessionária efetiva (já resolvida pelo escopo do usuário), pode ser nula.
     */
    public static Specification<Veiculo> filtrar(VeiculoFiltro filtro, Long concessionariaId, int anoAtual) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (concessionariaId != null) {
                predicados.add(cb.equal(root.get("cliente").get("concessionariaPreferida").get("id"), concessionariaId));
            }
            if (filtro != null) {
                if (SpecUtils.temTexto(filtro.modelo())) {
                    predicados.add(cb.like(cb.lower(root.get("modelo")), SpecUtils.contem(filtro.modelo()), '\\'));
                }
                if (filtro.idadeMin() != null) {
                    predicados.add(cb.lessThanOrEqualTo(root.get("ano"), anoAtual - filtro.idadeMin()));
                }
                if (filtro.idadeMax() != null) {
                    predicados.add(cb.greaterThanOrEqualTo(root.get("ano"), anoAtual - filtro.idadeMax()));
                }
                if (filtro.clienteId() != null) {
                    predicados.add(cb.equal(root.get("cliente").get("id"), filtro.clienteId()));
                }
                if (filtro.statusGarantia() != null) {
                    predicados.add(cb.equal(root.get("statusGarantia"), filtro.statusGarantia()));
                }
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
