package br.com.safetag;

import br.com.safetag.model.Dispositivo;
import br.com.safetag.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DispositivoServiceTest {
    private final DispositivoService service = new DispositivoService(null);
    private Dispositivo exemplo() {
        Dispositivo d = new Dispositivo();
        d.setNome(" Celular de teste "); d.setModelo(" Galaxy S23 ");
        d.setCodigoTag(" tag-001 "); d.setAtivo(true);
        return d;
    }
    @Test void normalizaCampos() {
        Dispositivo d = exemplo(); service.validar(d);
        assertEquals("Celular de teste", d.getNome());
        assertEquals("Galaxy S23", d.getModelo()); assertEquals("TAG-001", d.getCodigoTag());
    }
    @Test void rejeitaNomeCurto() {
        Dispositivo d = exemplo(); d.setNome("  a ");
        assertEquals(400, assertThrows(RegraException.class, () -> service.validar(d)).getStatus());
    }
    @Test void rejeitaModeloLongo() {
        Dispositivo d = exemplo(); d.setModelo("X".repeat(81));
        assertThrows(RegraException.class, () -> service.validar(d));
    }
    @Test void rejeitaTagInvalida() {
        Dispositivo d = exemplo(); d.setCodigoTag("TAG / 01");
        assertThrows(RegraException.class, () -> service.validar(d));
    }
    @Test void rejeitaAtivoAusente() {
        Dispositivo d = exemplo(); d.setAtivo(null);
        assertThrows(RegraException.class, () -> service.validar(d));
    }
}
