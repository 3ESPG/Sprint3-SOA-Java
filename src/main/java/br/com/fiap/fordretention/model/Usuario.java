package br.com.fiap.fordretention.model;

import br.com.fiap.fordretention.model.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt da senha. */
    @Column(nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /** Nulo apenas para ADMIN. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concessionaria_id")
    private Concessionaria concessionaria;

    /** Contas criadas pelo auto-cadastro nascem inativas até serem aprovadas. */
    @Column(nullable = false)
    private boolean ativo;

    public Usuario() {
    }

    public Usuario(String nome, String email, String senha, Role role, Concessionaria concessionaria, boolean ativo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.role = role;
        this.concessionaria = concessionaria;
        this.ativo = ativo;
    }

    public Long getConcessionariaId() {
        return concessionaria == null ? null : concessionaria.getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Concessionaria getConcessionaria() {
        return concessionaria;
    }

    public void setConcessionaria(Concessionaria concessionaria) {
        this.concessionaria = concessionaria;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
