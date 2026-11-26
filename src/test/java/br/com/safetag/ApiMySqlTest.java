package br.com.safetag;

import br.com.safetag.controller.DispositivoController;
import com.google.gson.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Teste HTTP real no Tomcat com JDBC/MySQL real. Só roda no banco safetag_test. */
@EnabledIfEnvironmentVariable(named="SAFETAG_DB_URL", matches=".*[/]safetag_test([?].*)?")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiMySqlTest {
    @Test void singletonMantemConexoesIndependentes() throws Exception {
        br.com.safetag.config.ConnectionFactory fabrica = br.com.safetag.config.ConnectionFactory.getInstance();
        assertSame(fabrica, br.com.safetag.config.ConnectionFactory.getInstance());
        try (java.sql.Connection segunda = fabrica.abrir()) {
            try (java.sql.Connection primeira = fabrica.abrir()) {
                assertNotSame(primeira, segunda);
                assertTrue(primeira.isValid(2));
            }
            // Encerrar uma operação não pode invalidar a conexão de outra.
            assertTrue(segunda.isValid(2));
            try (java.sql.Statement comando = segunda.createStatement();
                 java.sql.ResultSet resultado = comando.executeQuery("SELECT 1")) {
                assertTrue(resultado.next());
                assertEquals(1, resultado.getInt(1));
            }
        }
    }
    private Tomcat tomcat;
    private String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final List<Long> criados = new ArrayList<>();

    @BeforeAll void iniciar() throws Exception {
        tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("safetag-tomcat-").toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addContext("/safetag-api", Path.of("target").toAbsolutePath().toString());
        Tomcat.addServlet(context, "dispositivos", new DispositivoController());
        context.addServletMappingDecoded("/api/dispositivos/*", "dispositivos");
        tomcat.start();
        base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/safetag-api/api/dispositivos";
    }
    @AfterEach void limpar() throws Exception {
        for (Long id : criados) enviar("DELETE", "/" + id, null);
        criados.clear();
    }
    @AfterAll void parar() throws Exception {
        if (tomcat != null) { tomcat.stop(); tomcat.destroy(); }
    }
    private HttpResponse<String> enviar(String metodo, String rota, String json) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base + rota)).timeout(Duration.ofSeconds(15));
        if (json != null) req.header("Content-Type", "application/json");
        req.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }
    private String corpo(String tag) {
        return "{\"nome\":\"Celular de teste\",\"modelo\":\"Galaxy S23\",\"codigoTag\":\"" + tag + "\",\"ativo\":true}";
    }
    private long cadastrar(String tag) throws Exception {
        HttpResponse<String> r = enviar("POST", "", corpo(tag));
        assertEquals(201, r.statusCode(), r.body());
        long id = JsonParser.parseString(r.body()).getAsJsonObject().get("id").getAsLong();
        criados.add(id);
        assertTrue(r.headers().firstValue("Location").orElse("").endsWith("/" + id));
        return id;
    }
    @Test void crudCompletoEAtualizacaoIdentica() throws Exception {
        String tag = "T-" + UUID.randomUUID();
        long id = cadastrar(tag);
        assertEquals(200, enviar("GET", "/" + id, null).statusCode());
        JsonArray lista = JsonParser.parseString(enviar("GET", "", null).body()).getAsJsonArray();
        assertTrue(lista.asList().stream().anyMatch(e -> e.getAsJsonObject().get("id").getAsLong() == id));
        assertEquals(200, enviar("PUT", "/" + id, corpo(tag)).statusCode());
        String alterado = corpo(tag).replace("Galaxy S23", "Moto G").replace("true", "false");
        assertEquals(200, enviar("PUT", "/" + id, alterado).statusCode());
        JsonObject salvo = JsonParser.parseString(enviar("GET", "/" + id, null).body()).getAsJsonObject();
        assertEquals("Moto G", salvo.get("modelo").getAsString());
        assertFalse(salvo.get("ativo").getAsBoolean());
        HttpResponse<String> exclusao = enviar("DELETE", "/" + id, null);
        assertEquals(204, exclusao.statusCode()); assertEquals("", exclusao.body());
        assertEquals(404, enviar("GET", "/" + id, null).statusCode());
        assertEquals(404, enviar("DELETE", "/" + id, null).statusCode());
        assertEquals(404, enviar("PUT", "/" + id, corpo(tag)).statusCode());
    }
    @Test void tagDuplicadaNoCadastroEEdicao() throws Exception {
        String tag = "T-" + UUID.randomUUID();
        cadastrar(tag);
        assertEquals(409, enviar("POST", "", corpo(tag.toLowerCase())).statusCode());
        long outro = cadastrar("T-" + UUID.randomUUID());
        assertEquals(409, enviar("PUT", "/" + outro, corpo(tag)).statusCode());
    }
    @Test void rejeitaDadosInvalidos() throws Exception {
        for (String json : List.of("null", "[]", "{", "{}", "{nome:1}", corpo("TAG-1") + "{}",
                corpo("TAG-1").replace("true", "\"true\""), corpo("TAG-1").replace("Galaxy S23", ""),
                corpo("TAG-1").replace("\"nome\"", "\"campoDesconhecido\"")))
            assertEquals(400, enviar("POST", "", json).statusCode(), json);
    }
    @Test void validaRotasMetodosETipo() throws Exception {
        assertEquals(400, enviar("GET", "/abc", null).statusCode());
        assertEquals(400, enviar("GET", "/0", null).statusCode());
        assertEquals(400, enviar("GET", "/999999999999999999999999999", null).statusCode());
        assertEquals(405, enviar("POST", "/1", corpo("TAG-1")).statusCode());
        assertEquals(405, enviar("PUT", "", corpo("TAG-1")).statusCode());
        assertEquals(415, enviar("POST", "", null).statusCode());
        assertEquals(413, enviar("POST", "", " ".repeat(17000)).statusCode());
    }
    @Test void sqlComoTextoNaoExecutaComandos() throws Exception {
        String tag = "T-" + UUID.randomUUID();
        HttpResponse<String> r = enviar("POST", "", corpo(tag).replace("Galaxy S23", "X'; DROP TABLE dispositivos; --"));
        assertEquals(201, r.statusCode());
        long id = JsonParser.parseString(r.body()).getAsJsonObject().get("id").getAsLong();
        criados.add(id);
        assertEquals(200, enviar("GET", "", null).statusCode());
        assertTrue(enviar("GET", "/" + id, null).body().contains("DROP TABLE"));
    }
}
