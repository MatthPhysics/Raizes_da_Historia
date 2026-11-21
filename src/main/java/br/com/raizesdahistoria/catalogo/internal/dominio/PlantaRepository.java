package br.com.raizesdahistoria.catalogo.internal.dominio;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlantaRepository extends JpaRepository<Planta, UUID>, JpaSpecificationExecutor<Planta> {

    /** Evita N+1 ao serializar o tipo de cada planta da página. */
    @Override
    @EntityGraph(attributePaths = "tipo")
    Page<Planta> findAll(Specification<Planta> spec, Pageable pageable);

    @EntityGraph(attributePaths = "tipo")
    Optional<Planta> findWithTipoById(UUID id);

    @EntityGraph(attributePaths = "tipo")
    Optional<Planta> findWithTipoByIdAndAtivoTrue(UUID id);

    List<Planta> findByIdIn(Collection<UUID> ids);

    /**
     * {@code SELECT ... FOR UPDATE} ordenado por id: todos os checkouts bloqueiam as linhas na mesma ordem,
     * o que evita deadlock entre transações concorrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Planta p where p.id in :ids order by p.id")
    List<Planta> bloquearPorIds(@Param("ids") Collection<UUID> ids);

    boolean existsByTipoId(Integer tipoId);
}
