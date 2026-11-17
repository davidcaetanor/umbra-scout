package dev.davidcaetano.umbra_api.monitoramento.service;

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
class RestricaoDeUnicidadeNoSchemaTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void restricaoQueViraConflitoDeMonitoramentoDeveExistirNoSchemaComoUnica() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_constraint WHERE conname = ? AND contype = 'u'",
                Integer.class, MonitoramentoService.RESTRICAO_DE_UNICIDADE)).isEqualTo(1);
    }
}
