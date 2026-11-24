package dev.davidcaetano.umbra_api.catalogo.repository;

public record ReguaDeOfertas(double pesoEconomia,
                             double logaritmoDaReferencia,
                             double pesoEconomiaHardware,
                             double pesoPercentualHardware,
                             long tetoEconomiaHardwareCentavos) {

    public double pesoMinimo() {
        return 1.0 - pesoEconomia;
    }

    public double pesoMinimoHardware() {
        return 1.0 - pesoEconomiaHardware - pesoPercentualHardware;
    }

    public double logaritmoDoTetoHardware() {
        return Math.log1p(tetoEconomiaHardwareCentavos);
    }
}
