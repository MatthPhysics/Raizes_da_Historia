package br.com.raizesdahistoria;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(RaizesDaHistoriaApplication.class);

    @Test
    void verificaFronteirasEntreModulos() {
        modules.verify();
    }

    @Test
    void geraDocumentacaoDosModulos() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
