package br.com.safetag.model;

/** Um celular associado a uma tag. Não contém credenciais do usuário. */
public class Dispositivo {
    private Long id;
    private String nome;
    private String modelo;
    private String codigoTag;
    private Boolean ativo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getCodigoTag() { return codigoTag; }
    public void setCodigoTag(String codigoTag) { this.codigoTag = codigoTag; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
