package br.com.safetag.service;

public class RegraException extends RuntimeException {
    private final int status;
    public RegraException(int status, String mensagem) { super(mensagem); this.status = status; }
    public int getStatus() { return status; }
}
