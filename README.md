
# Guardian-pt2 — SafeTag API
## Servlets, Tomcat e JDBC

API REST Java para gerenciar celulares vinculados a tags. Implementa o CRUD de dispositivos do tema SafeTag. O aplicativo Android em ../ENTEC usa esses endpoints; seu login ainda é demonstrativo.

## Tecnologias
Java 17; Servlet 4.0 (javax.servlet); Tomcat 9.0.123; JDBC/MySQL Connector/J 8.4.0; MySQL 8.4; Gson 2.13.2 para JSON; Maven para compilar.
Não utiliza Spring, Hibernate ou Kotlin. Tomcat 10/11 usa jakarta.servlet e não é compatível diretamente com este WAR.
Referências: [Tomcat 9](https://tomcat.apache.org/tomcat-9.0-doc/) e [Connector/J via Maven](https://dev.mysql.com/doc/connector-j/en/connector-j-installing-maven.html).

## Organização para explicar em sala
Fluxo: requisição HTTP → DispositivoController → DispositivoService → DispositivoDAO → MySQL.
A resposta volta pelo mesmo caminho e o Controller a transforma em JSON.

- model/Dispositivo.java: dados da entidade, atributos, getters e setters.
- controller/DispositivoController.java: rota, método HTTP, leitura e resposta JSON.
- service/DispositivoService.java: campos obrigatórios, tamanhos, normalização da tag e conflitos.
- dao/DispositivoDAO.java: comandos SQL parametrizados, leitura de ResultSet, fechamento de recursos.
- config/ConnectionFactory.java: fábrica Singleton de conexões JDBC com configuração externa.
- src/main/webapp/WEB-INF/web.xml: registra o Servlet e sua rota.
- database/schema.sql: banco e tabela, chave primária e código de tag único.
- src/test/java: testes de regras e testes HTTP com Tomcat e MySQL reais.

O Model representa os dados. A representação retornada pela API é JSON; a interface visual é o cliente Android separado.
PreparedStatement evita interpretar valores recebidos como comandos SQL. try-with-resources fecha conexão, comando e resultados.
A senha do banco fica em variável de ambiente, nunca no Java. O usuário da API precisa somente de SELECT, INSERT, UPDATE e DELETE.

## Singleton no acesso ao banco
ConnectionFactory tem construtor privado, uma instância static final e o método público getInstance(). O Controller entrega essa mesma fábrica ao DAO. A inicialização estática da JVM permite que requisições simultâneas usem a mesma instância com segurança.

Exemplo: `ConnectionFactory.getInstance().abrir()`.

O Singleton é a fábrica, não uma Connection JDBC compartilhada. Cada chamada de abrir() cria uma conexão independente, fechada pelo DAO ao terminar. Assim, fechar a conexão de uma requisição não interrompe outra. Isso também evita compartilhar o estado de transações entre requisições. Não é um pool de conexões.

Para explicar em sala: “A fábrica tem uma única instância por carregamento da aplicação. O construtor privado impede criar outras instâncias com new, e getInstance() fornece o ponto de acesso. As conexões JDBC têm duração de uma operação.”

## Executar neste notebook
Na pasta outputs, execute Iniciar-API-SafeTag.bat pelo Explorador de Arquivos. Mantenha a janela aberta.
O iniciador utiliza os servidores locais em work e a configuração privada em .local, na pasta Patrick.
Endereço: http://localhost:8081/safetag-api/api/dispositivos
A lista vazia aparece como []. A API não tem página com botões; use a coleção Postman.
O MySQL está na porta 3307, separado de possíveis instalações na porta 3306.
O iniciador só funciona nesta estrutura de pastas. O ZIP do código não inclui servidores, dados ou senhas.

## Instalar em outro computador
1. Instale JDK 17, Maven, MySQL 8.4 e Tomcat 9.
2. Execute database/schema.sql no MySQL com uma conta administrativa.
3. Crie um usuário específico e conceda SELECT, INSERT, UPDATE e DELETE em safetag.*. Escolha uma senha local.
4. Copie configurar-exemplo.ps1 para .local/configurar.ps1 e preencha conexão, usuário e senha. Crie a pasta .local se necessário.
5. No PowerShell, execute a configuração na sessão atual:
   `. ./.local/configurar.ps1`
6. Compile com `mvn clean package`. Copie target/safetag-api.war para a pasta webapps do Tomcat.
7. Na mesma sessão configurada, execute o bin/catalina.bat run do Tomcat.
8. Use http://localhost:8080/safetag-api/api/dispositivos (ou a porta escolhida).
A URL JDBC deve manter useAffectedRows=false para que um PUT com os mesmos dados continue retornando sucesso.

## Endpoints
Base neste notebook: http://localhost:8081/safetag-api

| Método | Caminho | Resultado |
|---|---|---|
| POST | /api/dispositivos | Cadastra; 201 e cabeçalho Location |
| GET | /api/dispositivos | Lista; 200 e array JSON |
| GET | /api/dispositivos/{id} | Consulta; 200 |
| PUT | /api/dispositivos/{id} | Substitui os quatro campos; 200 |
| DELETE | /api/dispositivos/{id} | Exclui; 204 sem corpo |

POST e PUT exigem Content-Type: application/json e todos os campos:
```json
{
  "nome": "Celular de demonstração",
  "modelo": "Galaxy S23",
  "codigoTag": "TAG-0042",
  "ativo": true
}
```
id é gerado pelo banco e não deve ser enviado. nome: 3–80 caracteres; modelo: 2–80; codigoTag: 3–40, letras ASCII, números e hífen. Espaços nas extremidades são removidos; tag é convertida para maiúsculas. ativo deve ser booleano JSON.
O código da tag não pode pertencer a dois dispositivos, mesmo em cadastros simultâneos.

Erros: 400 dados/JSON/id inválidos; 404 dispositivo inexistente; 405 método incorreto; 409 tag repetida; 413 corpo acima de 16.384 caracteres; 415 formato diferente de JSON; 503 falha de acesso ao banco.
Formato: `{"erro":"Mensagem explicativa."}`. Detalhes SQL ficam no registro do servidor.
Não existe autenticação nesta etapa; o ambiente local escuta apenas 127.0.0.1 e não deve ser publicado como serviço público.

## Testar
Importe SafeTag.postman_collection.json no Postman e execute as requisições em ordem. O cadastro salva o id automaticamente na coleção.
A requisição final exclui somente o dispositivo criado nessa execução.

`mvn test` executa cinco testes unitários e pula os testes MySQL se a configuração de teste estiver ausente.
Para todos os onze testes, crie safetag_test usando o mesmo schema, configure SAFETAG_DB_URL apontando explicitamente para /safetag_test e configure usuário/senha; execute `mvn test`.
Os testes criam e apagam seus próprios registros. Não use um banco com dados importantes.
O Tomcat de teste abre uma porta local temporária e fecha ao terminar.

## Situação da entrega
Implementados: Java, Controller/Service/DAO, CRUD de dispositivos, persistência MySQL e documentação.
Validados em 13/11/2026: onze testes aprovados, incluindo Tomcat + JDBC + MySQL reais e a independência das conexões fornecidas pelo Singleton. Veja VALIDACAO.md.
Android conectado ao CRUD de dispositivos: projeto ../ENTEC. A autenticação e os recursos físicos de proteção ainda não estão implementados.
Em andamento: publicação no repositório GitHub.
Pendentes: envio do link no AVA e preparação/apresentação pelos integrantes.

