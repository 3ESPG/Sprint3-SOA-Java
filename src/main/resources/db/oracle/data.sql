-- =====================================================================
-- Ford Retention AI – dados de exemplo para ORACLE (perfil prod)
-- Executado apenas com DB_SEED=always. É idempotente: só insere se a
-- tabela CONCESSIONARIAS estiver vazia, então pode rodar várias vezes.
-- Senha de TODOS os usuários de teste: Ford@2026
-- Os ids são gerados pelas colunas IDENTITY e capturados com RETURNING.
-- Blocos separados por "/" (spring.sql.init.separator).
-- =====================================================================
DECLARE
    v_qtd   NUMBER;
    v_hash  CONSTANT VARCHAR2(100) := '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda';
    v_agora CONSTANT TIMESTAMP := LOCALTIMESTAMP;
    v_hoje  CONSTANT DATE := TRUNC(SYSDATE);
    -- concessionárias
    c_sp NUMBER; c_rj NUMBER; c_mg NUMBER;
    -- clientes
    cl_joao NUMBER; cl_maria NUMBER; cl_carlos NUMBER; cl_ana NUMBER; cl_pedro NUMBER;
    cl_fernanda NUMBER; cl_lucas NUMBER; cl_juliana NUMBER; cl_rafael NUMBER; cl_beatriz NUMBER;
    -- veículos
    v1 NUMBER; v2 NUMBER; v3 NUMBER; v4 NUMBER; v5 NUMBER; v6 NUMBER;
    v7 NUMBER; v8 NUMBER; v9 NUMBER; v10 NUMBER; v11 NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_qtd FROM concessionarias;
    IF v_qtd > 0 THEN
        RETURN;
    END IF;

    -- ---------- Concessionárias ----------
    INSERT INTO concessionarias (nome, cidade, estado, cnpj)
    VALUES ('Ford Central Paulista', 'São Paulo', 'SP', '11222333000181') RETURNING id INTO c_sp;
    INSERT INTO concessionarias (nome, cidade, estado, cnpj)
    VALUES ('Ford Rio Sul', 'Rio de Janeiro', 'RJ', '22333444000181') RETURNING id INTO c_rj;
    INSERT INTO concessionarias (nome, cidade, estado, cnpj)
    VALUES ('Ford Minas Motors', 'Belo Horizonte', 'MG', '33444555000181') RETURNING id INTO c_mg;

    -- ---------- Usuários ----------
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Administrador Ford', 'admin@ford.com', v_hash, 'ADMIN', NULL, 1);
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Serviço de ML (Python)', 'ml-service@ford.com', v_hash, 'ADMIN', NULL, 1);
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Paula Gestora SP', 'gestor.sp@fordcentral.com.br', v_hash, 'GESTOR_CONCESSIONARIA', c_sp, 1);
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Ricardo Gestor RJ', 'gestor.rj@fordriosul.com.br', v_hash, 'GESTOR_CONCESSIONARIA', c_rj, 1);
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Bruno Consultor SP', 'consultor.sp@fordcentral.com.br', v_hash, 'CONSULTOR', c_sp, 1);
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Camila Consultora RJ', 'consultor.rj@fordriosul.com.br', v_hash, 'CONSULTOR', c_rj, 1);
    INSERT INTO usuarios (nome, email, senha, role, concessionaria_id, ativo)
    VALUES ('Diego Aguardando Aprovação', 'pendente@fordcentral.com.br', v_hash, 'CONSULTOR', c_sp, 0);

    -- ---------- Clientes ----------
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('João Pereira', 'joao.pereira@email.com', '+5511988880001', c_sp, 'FIEL', 0.12, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_joao;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Maria Souza', 'maria.souza@email.com', '+5511988880002', c_sp, 'ABANDONO', 0.91, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_maria;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Carlos Lima', 'carlos.lima@email.com', '+5511988880003', c_sp, 'ESQUECIDO', 0.78, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_carlos;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Ana Rocha', 'ana.rocha@email.com', '+5511988880004', c_sp, 'ECONOMICO', 0.55, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_ana;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Pedro Alves', 'pedro.alves@email.com', '+5511988880005', c_sp, NULL, NULL, NULL) RETURNING id INTO cl_pedro;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Fernanda Dias', 'fernanda.dias@email.com', '+5521977770006', c_rj, 'ABANDONO', 0.84, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_fernanda;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Lucas Martins', 'lucas.martins@email.com', '+5521977770007', c_rj, 'FIEL', 0.08, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_lucas;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Juliana Castro', 'juliana.castro@email.com', '+5521977770008', c_rj, 'ESQUECIDO', 0.72, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_juliana;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Rafael Gomes', 'rafael.gomes@email.com', '+5531966660009', c_mg, 'ECONOMICO', 0.40, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_rafael;
    INSERT INTO clientes (nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil)
    VALUES ('Beatriz Nunes', 'beatriz.nunes@email.com', '+5531966660010', c_mg, 'ABANDONO', 0.95, v_agora - INTERVAL '7' DAY) RETURNING id INTO cl_beatriz;

    -- ---------- Veículos ----------
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000001', 'Ranger', 2023, 25000, cl_joao, 'ATIVA') RETURNING id INTO v1;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000002', 'Territory', 2025, 8000, cl_joao, 'ATIVA') RETURNING id INTO v2;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000003', 'Ranger', 2019, 98000, cl_maria, 'EXPIRADA') RETURNING id INTO v3;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000004', 'EcoSport', 2018, 110000, cl_carlos, 'EXPIRADA') RETURNING id INTO v4;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000005', 'Ka', 2020, 70000, cl_ana, 'EXPIRADA') RETURNING id INTO v5;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000006', 'Maverick', 2024, 15000, cl_pedro, 'ATIVA') RETURNING id INTO v6;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000007', 'Ranger', 2021, 60000, cl_fernanda, 'EXPIRADA') RETURNING id INTO v7;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000008', 'Bronco Sport', 2022, 40000, cl_lucas, 'ESTENDIDA') RETURNING id INTO v8;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000009', 'Ka', 2019, 85000, cl_juliana, 'EXPIRADA') RETURNING id INTO v9;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000010', 'Ranger', 2017, 130000, cl_rafael, 'EXPIRADA') RETURNING id INTO v10;
    INSERT INTO veiculos (vin, modelo, ano, quilometragem, cliente_id, status_garantia)
    VALUES ('9BFZZZ55LA0000011', 'Territory', 2021, 55000, cl_beatriz, 'EXPIRADA') RETURNING id INTO v11;

    -- ---------- Serviços (datas relativas a hoje) ----------
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v1, c_sp, 'REVISAO', 650.00, ADD_MONTHS(v_hoje, -2), 1, 'Revisão 20.000 km');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v2, c_sp, 'GARANTIA', 0.00, ADD_MONTHS(v_hoje, -4), 0, 'Troca de sensor em garantia');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v3, c_sp, 'REVISAO', 980.00, ADD_MONTHS(v_hoje, -20), 1, 'Revisão 80.000 km');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v4, c_sp, 'REPARO', 1750.00, ADD_MONTHS(v_hoje, -18), 1, 'Embreagem');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v5, c_rj, 'PECAS', 420.00, ADD_MONTHS(v_hoje, -5), 1, 'Pastilhas de freio (atendido no RJ)');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v6, c_sp, 'REVISAO', 480.00, ADD_MONTHS(v_hoje, -1), 1, 'Revisão 10.000 km');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v7, c_rj, 'REVISAO', 890.00, ADD_MONTHS(v_hoje, -15), 1, 'Revisão 50.000 km');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v8, c_rj, 'REVISAO', 890.00, ADD_MONTHS(v_hoje, -3), 1, 'Revisão 40.000 km');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v8, c_rj, 'REPARO', 1200.00, ADD_MONTHS(v_hoje, -6), 1, 'Suspensão dianteira');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v10, c_mg, 'REPARO', 2300.00, ADD_MONTHS(v_hoje, -8), 1, 'Bomba injetora');
    INSERT INTO servicos (veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao)
    VALUES (v11, c_mg, 'RECALL', 0.00, ADD_MONTHS(v_hoje, -2), 0, 'Recall airbag');

    -- ---------- Leads ----------
    INSERT INTO leads (cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao)
    VALUES (cl_maria, v3, c_sp, 'Modelo de ML classificou o cliente como Cliente de Abandono (score 0.91). Ranger 2019 — alto risco de sair da rede Ford: oferecer revisão com condição especial.',
            'ALTA', 'ABERTO', 'MODELO_ML', NULL, v_agora - INTERVAL '7' DAY, v_agora - INTERVAL '7' DAY);
    INSERT INTO leads (cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao)
    VALUES (cl_carlos, v4, c_sp, 'Modelo de ML classificou o cliente como Cliente Esquecido (score 0.78). EcoSport 2018 — sem serviço recente: enviar lembrete de manutenção preventiva.',
            'BAIXA', 'CONTATADO', 'MODELO_ML', 'Cliente pediu retorno na próxima semana', v_agora - INTERVAL '7' DAY, v_agora - INTERVAL '2' DAY);
    INSERT INTO leads (cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao)
    VALUES (cl_fernanda, v7, c_rj, 'Modelo de ML classificou o cliente como Cliente de Abandono (score 0.84). Ranger 2021 — alto risco de sair da rede Ford: oferecer revisão com condição especial.',
            'MEDIA', 'ABERTO', 'MODELO_ML', NULL, v_agora - INTERVAL '7' DAY, v_agora - INTERVAL '7' DAY);
    INSERT INTO leads (cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao)
    VALUES (cl_juliana, v9, c_rj, 'Modelo de ML classificou o cliente como Cliente Esquecido (score 0.72). Ka 2019 — sem serviço recente: enviar lembrete de manutenção preventiva.',
            'BAIXA', 'AGENDADO', 'MODELO_ML', 'Revisão agendada para o dia 10', v_agora - INTERVAL '7' DAY, v_agora - INTERVAL '1' DAY);
    INSERT INTO leads (cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao)
    VALUES (cl_beatriz, v11, c_mg, 'Modelo de ML classificou o cliente como Cliente de Abandono (score 0.95). Territory 2021 — alto risco de sair da rede Ford: oferecer revisão com condição especial.',
            'ALTA', 'ABERTO', 'MODELO_ML', NULL, v_agora - INTERVAL '7' DAY, v_agora - INTERVAL '7' DAY);
    INSERT INTO leads (cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao)
    VALUES (cl_ana, v5, c_sp, 'Cliente econômico: oferecer pacote de revisão com peças Motorcraft',
            'MEDIA', 'PERDIDO', 'MANUAL', 'Cliente optou por oficina independente', v_agora - INTERVAL '30' DAY, v_agora - INTERVAL '20' DAY);

    COMMIT;
END;
/
