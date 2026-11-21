package br.com.raizesdahistoria.catalogo.internal.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.raizesdahistoria.catalogo.internal.dominio.ExposicaoSol;
import br.com.raizesdahistoria.catalogo.internal.dominio.FrequenciaRega;
import br.com.raizesdahistoria.catalogo.internal.dominio.Planta;
import br.com.raizesdahistoria.catalogo.internal.dominio.TipoPlanta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs (records) do catálogo. Entidades nunca são retornadas pelos controllers. */
final class CatalogoDtos {

    private CatalogoDtos() {
    }

    record TipoPlantaResponse(Integer id, String nome, String descricao) {
        static TipoPlantaResponse de(TipoPlanta t) {
            return new TipoPlantaResponse(t.getId(), t.getNome(), t.getDescricao());
        }
    }

    record TipoResumo(Integer id, String nome) {
    }

    /** Visão pública: inclui {@code disponivel}; plantas esgotadas continuam no catálogo. */
    record PlantaResponse(UUID id, String nomePopular, String nomeCientifico, BigDecimal preco,
            FrequenciaRega frequenciaRega, ExposicaoSol exposicaoSol, int quantidadeDisponivel, boolean disponivel,
            String urlImagem, TipoResumo tipo) {
        static PlantaResponse de(Planta p) {
            return new PlantaResponse(p.getId(), p.getNomePopular(), p.getNomeCientifico(), p.getPreco(),
                    p.getFrequenciaRega(), p.getExposicaoSol(), p.getQuantidadeProntaVenda(), p.isDisponivel(),
                    p.getUrlImagem(), new TipoResumo(p.getTipo().getId(), p.getTipo().getNome()));
        }
    }

    /** Visão administrativa: inclui {@code ativo}. */
    record PlantaAdminResponse(UUID id, String nomePopular, String nomeCientifico, BigDecimal preco,
            FrequenciaRega frequenciaRega, ExposicaoSol exposicaoSol, int quantidadeProntaVenda, boolean ativo,
            String urlImagem, TipoResumo tipo) {
        static PlantaAdminResponse de(Planta p) {
            return new PlantaAdminResponse(p.getId(), p.getNomePopular(), p.getNomeCientifico(), p.getPreco(),
                    p.getFrequenciaRega(), p.getExposicaoSol(), p.getQuantidadeProntaVenda(), p.isAtivo(),
                    p.getUrlImagem(), new TipoResumo(p.getTipo().getId(), p.getTipo().getNome()));
        }
    }

    record TipoPlantaRequest(
            @NotBlank(message = "O nome é obrigatório.")
            @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
            String nome,
            String descricao) {
    }

    record PlantaRequest(
            @NotNull(message = "O tipo é obrigatório.")
            Integer tipoId,
            @NotBlank(message = "O nome popular é obrigatório.")
            @Size(max = 120, message = "O nome popular deve ter no máximo 120 caracteres.")
            @Schema(example = "Jibóia")
            String nomePopular,
            @Size(max = 160, message = "O nome científico deve ter no máximo 160 caracteres.")
            @Schema(example = "Epipremnum aureum")
            String nomeCientifico,
            @NotNull(message = "O preço é obrigatório.")
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
            @Digits(integer = 8, fraction = 2, message = "Preço inválido (máximo 2 casas decimais).")
            @Schema(example = "39.90")
            BigDecimal preco,
            @NotNull(message = "A frequência de rega é obrigatória.")
            FrequenciaRega frequenciaRega,
            @NotNull(message = "A exposição ao sol é obrigatória.")
            ExposicaoSol exposicaoSol,
            @Min(value = 0, message = "A quantidade não pode ser negativa.")
            @Schema(description = "Usada apenas na criação; para ajustar depois use PATCH /estoque.", example = "10")
            Integer quantidadeProntaVenda) {
    }

    record AtivoRequest(@NotNull(message = "Informe se a planta está ativa.") Boolean ativo) {
    }

    record EstoqueRequest(
            @NotNull(message = "A quantidade é obrigatória.")
            @Min(value = 0, message = "A quantidade não pode ser negativa.")
            Integer quantidadeProntaVenda) {
    }
}
