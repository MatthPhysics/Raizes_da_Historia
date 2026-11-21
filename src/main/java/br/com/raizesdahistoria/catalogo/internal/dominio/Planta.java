package br.com.raizesdahistoria.catalogo.internal.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "plantas")
public class Planta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoPlanta tipo;

    @Column(name = "nome_popular", nullable = false, length = 120)
    private String nomePopular;

    @Column(name = "nome_cientifico", length = 160)
    private String nomeCientifico;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequencia_rega", nullable = false, length = 20)
    private FrequenciaRega frequenciaRega;

    @Enumerated(EnumType.STRING)
    @Column(name = "exposicao_sol", nullable = false, length = 20)
    private ExposicaoSol exposicaoSol;

    @Column(name = "quantidade_pronta_venda", nullable = false)
    private int quantidadeProntaVenda;

    @Column(name = "url_imagem", length = 500)
    private String urlImagem;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Planta() {
    }

    public Planta(TipoPlanta tipo, String nomePopular, String nomeCientifico, BigDecimal preco,
            FrequenciaRega frequenciaRega, ExposicaoSol exposicaoSol, int quantidadeProntaVenda) {
        atualizar(tipo, nomePopular, nomeCientifico, preco, frequenciaRega, exposicaoSol);
        ajustarEstoque(quantidadeProntaVenda);
    }

    public void atualizar(TipoPlanta tipo, String nomePopular, String nomeCientifico, BigDecimal preco,
            FrequenciaRega frequenciaRega, ExposicaoSol exposicaoSol) {
        this.tipo = tipo;
        this.nomePopular = nomePopular;
        this.nomeCientifico = nomeCientifico;
        this.preco = preco.setScale(2, RoundingMode.HALF_EVEN);
        this.frequenciaRega = frequenciaRega;
        this.exposicaoSol = exposicaoSol;
    }

    public void ajustarEstoque(int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("Estoque não pode ser negativo");
        }
        this.quantidadeProntaVenda = quantidade;
    }

    /** Reserva unidades (checkout). Deve ser chamada com a linha bloqueada (lock pessimista). */
    public void reservar(int quantidade) {
        if (quantidade <= 0 || quantidade > quantidadeProntaVenda) {
            throw new IllegalStateException("Estoque insuficiente para a planta " + id);
        }
        this.quantidadeProntaVenda -= quantidade;
    }

    public void devolver(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade a devolver deve ser positiva");
        }
        this.quantidadeProntaVenda += quantidade;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public void definirUrlImagem(String urlImagem) {
        this.urlImagem = urlImagem;
    }

    public boolean isDisponivel() {
        return ativo && quantidadeProntaVenda > 0;
    }

    public UUID getId() {
        return id;
    }

    public TipoPlanta getTipo() {
        return tipo;
    }

    public String getNomePopular() {
        return nomePopular;
    }

    public String getNomeCientifico() {
        return nomeCientifico;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public FrequenciaRega getFrequenciaRega() {
        return frequenciaRega;
    }

    public ExposicaoSol getExposicaoSol() {
        return exposicaoSol;
    }

    public int getQuantidadeProntaVenda() {
        return quantidadeProntaVenda;
    }

    public String getUrlImagem() {
        return urlImagem;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
