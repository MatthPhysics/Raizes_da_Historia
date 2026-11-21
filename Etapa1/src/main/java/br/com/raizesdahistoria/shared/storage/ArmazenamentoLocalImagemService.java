package br.com.raizesdahistoria.shared.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Grava imagens no disco local (padrão: {@code uploads/plantas}) e as serve em {@code /imagens/plantas/**}. */
@Service
public class ArmazenamentoLocalImagemService implements ArmazenamentoImagemService, WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoLocalImagemService.class);

    private final Path diretorio;
    private final String urlBase;

    public ArmazenamentoLocalImagemService(@Value("${raizes.imagens.diretorio}") String diretorio,
            @Value("${raizes.imagens.url-base}") String urlBase) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar o diretório de imagens " + this.diretorio, e);
        }
    }

    @Override
    public String salvar(byte[] conteudo) {
        TipoImagem tipo = TipoImagem.detectar(conteudo).orElseThrow(() ->
                new ImagemInvalidaException("Formato não suportado. Envie uma imagem JPEG, PNG ou WEBP."));
        String nome = UUID.randomUUID() + "." + tipo.extensao();
        try {
            Files.write(diretorio.resolve(nome), conteudo);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar imagem", e);
        }
        return urlBase + "/" + nome;
    }

    @Override
    public void remover(String url) {
        if (url == null || !url.startsWith(urlBase + "/")) {
            return;
        }
        Path arquivo = diretorio.resolve(url.substring(urlBase.length() + 1)).normalize();
        if (!arquivo.startsWith(diretorio)) {
            return; // proteção contra path traversal
        }
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException e) {
            log.warn("Não foi possível remover a imagem {}", arquivo.getFileName(), e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(urlBase + "/**")
                .addResourceLocations(diretorio.toUri().toString())
                .setCachePeriod(86_400);
    }
}
