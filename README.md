# API Spring Security 6 + JWT + OAuth2

Este repositório contém uma API REST escrita em Java que demonstra como proteger endpoints com **Spring Security 6**, **JWT** (JSON Web Token) e **OAuth 2**. O projeto utiliza **Spring Boot** e disponibiliza endpoints para cadastro de usuários, autenticação via token, publicação de tweets e listagem de um feed, com controle de acesso baseado em perfis. Ele foi idealizado como um exemplo de aplicação que usa a abordagem stateless (sem sessão) configurada pelo `SecurityConfig`.

--- 

## Principais características

- **Java 21 e Spring Boot 3.5.4** – A versão Java utilizada é definida no pom.xml como 21. O projeto usa Spring Boot, que simplifica o bootstrap da aplicação e a configuração do ambiente.
- **Autenticação e autorização com JWT** – O endpoint `/login` valida o usuário e gera um JWT assinado com chaves RSA presentes em `src/main/resources` (`app.key` e `app.pub`); o token inclui as *authorities* (funções) do usuário como *scopes*. O `SecurityConfig` configura o servidor de recursos OAuth 2 para aceitar tokens Bearer e aplica política de sessão `STATELESS`.
- **Perfis de acesso** – Dois perfis são definidos: `ADMIN` e `BASIC`. O script `data.sql` garante a criação inicial desses perfis. A classe `AdminUserConfig` cria um usuário administrador padrão (usuário `admin` com senha `123`) caso ele não exista.
- **Banco de dados PostgreSQL** – Configurado em `application.properties` com URL padrão `jdbc:postgresql://localhost:5432/spring-security`, usuário `admin` e senha `123`. O *docker-compose* incluso fornece um contêiner para o Postgres.
- **Camadas de domínio** – O domínio inclui as entidades `User`, `Role` e `Tweet`, mapeadas com JPA. `User` possui um `UUID` como chave primária, campos `username`, `password` e associação *many‑to‑many* com `Role`. `Role` define os perfis e é armazenado na tabela `tb_roles`. `Tweet` representa um post no feed, contendo o texto e carimbo de tempo de criação.
- **Paginação de feed e validação de permissões** – O endpoint `/feed` retorna uma lista paginada de tweets ordenada por data de criação. Somente administradores ou o próprio autor podem excluir um tweet.
- **Dependências principais** – Entre as dependências declaradas no `pom.xml` estão `spring‑boot‑starter‑data‑jpa`, `spring‑boot‑starter‑security`, `spring‑boot‑starter‑oauth2‑resource‑server`, `spring‑boot‑starter‑web` e o driver `postgresql`. Também é utilizado `lombok` para reduzir código repetitivo.

## Pré-requisitos

Para executar a aplicação localmente você precisará de:

- **Java JDK 21**
- **Maven 3.8**+ (já incluído no projeto via `mvnw`/`mvnw.cmd` caso deseje usar o *wrapper*)
- **PostgreSQL** 14 ou superior ou **Docker** para subir o banco via *docker‑compose*

Opcionalmente, use o arquivo `docker/docker-compose.yaml` para iniciar um contêiner Postgres já configurado com as credenciais esperadas pela aplicação:

```bash
cd docker
docker-compose up -d
```

Esse arquivo cria um serviço `postgres-db` com usuário **admin**, senha **123** e banco **spring-security**.

## Configuração

