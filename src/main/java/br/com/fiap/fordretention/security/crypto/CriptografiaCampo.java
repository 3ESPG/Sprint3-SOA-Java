package br.com.fiap.fordretention.security.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM (confidencialidade + integridade) para campos com dados pessoais.
 * Formato gravado: {@code v1:<base64(iv[12] || ciphertext || tag[16])>}. O IV é aleatório por valor,
 * então o mesmo telefone gera textos cifrados diferentes, e qualquer alteração no banco é detectada
 * pela tag na leitura. O prefixo de versão permite trocar algoritmo/chave no futuro.
 */
@Component
public class CriptografiaCampo {

    static final String PREFIXO = "v1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    public CriptografiaCampo(CriptografiaProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.fieldKey());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY deve estar em Base64", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY deve ter 32 bytes (AES-256); gere com openssl rand -base64 32");
        }
        this.chave = new SecretKeySpec(bytes, "AES");
    }

    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANHO_IV];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar campo", e);
        }
    }

    /**
     * Decifra um valor gravado por {@link #cifrar}. Valores sem o prefixo são legados em texto puro
     * (ainda não migrados) e são devolvidos como estão.
     *
     * @throws IllegalStateException se o valor foi adulterado ou cifrado com outra chave
     */
    public String decifrar(String valor) {
        if (valor == null || !estaCifrado(valor)) {
            return valor;
        }
        try {
            ByteBuffer entrada = ByteBuffer.wrap(Base64.getDecoder().decode(valor.substring(PREFIXO.length())));
            byte[] iv = new byte[TAMANHO_IV];
            entrada.get(iv);
            byte[] cifrado = new byte[entrada.remaining()];
            entrada.get(cifrado);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException | java.nio.BufferUnderflowException e) {
            throw new IllegalStateException("Campo cifrado inválido ou adulterado", e);
        }
    }

    public boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIXO);
    }
}
