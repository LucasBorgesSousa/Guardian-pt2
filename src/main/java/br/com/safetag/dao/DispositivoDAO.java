package br.com.safetag.dao;

import br.com.safetag.config.ConnectionFactory;
import br.com.safetag.model.Dispositivo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** SQL fica exclusivamente nesta camada. Valores são enviados como parâmetros. */
public class DispositivoDAO {
    private final ConnectionFactory conexoes;
    public DispositivoDAO(ConnectionFactory conexoes) { this.conexoes = conexoes; }

    public List<Dispositivo> listar() throws SQLException {
        List<Dispositivo> lista = new ArrayList<>();
        try (Connection c = conexoes.abrir();
             PreparedStatement p = c.prepareStatement("SELECT id,nome,modelo,codigo_tag,ativo FROM dispositivos ORDER BY id");
             ResultSet r = p.executeQuery()) {
            while (r.next()) lista.add(mapear(r));
        }
        return lista;
    }

    public Dispositivo buscar(long id) throws SQLException {
        try (Connection c = conexoes.abrir();
             PreparedStatement p = c.prepareStatement("SELECT id,nome,modelo,codigo_tag,ativo FROM dispositivos WHERE id=?")) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) { return r.next() ? mapear(r) : null; }
        }
    }

    public Dispositivo inserir(Dispositivo d) throws SQLException {
        try (Connection c = conexoes.abrir();
             PreparedStatement p = c.prepareStatement(
                     "INSERT INTO dispositivos(nome,modelo,codigo_tag,ativo) VALUES(?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            preencher(p, d);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) {
                if (!r.next()) throw new SQLException("Banco não retornou o identificador.");
                d.setId(r.getLong(1));
            }
            return d;
        }
    }

    public boolean atualizar(long id, Dispositivo d) throws SQLException {
        try (Connection c = conexoes.abrir();
             PreparedStatement p = c.prepareStatement(
                     "UPDATE dispositivos SET nome=?,modelo=?,codigo_tag=?,ativo=? WHERE id=?")) {
            preencher(p, d);
            p.setLong(5, id);
            return p.executeUpdate() > 0;
        }
    }

    public boolean excluir(long id) throws SQLException {
        try (Connection c = conexoes.abrir();
             PreparedStatement p = c.prepareStatement("DELETE FROM dispositivos WHERE id=?")) {
            p.setLong(1, id);
            return p.executeUpdate() > 0;
        }
    }

    private void preencher(PreparedStatement p, Dispositivo d) throws SQLException {
        p.setString(1, d.getNome()); p.setString(2, d.getModelo());
        p.setString(3, d.getCodigoTag()); p.setBoolean(4, d.getAtivo());
    }

    private Dispositivo mapear(ResultSet r) throws SQLException {
        Dispositivo d = new Dispositivo();
        d.setId(r.getLong("id")); d.setNome(r.getString("nome"));
        d.setModelo(r.getString("modelo")); d.setCodigoTag(r.getString("codigo_tag"));
        d.setAtivo(r.getBoolean("ativo"));
        return d;
    }
}
