package br.com.safetag.service;

import br.com.safetag.dao.DispositivoDAO;
import br.com.safetag.model.Dispositivo;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public class DispositivoService {
    private final DispositivoDAO dao;
    public DispositivoService(DispositivoDAO dao) { this.dao = dao; }

    public List<Dispositivo> listar() throws SQLException { return dao.listar(); }
    public Dispositivo buscar(long id) throws SQLException {
        Dispositivo d = dao.buscar(id);
        if (d == null) throw new RegraException(404, "Dispositivo não encontrado.");
        return d;
    }
    public Dispositivo cadastrar(Dispositivo d) throws SQLException {
        validar(d);
        try { return dao.inserir(d); }
        catch (SQLException e) { throw traduzir(e); }
    }
    public Dispositivo atualizar(long id, Dispositivo d) throws SQLException {
        validar(d);
        try {
            if (!dao.atualizar(id, d)) throw new RegraException(404, "Dispositivo não encontrado.");
            d.setId(id);
            return d;
        } catch (SQLException e) { throw traduzir(e); }
    }
    public void excluir(long id) throws SQLException {
        if (!dao.excluir(id)) throw new RegraException(404, "Dispositivo não encontrado.");
    }
    public void validar(Dispositivo d) {
        if (d == null) throw new RegraException(400, "Informe os dados do dispositivo.");
        d.setNome(texto(d.getNome(), "nome", 3, 80));
        d.setModelo(texto(d.getModelo(), "modelo", 2, 80));
        String tag = texto(d.getCodigoTag(), "codigoTag", 3, 40).toUpperCase(Locale.ROOT);
        if (!tag.matches("[A-Z0-9-]+")) throw new RegraException(400, "codigoTag aceita letras, números e hífen.");
        d.setCodigoTag(tag);
        if (d.getAtivo() == null) throw new RegraException(400, "ativo deve ser true ou false.");
    }
    private String texto(String valor, String campo, int minimo, int maximo) {
        if (valor == null || valor.trim().length() < minimo || valor.trim().length() > maximo)
            throw new RegraException(400, campo + " deve ter entre " + minimo + " e " + maximo + " caracteres.");
        return valor.trim();
    }
    private SQLException traduzir(SQLException e) {
        // A restrição UNIQUE também protege contra cadastros simultâneos da mesma tag.
        if (e.getErrorCode() == 1062) throw new RegraException(409, "Esta tag já está vinculada a um dispositivo.");
        return e;
    }
}
