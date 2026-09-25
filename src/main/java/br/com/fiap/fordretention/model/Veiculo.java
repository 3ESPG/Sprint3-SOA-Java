package br.com.fiap.fordretention.model;

import br.com.fiap.fordretention.model.enums.StatusGarantia;
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
@Table(name = "veiculos")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(nullable = false, length = 60)
    private String modelo;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer quilometragem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_garantia", nullable = false, length = 20)
    private StatusGarantia statusGarantia;

    public Veiculo() {
    }

    public Veiculo(String vin, String modelo, Integer ano, Integer quilometragem, Cliente cliente,
                   StatusGarantia statusGarantia) {
        this.vin = vin;
        this.modelo = modelo;
        this.ano = ano;
        this.quilometragem = quilometragem;
        this.cliente = cliente;
        this.statusGarantia = statusGarantia;
    }

    /** Idade do veículo em anos, dado o ano corrente. */
    public int idade(int anoAtual) {
        return Math.max(0, anoAtual - ano);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Integer quilometragem) {
        this.quilometragem = quilometragem;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public StatusGarantia getStatusGarantia() {
        return statusGarantia;
    }

    public void setStatusGarantia(StatusGarantia statusGarantia) {
        this.statusGarantia = statusGarantia;
    }
}
