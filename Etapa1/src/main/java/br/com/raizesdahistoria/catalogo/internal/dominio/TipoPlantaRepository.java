package br.com.raizesdahistoria.catalogo.internal.dominio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoPlantaRepository extends JpaRepository<TipoPlanta, Integer> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Integer id);
}
