package br.com.fiap.fordretention.security.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Na subida, cifra telefones gravados em texto puro (carga inicial data.sql ou dados anteriores a esta
 * versão). Idempotente: valores já cifrados (prefixo v1:) são ignorados.
 */
@Component
public class MigracaoCriptografiaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoCriptografiaRunner.class);

    private final JdbcTemplate jdbc;
    private final CriptografiaCampo criptografia;

    public MigracaoCriptografiaRunner(JdbcTemplate jdbc, CriptografiaCampo criptografia) {
        this.jdbc = jdbc;
        this.criptografia = criptografia;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> legados = jdbc.queryForList(
                "SELECT id, telefone FROM clientes WHERE telefone IS NOT NULL AND telefone NOT LIKE 'v1:%'");
        for (Map<String, Object> linha : legados) {
            jdbc.update("UPDATE clientes SET telefone = ? WHERE id = ?",
                    criptografia.cifrar((String) linha.get("TELEFONE")), linha.get("ID"));
        }
        if (!legados.isEmpty()) {
            log.atInfo()
                    .addKeyValue("evento", "crypto.migracao_concluida")
                    .addKeyValue("registros", legados.size())
                    .log("Telefones legados cifrados com AES-256-GCM");
        }
    }
}
