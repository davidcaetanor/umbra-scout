package dev.davidcaetano.umbra_api.usuario.viacep;

import dev.davidcaetano.umbra_api.usuario.service.ConsultaDeCep;
import dev.davidcaetano.umbra_api.usuario.service.ConsultaDeCepIndisponivelException;
import dev.davidcaetano.umbra_api.usuario.service.RegiaoDoCep;
import dev.davidcaetano.umbra_api.usuario.viacep.dto.response.ViaCepResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ViaCepConsultaDeCep implements ConsultaDeCep {

    private static final String CEP_INEXISTENTE = "true";
    private static final Pattern FORMATO_UF = Pattern.compile("[A-Z]{2}");
    private static final int LIMITE_DA_CIDADE = 120;

    private final RestClient viaCepRestClient;

    @Override
    public Optional<RegiaoDoCep> consultar(String cep) {
        ViaCepResponse resposta = buscar(cep);

        if (resposta == null) {
            throw new ConsultaDeCepIndisponivelException("ViaCEP respondeu sem corpo");
        }
        if (CEP_INEXISTENTE.equals(resposta.erro())) {
            return Optional.empty();
        }
        if (!dentroDoContrato(resposta)) {
            throw new ConsultaDeCepIndisponivelException("ViaCEP respondeu fora do contrato");
        }
        return Optional.of(new RegiaoDoCep(resposta.localidade().strip(), resposta.uf()));
    }

    private ViaCepResponse buscar(String cep) {
        try {
            return viaCepRestClient.get()
                    .uri("/ws/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientException ex) {
            throw new ConsultaDeCepIndisponivelException(
                    "ViaCEP indisponível: " + ex.getClass().getSimpleName(), ex);
        }
    }

    private static boolean dentroDoContrato(ViaCepResponse resposta) {
        String cidade = resposta.localidade();
        return cidade != null
                && !cidade.isBlank()
                && cidade.strip().length() <= LIMITE_DA_CIDADE
                && resposta.uf() != null
                && FORMATO_UF.matcher(resposta.uf()).matches();
    }
}
