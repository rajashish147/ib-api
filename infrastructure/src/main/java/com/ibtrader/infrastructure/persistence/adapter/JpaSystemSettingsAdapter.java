package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.port.outbound.SystemSettingsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaSystemSettingsAdapter implements SystemSettingsPort {

    // Simple in-memory mock implementation for now
    private final ConcurrentHashMap<String, Object> settings = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T saveSetting(String key, T value) {
        settings.put(key, value);
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getSetting(String key) {
        return Optional.ofNullable((T) settings.get(key));
    }

    @Override
    public void deleteSetting(String key) {
        settings.remove(key);
    }
}
