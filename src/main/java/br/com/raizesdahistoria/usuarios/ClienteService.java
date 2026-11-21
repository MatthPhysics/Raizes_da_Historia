package br.com.raizesdahistoria.usuarios;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdahistoria.shared.util.Mascaramento;
import br.com.raizesdahistoria.usuarios.internal.dominio.Cliente;
import br.com.raizesdahistoria.usuarios.internal.dominio.ClienteRepository;

/** API pública do módulo para clientes (checkout rápido: sempre cria um novo registro, sem deduplicar por e-mail). */
@Service
public class ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository repository;
    private final Clock clock;

    public ClienteService(ClienteRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ClienteDto criar(NovoCliente dados) {
        String cpf = dados.cpf() == null || dados.cpf().isBlank() ? null : dados.cpf().trim();
        var cliente = repository.save(new Cliente(
                dados.nome().trim(),
                dados.email().trim().toLowerCase(),
                dados.telefoneWhatsapp().trim(),
                cpf,
                Instant.now(clock)));
        log.debug("Cliente {} criado (e-mail {})", cliente.getId(), Mascaramento.email(cliente.getEmail()));
        return toDto(cliente);
    }

    @Transactional(readOnly = true)
    public Optional<ClienteDto> buscar(UUID id) {
        return repository.findById(id).map(ClienteService::toDto);
    }

    private static ClienteDto toDto(Cliente c) {
        return new ClienteDto(c.getId(), c.getNome(), c.getEmail(), c.getTelefoneWhatsapp(), c.getCpf(), c.getCriadoEm());
    }
}
