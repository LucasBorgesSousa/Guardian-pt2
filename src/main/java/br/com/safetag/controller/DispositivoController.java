package br.com.safetag.controller;

import br.com.safetag.config.ConnectionFactory;
import br.com.safetag.dao.DispositivoDAO;
import br.com.safetag.model.Dispositivo;
import br.com.safetag.service.*;
import com.google.gson.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.*;

/** Recebe HTTP, chama o Service e devolve JSON. Não contém SQL. */
public class DispositivoController extends HttpServlet {
    private final Gson gson = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private final DispositivoService service = new DispositivoService(new DispositivoDAO(ConnectionFactory.getInstance()));

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
        try {
            Long id = idDaRota(req.getPathInfo());
            String metodo = req.getMethod();
            if ("OPTIONS".equals(metodo)) {
                resp.setHeader("Allow", "GET, POST, PUT, DELETE, OPTIONS");
                resp.setStatus(204);
            } else if ("GET".equals(metodo)) {
                responder(resp, 200, id == null ? service.listar() : service.buscar(id));
            } else if ("POST".equals(metodo) && id == null) {
                Dispositivo criado = service.cadastrar(lerCorpo(req));
                resp.setHeader("Location", req.getContextPath() + "/api/dispositivos/" + criado.getId());
                responder(resp, 201, criado);
            } else if ("PUT".equals(metodo) && id != null) {
                responder(resp, 200, service.atualizar(id, lerCorpo(req)));
            } else if ("DELETE".equals(metodo) && id != null) {
                service.excluir(id);
                resp.setStatus(204);
            } else {
                resp.setHeader("Allow", id == null ? "GET, POST, OPTIONS" : "GET, PUT, DELETE, OPTIONS");
                throw new RegraException(405, "Método não permitido nesta rota.");
            }
        } catch (RegraException e) {
            responder(resp, e.getStatus(), Map.of("erro", e.getMessage()));
        } catch (JsonParseException | IllegalStateException e) {
            responder(resp, 400, Map.of("erro", "JSON inválido ou tipos de campos incorretos."));
        } catch (SQLException e) {
            getServletContext().log("Falha no banco de dados.", e);
            responder(resp, 503, Map.of("erro", "Banco de dados indisponível. Tente novamente."));
        }
    }

    private Long idDaRota(String caminho) {
        if (caminho == null || caminho.equals("/")) return null;
        if (!caminho.matches("/[1-9][0-9]*")) throw new RegraException(400, "Informe um identificador numérico positivo.");
        try { return Long.valueOf(caminho.substring(1)); }
        catch (NumberFormatException e) { throw new RegraException(400, "Identificador fora do limite."); }
    }

    private Dispositivo lerCorpo(HttpServletRequest req) throws IOException {
        String tipo = req.getContentType();
        if (tipo == null || !tipo.split(";")[0].trim().equalsIgnoreCase("application/json"))
            throw new RegraException(415, "Use Content-Type: application/json.");
        StringBuilder corpo = new StringBuilder();
        char[] buffer = new char[1024];
        int lidos;
        Reader reader = req.getReader();
        while ((lidos = reader.read(buffer)) != -1) {
            corpo.append(buffer, 0, lidos);
            if (corpo.length() > 16384) throw new RegraException(413, "Corpo da requisição muito grande.");
        }
        JsonObject json = gson.fromJson(corpo.toString(), JsonObject.class);
        if (json == null) throw new RegraException(400, "Informe um objeto JSON.");
        Set<String> permitidos = Set.of("nome", "modelo", "codigoTag", "ativo");
        for (String campo : json.keySet())
            if (!permitidos.contains(campo)) throw new RegraException(400, "Campo desconhecido: " + campo);
        for (String campo : List.of("nome", "modelo", "codigoTag")) {
            JsonElement valor = json.get(campo);
            if (valor == null || !valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isString())
                throw new RegraException(400, campo + " deve ser texto.");
        }
        JsonElement ativo = json.get("ativo");
        if (ativo == null || !ativo.isJsonPrimitive() || !ativo.getAsJsonPrimitive().isBoolean())
            throw new RegraException(400, "ativo deve ser true ou false.");
        return gson.fromJson(json, Dispositivo.class);
    }

    private void responder(HttpServletResponse resp, int status, Object dados) throws IOException {
        resp.setStatus(status);
        gson.toJson(dados, resp.getWriter());
    }
}
