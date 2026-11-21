package br.com.raizesdahistoria.usuarios.internal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import br.com.raizesdahistoria.shared.util.Mascaramento;
import br.com.raizesdahistoria.usuarios.AdministradorService;

/**
 * Cria o primeiro administrador quando a tabela está vazia, a partir de
 * {@code ADMIN_INICIAL_NOME}, {@code ADMIN_INICIAL_EMAIL} e {@code ADMIN_INICIAL_SENHA}.
 * A senha nunca fica versionada (por isso não está em migration).
 */
@Component
class AdminInicialSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialSeeder.class);
    static final int SENHA_MINIMA = 8;

    private final AdministradorService administradorService;
    private final AdminInicialProperties props;

    AdminInicialSeeder(AdministradorService administradorService, AdminInicialProperties props) {
        this.administradorService = administradorService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (administradorService.existeAlgum()) {
            return;
        }
        if (isBlank(props.nome()) || isBlank(props.email()) || isBlank(props.senha())) {
            log.warn("Nenhum administrador cadastrado e ADMIN_INICIAL_NOME/EMAIL/SENHA não definidos: "
                    + "não será possível acessar a área administrativa.");
            return;
        }
        if (props.senha().length() < SENHA_MINIMA) {
            throw new IllegalStateException("ADMIN_INICIAL_SENHA deve ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        administradorService.cadastrar(props.nome(), props.email(), props.senha());
        log.info("Administrador inicial criado: {}", Mascaramento.email(props.email()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @ConfigurationProperties("raizes.admin-inicial")
    record AdminInicialProperties(String nome, String email, String senha) {
    }
}
