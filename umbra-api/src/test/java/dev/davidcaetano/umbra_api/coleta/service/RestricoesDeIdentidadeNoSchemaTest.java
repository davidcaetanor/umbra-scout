package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RestricoesDeIdentidadeNoSchemaTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void todaRestricaoQueDisparaNovaTentativaDeveExistirNoSchemaComoUnica() {
        assertThat(ColetaService.RESTRICOES_DE_IDENTIDADE).allSatisfy(nome -> assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname = ? AND contype = 'u'", Integer.class, nome))
                .as("restricao unica %s no schema", nome)
                .isEqualTo(1));
    }
}
