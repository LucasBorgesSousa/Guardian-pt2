package br.com.safetag.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Fábrica Singleton. Cada operação recebe sua própria conexão JDBC. */
public final class ConnectionFactory {
    // A JVM inicializa esta instância uma única vez por carregamento da aplicação.
    private static final ConnectionFactory INSTANCE = new ConnectionFactory();

    private ConnectionFactory() { }

    public static ConnectionFactory getInstance() {
        return INSTANCE;
    }

    /** O chamador deve fechar a conexão com try-with-resources. */
    public Connection abrir() throws SQLException {
        String url = System.getenv("SAFETAG_DB_URL");
        String usuario = System.getenv("SAFETAG_DB_USER");
        String senha = System.getenv("SAFETAG_DB_PASSWORD");
        if (url == null || usuario == null || senha == null) {
            throw new SQLException("Defina SAFETAG_DB_URL, SAFETAG_DB_USER e SAFETAG_DB_PASSWORD.");
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não disponível.", e);
        }
        return DriverManager.getConnection(url, usuario, senha);
    }
}
