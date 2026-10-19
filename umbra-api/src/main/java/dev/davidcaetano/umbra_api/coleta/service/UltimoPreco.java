package dev.davidcaetano.umbra_api.coleta.service;

import java.time.Instant;

record UltimoPreco(long valorCentavos, boolean disponivel, Instant coletadoEm) {
}
