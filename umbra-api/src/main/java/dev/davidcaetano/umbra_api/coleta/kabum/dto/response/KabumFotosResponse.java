package dev.davidcaetano.umbra_api.coleta.kabum.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KabumFotosResponse(List<String> m, List<String> g) {
}
