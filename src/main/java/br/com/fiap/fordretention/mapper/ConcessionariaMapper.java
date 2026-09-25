package br.com.fiap.fordretention.mapper;

import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaPatchRequest;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaRequest;
import br.com.fiap.fordretention.dto.concessionaria.ConcessionariaResponse;
import br.com.fiap.fordretention.model.Concessionaria;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ConcessionariaMapper {

    public Concessionaria toEntity(ConcessionariaRequest request) {
        Concessionaria concessionaria = new Concessionaria();
        atualizar(concessionaria, request);
        return concessionaria;
    }

    public void atualizar(Concessionaria concessionaria, ConcessionariaRequest request) {
        concessionaria.setNome(request.nome().trim());
        concessionaria.setCidade(request.cidade().trim());
        concessionaria.setEstado(normalizarUf(request.estado()));
        concessionaria.setCnpj(normalizarCnpj(request.cnpj()));
    }

    public void aplicarPatch(Concessionaria concessionaria, ConcessionariaPatchRequest request) {
        if (request.nome() != null) {
            concessionaria.setNome(request.nome().trim());
        }
        if (request.cidade() != null) {
            concessionaria.setCidade(request.cidade().trim());
        }
        if (request.estado() != null) {
            concessionaria.setEstado(normalizarUf(request.estado()));
        }
    }

    public ConcessionariaResponse toResponse(Concessionaria c) {
        return new ConcessionariaResponse(c.getId(), c.getNome(), c.getCidade(), c.getEstado(), c.getCnpj());
    }

    /** Armazena o CNPJ só com dígitos, para garantir unicidade independente da formatação. */
    public static String normalizarCnpj(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "");
    }

    private static String normalizarUf(String uf) {
        return uf.trim().toUpperCase(Locale.ROOT);
    }
}
