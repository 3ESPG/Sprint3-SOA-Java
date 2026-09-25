package br.com.fiap.fordretention.model;

import br.com.fiap.fordretention.model.enums.TipoServico;
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
import java.time.LocalDate;

@Entity
@Table(name = "servicos")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    /** Concessionária que executou o serviço. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concessionaria_id", nullable = false)
    private Concessionaria concessionaria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoServico tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_servico", nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private boolean pago;

    @Column(length = 255)
    private String descricao;

    public Servico() {
    }

    public Servico(Veiculo veiculo, Concessionaria concessionaria, TipoServico tipo, BigDecimal valor,
                   LocalDate data, boolean pago, String descricao) {
        this.veiculo = veiculo;
        this.concessionaria = concessionaria;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.pago = pago;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Concessionaria getConcessionaria() {
        return concessionaria;
    }

    public void setConcessionaria(Concessionaria concessionaria) {
        this.concessionaria = concessionaria;
    }

    public TipoServico getTipo() {
        return tipo;
    }

    public void setTipo(TipoServico tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public boolean isPago() {
        return pago;
    }

    public void setPago(boolean pago) {
        this.pago = pago;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
