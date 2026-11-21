package br.com.raizesdahistoria.usuarios.internal.dominio;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "telefone_whatsapp", nullable = false, length = 20)
    private String telefoneWhatsapp;

    @Column(length = 11)
    private String cpf;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Cliente() {
    }

    public Cliente(String nome, String email, String telefoneWhatsapp, String cpf, Instant criadoEm) {
        this.nome = nome;
        this.email = email;
        this.telefoneWhatsapp = telefoneWhatsapp;
        this.cpf = cpf;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefoneWhatsapp() {
        return telefoneWhatsapp;
    }

    public String getCpf() {
        return cpf;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
