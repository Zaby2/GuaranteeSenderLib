package ru.bsh.guarantee.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;


@Data
public class SqlSenderProperties {

    private Map<String, DataSourceProperties> propertiesMap = new HashMap<>();

    @Data
    public static class DataSourceProperties {

        private String url;
        private String userName;
        private String password;
        private String driverClassName;
    }
}
