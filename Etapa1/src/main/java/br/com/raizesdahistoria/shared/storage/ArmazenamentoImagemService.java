package br.com.raizesdahistoria.shared.storage;

/**
 * Armazenamento de imagens independente de provedor. A implementação inicial grava em disco local;
 * um provedor de nuvem pode substituí-la quando a hospedagem for definida.
 */
public interface ArmazenamentoImagemService {

    /**
     * Valida o conteúdo (magic bytes: JPEG, PNG ou WEBP), grava com nome gerado pelo servidor e
     * devolve a URL pública da imagem.
     *
     * @throws ImagemInvalidaException se o conteúdo não for de um tipo aceito
     */
    String salvar(byte[] conteudo);

    /** Remove a imagem a partir da URL devolvida por {@link #salvar(byte[])}. Ignora URLs desconhecidas. */
    void remover(String url);
}
