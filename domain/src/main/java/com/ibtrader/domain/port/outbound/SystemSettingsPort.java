package com.ibtrader.domain.port.outbound;

import java.util.Optional;

public interface SystemSettingsPort {
    <T> T saveSetting(String key, T value);
    <T> Optional<T> getSetting(String key);
    void deleteSetting(String key);
}
