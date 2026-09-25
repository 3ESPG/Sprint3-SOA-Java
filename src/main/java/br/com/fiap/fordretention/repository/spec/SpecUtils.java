package br.com.fiap.fordretention.repository.spec;

import java.util.Locale;

final class SpecUtils {

    private SpecUtils() {
    }

    static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** Padrão LIKE "contém", em minúsculas e com curingas escapados. */
    static String contem(String valor) {
        String escapado = valor.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
