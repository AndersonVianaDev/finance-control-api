package com.andersonvianadev.finance_control_api.infra.client.api;

public record CalendarExternalDTO(String data, boolean diaUtilBancario,
                                  String motivo, String proximoDiaUtil) {
}
