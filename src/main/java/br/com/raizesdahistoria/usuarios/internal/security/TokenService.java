package br.com.raizesdahistoria.usuarios.internal.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
class TokenService {

    static final String ISSUER = "raizes-da-historia";
    static final String CLAIM_ROLES = "roles";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration expiracao;

    TokenService(JwtEncoder encoder, Clock clock, JwtProperties props) {
        this.encoder = encoder;
        this.clock = clock;
        this.expiracao = Duration.ofHours(props.expiracaoHoras());
    }

    TokenEmitido emitirParaAdmin(UUID adminId, String email) {
        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(expiracao);
        var claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(adminId.toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("email", email)
                .claim(CLAIM_ROLES, List.of("ADMIN"))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    record TokenEmitido(String token, Instant expiraEm) {
    }
}
