package br.com.fiap.fordretention.security;

import br.com.fiap.fordretention.model.Usuario;
import br.com.fiap.fordretention.model.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal da aplicação. No login carrega o hash da senha (para o BCrypt comparar);
 * quando reconstruído a partir do JWT, a senha é nula.
 */
public record UsuarioAutenticado(
        Long id,
        String nome,
        String email,
        String senha,
        Role role,
        Long concessionariaId,
        boolean ativo
) implements UserDetails {

    public static UsuarioAutenticado of(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getSenha(),
                usuario.getRole(), usuario.getConcessionariaId(), usuario.isAtivo());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[id=%d, email=%s, role=%s, concessionariaId=%s]"
                .formatted(id, email, role, concessionariaId);
    }
}
