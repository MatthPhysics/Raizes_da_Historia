package br.com.raizesdahistoria.shared.util;

/** Mascaramento de dados pessoais para logs (LGPD). Nunca logar CPF, e-mail ou telefone completos. */
public final class Mascaramento {

    private Mascaramento() {
    }

    /** {@code 12345678912} → {@code ***.***.***-12}. */
    public static String cpf(String cpf) {
        if (cpf == null || cpf.length() < 2) {
            return "***";
        }
        return "***.***.***-" + cpf.substring(cpf.length() - 2);
    }

    /** {@code maria@email.com} → {@code m***@email.com}. */
    public static String email(String email) {
        if (email == null) {
            return "***";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }

    /** {@code 11999998888} → {@code *******8888}. */
    public static String telefone(String telefone) {
        if (telefone == null || telefone.length() < 4) {
            return "***";
        }
        return "*".repeat(telefone.length() - 4) + telefone.substring(telefone.length() - 4);
    }
}