Os parâmetros sensíveis da aplicação são definidos em `src/main/resources/application.properties`. Os principais são:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/spring-security` – define a URL do banco.
- `spring.datasource.username` / `spring.datasource.password` – definem usuário e senha do Postgres.
- `spring.jpa.hibernate.ddl-auto=update` – atualiza o esquema conforme as entidades.
- `jwt.public.key` e `jwt.private.key` – apontam para os arquivos de chave pública e privada no classpath que assinam e validam os tokens.

> **Atenção:** As chaves RSA fornecidas no repositório são apenas para exemplo.  
> Gere suas próprias chaves para uso em produção e atualize os caminhos em `application.properties`.

## Executando a aplicação

1. **Clone o repositório**

```bash
git clone https://github.com/MTaier/Spring-Security.git
cd Spring-Security
```

2. **Configure o banco de dados**

- Se estiver usando Docker, execute `docker-compose up -d` no diretório `docker` conforme descrito acima.
- Para usar uma instância Postgres local, crie um banco chamado `spring-security` e ajuste `spring.datasource.url`, `username` e `password` em `application.properties`.

3. **Compile e execute**

Utilize o Maven wrapper incluso ou sua instalação local de Maven:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

A aplicação iniciará na porta **8080**. O console exibirá que o usuário administrador foi criado caso ainda não exista.

## Uso da API

Após iniciar a aplicação, utilize um cliente HTTP (como curl, Postman ou Insomnia) para interagir com a API. Todos os endpoints retornam e esperam dados em JSON.

### Registrar usuário

- **Endpoint**: `POST /users`
- **Descrição**: Cria um novo usuário com perfil **BASIC**. Não requer autenticação.
- **Corpo da requisição**:

  ```json
  {
    "username": "joao",
    "password": "senhaSegura"
  }
  ```

- **Resposta**: `200 OK` em caso de sucesso ou `422 Unprocessable Entity` se o nome de usuário já existir.

### Autenticação e obtenção de token

- **Endpoint**: `POST /login`
- **Descrição**: Autentica um usuário existente e retorna um token JWT válido por 300 segundos.
- **Corpo da requisição**:

  ```json
  {
    "username": "admin",
    "password": "123"
  }
  ```

- **Resposta de sucesso** `(200 OK)`:

  ```json
  {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR...",
    "expiresIn": "300"
  }
  ```
  
Utilize o valor de `accessToken` como *Bearer* no cabeçalho `Authorization` das chamadas subsequentes.

### Listar usuários (apenas ADMIN)

- **Endpoint**: `GET /users`
- **Descrição**: Retorna a lista de usuários cadastrados. Acesso restrito a tokens com escopo `ADMIN`.
- **Cabeçalho de autorização**: `Authorization: Bearer <token>`

### Publicar tweet

- **Endpoint**: `POST /tweets`
- **Descrição**: Cria um novo tweet associado ao usuário autenticado.
- **Cabeçalho de autorização**: `Authorization: Bearer <token>`
- **Corpo da requisição**:

  ```json
  {
    "content": "Olá, mundo!"
  }
  ```

- **Resposta**: `200 OK` quando a publicação for salva.

### Listar feed

- **Endpoint**: `GET /feed`
- **Descrição**: Retorna os tweets ordenados do mais recente para o mais antigo, com suporte a paginação. Qualquer usuário autenticado pode acessar.
- **Cabeçalho de autorização**: `Authorization: Bearer <token>`
- **Parâmetros de consulta opcionais:**
  - `page` (padrão = 0)
  - `pageSize` (padrão = 10)
 
- **Resposta**:

  ```json
  {
    "feedItems": [
      {
        "tweetId": 1,
        "content": "Texto do tweet",
        "username": "joao"
      },
      ...
    ],
    "page": 0,
    "pageSize": 10,
    "totalPages": 1,
    "totalElements": 7
  }
  ```
  
A implementação utiliza `PageRequest` e `Sort.Direction.DESC` para ordenar os tweets

### Excluir tweet

- **Endpoint**: `DELETE /tweets/{id}`
- **Descrição**: Remove um tweet pelo seu identificador. Apenas o autor do tweet ou um administrador pode executar a remoção.
- **Cabeçalho de autorização**: `Authorization: Bearer <token>`
- **Resposta**: `200 OK` em caso de exclusão; `403 Forbidden` se o usuário não tiver permissão; `404 Not Found` se io tweet não existir.

## Como funciona a segurança

1. **Criptografia de senha** – Senhas são armazenadas usando `BCryptPasswordEncoder` configurado como *bean* em `SecurityConfig`. A classe `User` contém um método `isLoginCorrect` que compara a senha informada com o hash armazenado.
2. **Geração de token** – `TokenController` verifica o usuário e gera um JWT com as *authorities* do usuário em um *scope* (ex.: `ADMIN` ou `BASIC`) e validade de 300 segundos. O token é assinado pela chave privada e decodificado pela chave pública via Nimbus.
3. **Configuração de filtros** – `SecurityConfig` permite acesso público ao login e ao cadastro de usuários, desabilita CSRF, define política de sessão *stateless* e exige autenticação nos demais endpoints. A anotação `@PreAuthorize` nos métodos controla o acesso por autoridade, como em `UserController.listUsers` que exige `hasAuthority('SCOPE_ADMIN')`.
4. **Criação de perfis** – O script `data.sql` insere os perfis `ADMIN` e `BASIC` se não existirem. O método `AdminUserConfig.run` garante que sempre exista um usuário administrador com a senha definida na inicialização.
