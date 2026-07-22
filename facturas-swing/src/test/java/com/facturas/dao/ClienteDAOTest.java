package com.facturas.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteDAOTest {
    private final ClienteDAO dao = new ClienteDAO();

    @Test
    void nombreFiscalDebeCoincidirCompletoYNoPorUnaPalabraComun() {
        assertFalse(dao.coincide("farmacia", "Farmacia Central"));
        assertFalse(dao.coincide("farmacia", "Farmacia del Parque"));
    }

    @Test
    void nombreFiscalSigueAdmitiendoDiferenciasDeFormato() {
        assertTrue(dao.coincide("farmacia central", "FARMACIA  CENTRAL"));
        assertTrue(dao.coincide("farmacia central", "Farmacia del Central"));
    }
}
