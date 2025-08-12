INSERT INTO tb_roles (name)
SELECT 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM tb_roles WHERE LOWER(name)='admin');

INSERT INTO tb_roles (name)
SELECT 'BASIC'
WHERE NOT EXISTS (SELECT 1 FROM tb_roles WHERE LOWER(name)='basic');