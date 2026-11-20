package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChaveDoJogoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Back 4 Blood: Ultimate Edition|back 4 blood",
            "Back 4 Blood: Deluxe Edition|back 4 blood",
            "Back 4 Blood|back 4 blood",
            "Star Wars Outlaws Gold Edition|star wars outlaws",
            "Star Wars Outlaws - Deluxe Edition|star wars outlaws",
            "Battlefield™ 2042 Elite Edition|battlefield 2042",
            "TEKKEN 7 - Definitive Edition|tekken 7",
            "Marvel's Midnight Suns Digital+ Edition|marvel's midnight suns",
            "DRAGON BALL FIGHTERZ - FighterZ Edition|dragon ball fighterz",
            "Hogwarts Legacy: Digital Deluxe Edition|hogwarts legacy",
            "The Witcher 3: Wild Hunt GOTY|the witcher 3: wild hunt",
            "Pokémon Scarlet|pokemon scarlet"})
    void edicoesDoMesmoJogo_devemTerAMesmaChave(String nome, String chave) {
        assertThat(ChaveDoJogo.de(nome)).isEqualTo(chave);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ONE PIECE: PIRATE WARRIORS 4 Ultimate Edition|ONE PIECE World Seeker Deluxe Edition",
            "Gold Rush: The Game|Golden Axe",
            "Elite Dangerous|Elite Beat Agents"})
    void jogosDiferentes_naoDevemSeJuntar(String um, String outro) {
        assertThat(ChaveDoJogo.de(um)).isNotEqualTo(ChaveDoJogo.de(outro));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"Deluxe Edition|deluxe edition", "Edition|edition"})
    void nomeQueSoTemEdicao_deveVirarOProprioNome(String nome, String chave) {
        assertThat(ChaveDoJogo.de(nome)).isEqualTo(chave);
    }
}
