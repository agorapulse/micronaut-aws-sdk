package com.agorapulse.micronaut.http.basic;

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.http.MutableHttpHeaders;

import java.util.*;

public class BasicHttpHeaders implements MutableHttpHeaders {

    private final Map<String, String> headers;
    private final ConversionService conversionService;

    public BasicHttpHeaders(Map<String, String> headers, ConversionService conversionService) {
        this.headers = headers;
        this.conversionService = conversionService;
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
        String value = headers.get(name.toString());
        if (value != null) {
            return conversionService.convert(value, conversionContext);
        }
        return Optional.empty();
    }

    @Override
    public List<String> getAll(CharSequence name) {
        return Collections.singletonList(headers.get(name.toString()));
    }

    @Override
    public Set<String> names() {
        return headers.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        Set<String> names = names();
        List<List<String>> values = new ArrayList<>();
        for (String name : names) {
            values.add(getAll(name));
        }
        return Collections.unmodifiableList(values);
    }

    @Override
    public String get(CharSequence name) {
        return headers.get(name.toString());
    }

    @Override
    public MutableHttpHeaders add(CharSequence header, CharSequence value) {
        headers.put(header.toString(), value == null ? null : value.toString());
        return this;
    }

    @Override
    public void remove(CharSequence header) {
        headers.remove(header.toString());
    }
}
