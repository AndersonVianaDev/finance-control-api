package com.andersonvianadev.finance_control_api.infra.client.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CalendarExternalDTO(
        String data,
        @JsonProperty("dia_util_bancario") Boolean diaUtilBancario,
        String motivo,
        @JsonProperty("proximo_dia_util") String proximoDiaUtil) {
}
