package br.com.fiap.fordretention.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Cifra o atributo ao gravar e decifra ao ler; o restante da aplicação só vê o valor em claro.
 * Instanciado pelo Spring (SpringBeanContainer do Hibernate), por isso recebe a {@link CriptografiaCampo}.
 */
@Converter
public class CampoCifradoConverter implements AttributeConverter<String, String> {

    private final CriptografiaCampo criptografia;

    public CampoCifradoConverter(CriptografiaCampo criptografia) {
        this.criptografia = criptografia;
    }

    @Override
    public String convertToDatabaseColumn(String valor) {
        return criptografia.cifrar(valor);
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        return criptografia.decifrar(valor);
    }
}
