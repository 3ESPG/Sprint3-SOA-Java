-- =====================================================================
-- Ford Retention AI – dados de exemplo (perfil dev / H2)
-- Senha de TODOS os usuários de teste: Ford@2026  (hash BCrypt abaixo)
-- Datas de serviços e leads são relativas a hoje, para o Service Share
-- dos últimos 12 meses sempre ter dados coerentes.
-- =====================================================================

-- ---------- Concessionárias ----------
INSERT INTO concessionarias (id, nome, cidade, estado, cnpj) VALUES
  (1, 'Ford Central Paulista', 'São Paulo',      'SP', '11222333000181'),
  (2, 'Ford Rio Sul',          'Rio de Janeiro', 'RJ', '22333444000181'),
  (3, 'Ford Minas Motors',     'Belo Horizonte', 'MG', '33444555000181');

-- ---------- Usuários (um de cada perfil + usuário técnico do ML + um pendente) ----------
INSERT INTO usuarios (id, nome, email, senha, role, concessionaria_id, ativo) VALUES
  (1, 'Administrador Ford',        'admin@ford.com',                  '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'ADMIN',                 NULL, TRUE),
  (2, 'Serviço de ML (Python)',    'ml-service@ford.com',             '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'ADMIN',                 NULL, TRUE),
  (3, 'Paula Gestora SP',          'gestor.sp@fordcentral.com.br',    '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'GESTOR_CONCESSIONARIA', 1,    TRUE),
  (4, 'Ricardo Gestor RJ',         'gestor.rj@fordriosul.com.br',     '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'GESTOR_CONCESSIONARIA', 2,    TRUE),
  (5, 'Bruno Consultor SP',        'consultor.sp@fordcentral.com.br', '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'CONSULTOR',             1,    TRUE),
  (6, 'Camila Consultora RJ',      'consultor.rj@fordriosul.com.br',  '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'CONSULTOR',             2,    TRUE),
  (7, 'Diego Aguardando Aprovação','pendente@fordcentral.com.br',     '$2a$10$054WsgDcGKzY2YAK/sZ/1e4/h/J7tS7yzYs5x5G8834ZMTXyQXKda', 'CONSULTOR',             1,    FALSE);

-- ---------- Clientes (com os 4 perfis do modelo de ML) ----------
INSERT INTO clientes (id, nome, email, telefone, concessionaria_preferida_id, perfil, score_risco, data_ultima_atualizacao_perfil) VALUES
  (1,  'João Pereira',   'joao.pereira@email.com',   '+5511988880001', 1, 'FIEL',      0.1200, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (2,  'Maria Souza',    'maria.souza@email.com',    '+5511988880002', 1, 'ABANDONO',  0.9100, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (3,  'Carlos Lima',    'carlos.lima@email.com',    '+5511988880003', 1, 'ESQUECIDO', 0.7800, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (4,  'Ana Rocha',      'ana.rocha@email.com',      '+5511988880004', 1, 'ECONOMICO', 0.5500, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (5,  'Pedro Alves',    'pedro.alves@email.com',    '+5511988880005', 1, NULL,        NULL,   NULL),
  (6,  'Fernanda Dias',  'fernanda.dias@email.com',  '+5521977770006', 2, 'ABANDONO',  0.8400, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (7,  'Lucas Martins',  'lucas.martins@email.com',  '+5521977770007', 2, 'FIEL',      0.0800, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (8,  'Juliana Castro', 'juliana.castro@email.com', '+5521977770008', 2, 'ESQUECIDO', 0.7200, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (9,  'Rafael Gomes',   'rafael.gomes@email.com',   '+5531966660009', 3, 'ECONOMICO', 0.4000, DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (10, 'Beatriz Nunes',  'beatriz.nunes@email.com',  '+5531966660010', 3, 'ABANDONO',  0.9500, DATEADD('DAY', -7, LOCALTIMESTAMP));

-- ---------- Veículos (mistura de novos e antigos, onde o Service Share cai) ----------
INSERT INTO veiculos (id, vin, modelo, ano, quilometragem, cliente_id, status_garantia) VALUES
  (1,  '9BFZZZ55LA0000001', 'Ranger',        2023,  25000, 1,  'ATIVA'),
  (2,  '9BFZZZ55LA0000002', 'Territory',     2025,   8000, 1,  'ATIVA'),
  (3,  '9BFZZZ55LA0000003', 'Ranger',        2019,  98000, 2,  'EXPIRADA'),
  (4,  '9BFZZZ55LA0000004', 'EcoSport',      2018, 110000, 3,  'EXPIRADA'),
  (5,  '9BFZZZ55LA0000005', 'Ka',            2020,  70000, 4,  'EXPIRADA'),
  (6,  '9BFZZZ55LA0000006', 'Maverick',      2024,  15000, 5,  'ATIVA'),
  (7,  '9BFZZZ55LA0000007', 'Ranger',        2021,  60000, 6,  'EXPIRADA'),
  (8,  '9BFZZZ55LA0000008', 'Bronco Sport',  2022,  40000, 7,  'ESTENDIDA'),
  (9,  '9BFZZZ55LA0000009', 'Ka',            2019,  85000, 8,  'EXPIRADA'),
  (10, '9BFZZZ55LA0000010', 'Ranger',        2017, 130000, 9,  'EXPIRADA'),
  (11, '9BFZZZ55LA0000011', 'Territory',     2021,  55000, 10, 'EXPIRADA');

-- ---------- Serviços (datas relativas a hoje) ----------
INSERT INTO servicos (id, veiculo_id, concessionaria_id, tipo, valor, data_servico, pago, descricao) VALUES
  (1,  1,  1, 'REVISAO',   650.00, DATEADD('MONTH', -2,  CURRENT_DATE), TRUE,  'Revisão 20.000 km'),
  (2,  2,  1, 'GARANTIA',    0.00, DATEADD('MONTH', -4,  CURRENT_DATE), FALSE, 'Troca de sensor em garantia'),
  (3,  3,  1, 'REVISAO',   980.00, DATEADD('MONTH', -20, CURRENT_DATE), TRUE,  'Revisão 80.000 km'),
  (4,  4,  1, 'REPARO',   1750.00, DATEADD('MONTH', -18, CURRENT_DATE), TRUE,  'Embreagem'),
  (5,  5,  2, 'PECAS',     420.00, DATEADD('MONTH', -5,  CURRENT_DATE), TRUE,  'Pastilhas de freio (atendido no RJ)'),
  (6,  6,  1, 'REVISAO',   480.00, DATEADD('MONTH', -1,  CURRENT_DATE), TRUE,  'Revisão 10.000 km'),
  (7,  7,  2, 'REVISAO',   890.00, DATEADD('MONTH', -15, CURRENT_DATE), TRUE,  'Revisão 50.000 km'),
  (8,  8,  2, 'REVISAO',   890.00, DATEADD('MONTH', -3,  CURRENT_DATE), TRUE,  'Revisão 40.000 km'),
  (9,  8,  2, 'REPARO',   1200.00, DATEADD('MONTH', -6,  CURRENT_DATE), TRUE,  'Suspensão dianteira'),
  (10, 10, 3, 'REPARO',   2300.00, DATEADD('MONTH', -8,  CURRENT_DATE), TRUE,  'Bomba injetora'),
  (11, 11, 3, 'RECALL',      0.00, DATEADD('MONTH', -2,  CURRENT_DATE), FALSE, 'Recall airbag');

-- ---------- Leads (gerados pelo ML e manuais, em várias etapas do funil) ----------
INSERT INTO leads (id, cliente_id, veiculo_id, concessionaria_id, motivo, prioridade, status, origem, observacao, data_criacao, data_atualizacao) VALUES
  (1, 2,  3,  1, 'Modelo de ML classificou o cliente como Cliente de Abandono (score 0.91). Ranger 2019 — alto risco de sair da rede Ford: oferecer revisão com condição especial.', 'ALTA',  'ABERTO',    'MODELO_ML', NULL, DATEADD('DAY', -7, LOCALTIMESTAMP), DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (2, 3,  4,  1, 'Modelo de ML classificou o cliente como Cliente Esquecido (score 0.78). EcoSport 2018 — sem serviço recente: enviar lembrete de manutenção preventiva.', 'BAIXA', 'CONTATADO', 'MODELO_ML', 'Cliente pediu retorno na próxima semana', DATEADD('DAY', -7, LOCALTIMESTAMP), DATEADD('DAY', -2, LOCALTIMESTAMP)),
  (3, 6,  7,  2, 'Modelo de ML classificou o cliente como Cliente de Abandono (score 0.84). Ranger 2021 — alto risco de sair da rede Ford: oferecer revisão com condição especial.', 'MEDIA', 'ABERTO',    'MODELO_ML', NULL, DATEADD('DAY', -7, LOCALTIMESTAMP), DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (4, 8,  9,  2, 'Modelo de ML classificou o cliente como Cliente Esquecido (score 0.72). Ka 2019 — sem serviço recente: enviar lembrete de manutenção preventiva.', 'BAIXA', 'AGENDADO',  'MODELO_ML', 'Revisão agendada para o dia 10', DATEADD('DAY', -7, LOCALTIMESTAMP), DATEADD('DAY', -1, LOCALTIMESTAMP)),
  (5, 10, 11, 3, 'Modelo de ML classificou o cliente como Cliente de Abandono (score 0.95). Territory 2021 — alto risco de sair da rede Ford: oferecer revisão com condição especial.', 'ALTA',  'ABERTO',    'MODELO_ML', NULL, DATEADD('DAY', -7, LOCALTIMESTAMP), DATEADD('DAY', -7, LOCALTIMESTAMP)),
  (6, 4,  5,  1, 'Cliente econômico: oferecer pacote de revisão com peças Motorcraft', 'MEDIA', 'PERDIDO', 'MANUAL', 'Cliente optou por oficina independente', DATEADD('DAY', -30, LOCALTIMESTAMP), DATEADD('DAY', -20, LOCALTIMESTAMP));

-- Reinicia as sequências de identidade após os inserts com id explícito
ALTER TABLE concessionarias ALTER COLUMN id RESTART WITH 100;
ALTER TABLE usuarios        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE clientes        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE veiculos        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE servicos        ALTER COLUMN id RESTART WITH 100;
ALTER TABLE leads           ALTER COLUMN id RESTART WITH 100;
