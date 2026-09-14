package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class LojaRepositoryTest extends IntegrationTestBase {

    @Autowired
    private LojaRepository lojaRepository;

    @Test
    void deveResolverIdDaLojaQuandoBuscarPeloCodigo() {
        LojaEntity steam = lojaRepository.findByCodigo(CodigoLoja.STEAM).orElseThrow();
        LojaEntity terabyte = lojaRepository.findByCodigo(CodigoLoja.TERABYTE).orElseThrow();

        assertThat(terabyte.getId()).isNotNull();
        assertThat(terabyte.getCodigo()).isEqualTo(CodigoLoja.TERABYTE);
        assertThat(terabyte.getId()).isNotEqualTo(steam.getId());
    }

    @Test
    void deveHidratarTodasAsLojasDoSeedQuandoBuscarTodas() {
        assertThat(lojaRepository.findAll())
                .extracting(LojaEntity::getCodigo)
                .containsExactlyInAnyOrder(CodigoLoja.STEAM, CodigoLoja.NUUVEM,
                        CodigoLoja.TERABYTE, CodigoLoja.EPIC, CodigoLoja.KABUM);
    }

}
