package br.com.fiap.fordretention.security;

/**
 * Utilitários para logs de segurança. Regra: nunca logar senha, token, telefone ou dados de cliente;
 * e-mail só mascarado. Contexto da requisição (traceId, ip, rota, usuarioId) vem do MDC.
 */
public final class LogSeguranca {

    private LogSeguranca() {
    }

    /** "consultor.sp@ford.com" → "c***@ford.com". */
    public static String mascararEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }
}
