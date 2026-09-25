package br.com.fiap.fordretention.model;

import br.com.fiap.fordretention.model.enums.PerfilCliente;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concessionaria_preferida_id", nullable = false)
    private Concessionaria concessionariaPreferida;

    /** Perfil calculado pelo modelo de ML; nulo até a primeira classificação. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PerfilCliente perfil;

    /** Probabilidade (0..1) de o cliente deixar a rede, informada pelo modelo. */
    @Column(name = "score_risco", precision = 5, scale = 4)
    private BigDecimal scoreRisco;

    @Column(name = "data_ultima_atualizacao_perfil")
    private LocalDateTime dataUltimaAtualizacaoPerfil;

    public Cliente() {
    }

    public Cliente(String nome, String email, String telefone, Concessionaria concessionariaPreferida) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.concessionariaPreferida = concessionariaPreferida;
    }

    public Long getConcessionariaPreferidaId() {
        return concessionariaPreferida == null ? null : concessionariaPreferida.getId();
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Concessionaria getConcessionariaPreferida() {
        return concessionariaPreferida;
    }

    public void setConcessionariaPreferida(Concessionaria concessionariaPreferida) {
        this.concessionariaPreferida = concessionariaPreferida;
    }

    public PerfilCliente getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilCliente perfil) {
        this.perfil = perfil;
    }

    public BigDecimal getScoreRisco() {
        return scoreRisco;
    }

    public void setScoreRisco(BigDecimal scoreRisco) {
        this.scoreRisco = scoreRisco;
    }

    public LocalDateTime getDataUltimaAtualizacaoPerfil() {
        return dataUltimaAtualizacaoPerfil;
    }

    public void setDataUltimaAtualizacaoPerfil(LocalDateTime dataUltimaAtualizacaoPerfil) {
        this.dataUltimaAtualizacaoPerfil = dataUltimaAtualizacaoPerfil;
    }
}
