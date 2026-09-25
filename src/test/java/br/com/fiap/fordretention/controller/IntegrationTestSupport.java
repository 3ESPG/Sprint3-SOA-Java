package br.com.fiap.fordretention.controller;

import br.com.fiap.fordretention.model.Cliente;
import br.com.fiap.fordretention.model.Concessionaria;
import br.com.fiap.fordretention.model.Lead;
import br.com.fiap.fordretention.model.Servico;
import br.com.fiap.fordretention.model.Usuario;
import br.com.fiap.fordretention.model.Veiculo;
import br.com.fiap.fordretention.model.enums.OrigemLead;
import br.com.fiap.fordretention.model.enums.PerfilCliente;
import br.com.fiap.fordretention.model.enums.Prioridade;
import br.com.fiap.fordretention.model.enums.Role;
import br.com.fiap.fordretention.model.enums.StatusGarantia;
import br.com.fiap.fordretention.model.enums.TipoServico;
import br.com.fiap.fordretention.repository.ClienteRepository;
import br.com.fiap.fordretention.repository.ConcessionariaRepository;
import br.com.fiap.fordretention.repository.LeadRepository;
import br.com.fiap.fordretention.repository.ServicoRepository;
import br.com.fiap.fordretention.repository.UsuarioRepository;
import br.com.fiap.fordretention.repository.VeiculoRepository;
import br.com.fiap.fordretention.security.JwtService;
import br.com.fiap.fordretention.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;

/**
 * Base dos testes de integração: sobe o contexto completo (segurança, JPA, H2) e cria um cenário
 * com duas concessionárias (SP e RJ), um usuário de cada perfil, clientes, veículos, serviço e leads.
 * Cada teste roda em uma transação revertida ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {

    static final String SENHA = "Senha1234";
    static final int ANO_ATUAL = Year.now().getValue();
    private static String senhaHash;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected ConcessionariaRepository concessionariaRepository;
    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected ClienteRepository clienteRepository;
    @Autowired
    protected VeiculoRepository veiculoRepository;
    @Autowired
    protected ServicoRepository servicoRepository;
    @Autowired
    protected LeadRepository leadRepository;

    protected Concessionaria concSp;
    protected Concessionaria concRj;
    protected Usuario admin;
    protected Usuario gestorSp;
    protected Usuario consultorSp;
    protected Usuario gestorRj;
    protected Usuario pendente;
    protected Cliente mariaSp;
    protected Cliente joaoSp;
    protected Cliente fernandaRj;
    protected Veiculo rangerAntigaMaria;
    protected Veiculo territoryNovaJoao;
    protected Veiculo kaFernanda;
    protected Lead leadMaria;
    protected Lead leadFernanda;

    @BeforeEach
    void criarCenario() {
        if (senhaHash == null) {
            senhaHash = passwordEncoder.encode(SENHA);
        }
        concSp = concessionariaRepository.save(
                new Concessionaria("Ford Central Paulista", "São Paulo", "SP", "11222333000181"));
        concRj = concessionariaRepository.save(
                new Concessionaria("Ford Rio Sul", "Rio de Janeiro", "RJ", "22333444000181"));

        admin = usuario("Admin", "admin@ford.com", Role.ADMIN, null, true);
        gestorSp = usuario("Gestor SP", "gestor.sp@ford.com", Role.GESTOR_CONCESSIONARIA, concSp, true);
        consultorSp = usuario("Consultor SP", "consultor.sp@ford.com", Role.CONSULTOR, concSp, true);
        gestorRj = usuario("Gestor RJ", "gestor.rj@ford.com", Role.GESTOR_CONCESSIONARIA, concRj, true);
        pendente = usuario("Pendente", "pendente@ford.com", Role.CONSULTOR, concSp, false);

        mariaSp = cliente("Maria Souza", "maria@email.com", concSp, PerfilCliente.ABANDONO, "0.9100");
        joaoSp = cliente("João Pereira", "joao@email.com", concSp, null, null);
        fernandaRj = cliente("Fernanda Dias", "fernanda@email.com", concRj, PerfilCliente.ESQUECIDO, "0.7500");

        rangerAntigaMaria = veiculoRepository.save(new Veiculo("9BFZZZ55LA0000001", "Ranger", ANO_ATUAL - 7,
                98_000, mariaSp, StatusGarantia.EXPIRADA));
        territoryNovaJoao = veiculoRepository.save(new Veiculo("9BFZZZ55LA0000002", "Territory", ANO_ATUAL - 1,
                8_000, joaoSp, StatusGarantia.ATIVA));
        kaFernanda = veiculoRepository.save(new Veiculo("9BFZZZ55LA0000003", "Ka", ANO_ATUAL - 5,
                70_000, fernandaRj, StatusGarantia.EXPIRADA));

        servicoRepository.save(new Servico(territoryNovaJoao, concSp, TipoServico.REVISAO, new BigDecimal("650.00"),
                LocalDate.now().minusMonths(2), true, "Revisão 10.000 km"));

        leadMaria = leadRepository.save(new Lead(mariaSp, rangerAntigaMaria, concSp, "Risco de abandono",
                Prioridade.ALTA, OrigemLead.MODELO_ML, LocalDateTime.now()));
        leadFernanda = leadRepository.save(new Lead(fernandaRj, kaFernanda, concRj, "Cliente esquecido",
                Prioridade.BAIXA, OrigemLead.MODELO_ML, LocalDateTime.now()));
    }

    /** Header Authorization com um JWT real, assinado pelo JwtService da aplicação. */
    protected String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(UsuarioAutenticado.of(usuario));
    }

    protected RequestPostProcessor tokenDe(Usuario usuario) {
        String header = bearer(usuario);
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, header);
            return request;
        };
    }

    private Usuario usuario(String nome, String email, Role role, Concessionaria concessionaria, boolean ativo) {
        return usuarioRepository.save(new Usuario(nome, email, senhaHash, role, concessionaria, ativo));
    }

    private Cliente cliente(String nome, String email, Concessionaria concessionaria, PerfilCliente perfil,
                            String score) {
        Cliente cliente = new Cliente(nome, email, "+5511999990000", concessionaria);
        cliente.setPerfil(perfil);
        cliente.setScoreRisco(score == null ? null : new BigDecimal(score));
        return clienteRepository.save(cliente);
    }
}
