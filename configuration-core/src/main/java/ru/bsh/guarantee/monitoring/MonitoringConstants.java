package ru.bsh.guarantee.monitoring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MonitoringConstants {

    HTTP_SENDER("http_sender", "send"),
    SQL_SENDER("sql_sender", "sql_send"),
    NO_SQL_SENDER("no-sql_sender", "no-sql_send"),
    BROKER_SENDER("broker_sender", "broker_send"),

    SQL_PULLER("sql_puller", "sql_pull"),
    NO_SQL_PULLER("no-sql_puller", "no-sql_pull"),
    BROKER_PULLER("broker_puller", "broker_pull"),

    SQL_CLEANER("sql_clean", "sql_clean"),
    NO_SQL_CLEANER("no-sql_clean", "no-sql_clean"),

    SIGN_DATA("signature_service", "sign_data"),
    SIGNATURE_CHECK("signature_service", "verify_signature"),

    GUARANTEE_SENDER("guarantee_proxy", "guarantee_send"),

    TRANSPORT("balancing_group", "send");

    private final String layer;
    private final String operation;
}
