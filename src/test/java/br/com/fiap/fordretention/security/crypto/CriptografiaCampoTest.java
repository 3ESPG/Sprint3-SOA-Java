package br.com.fiap.fordretention.security.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriptografiaCampoTest {

    private static String chave(char c) {
        return Base64.getEncoder().encodeToString(String.valueOf(c).repeat(32).getBytes());
    }

    private final CriptografiaCampo cripto = new CriptografiaCampo(new CriptografiaProperties(chave('a')));

    @Test
    @DisplayName("cifra e decifra; o texto cifrado não contém o valor original")
    void idaEVolta() {
        String cifrado = cripto.cifrar("+5511988887777");

        assertThat(cifrado).startsWith("v1:").doesNotContain("988887777");
        assertThat(cripto.decifrar(cifrado)).isEqualTo("+5511988887777");
    }

    @Test
    @DisplayName("IV aleatório: o mesmo valor gera textos cifrados diferentes")
    void ivAleatorio() {
        assertThat(cripto.cifrar("+5511988887777")).isNotEqualTo(cripto.cifrar("+5511988887777"));
    }

    @Test
    @DisplayName("GCM detecta adulteração do valor no banco")
    void detectaAdulteracao() {
        String cifrado = cripto.cifrar("+5511988887777");
        char[] c = cifrado.toCharArray();
        int i = c.length - 5;
        c[i] = c[i] == 'A' ? 'B' : 'A';

        assertThatThrownBy(() -> cripto.decifrar(new String(c)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("adulterado");
    }

    @Test
    @DisplayName("outra chave não consegue decifrar")
    void outraChave() {
        String cifrado = cripto.cifrar("+5511988887777");
        CriptografiaCampo outra = new CriptografiaCampo(new CriptografiaProperties(chave('b')));

        assertThatThrownBy(() -> outra.decifrar(cifrado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("valor legado em texto puro é devolvido como está; nulo continua nulo")
    void legadoENulo() {
        assertThat(cripto.decifrar("+5511999990000")).isEqualTo("+5511999990000");
        assertThat(cripto.cifrar(null)).isNull();
        assertThat(cripto.decifrar(null)).isNull();
    }

    @Test
    @DisplayName("chave com tamanho diferente de 32 bytes é recusada na subida")
    void chaveFraca() {
        String curta = Base64.getEncoder().encodeToString("curta".getBytes());
        assertThatThrownBy(() -> new CriptografiaCampo(new CriptografiaProperties(curta)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
