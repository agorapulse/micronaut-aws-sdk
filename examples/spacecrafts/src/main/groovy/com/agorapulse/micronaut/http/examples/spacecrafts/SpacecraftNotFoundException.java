package com.agorapulse.micronaut.http.examples.spacecrafts;

public class SpacecraftNotFoundException extends RuntimeException {

    private final String spacecraft;

    public SpacecraftNotFoundException(String spacecraft) {
        this.spacecraft = spacecraft;
    }

    public SpacecraftNotFoundException(String spacecraft, String message) {
        super(message);
        this.spacecraft = spacecraft;
    }

    public SpacecraftNotFoundException(String spacecraft, String message, Throwable cause) {
        super(message, cause);
        this.spacecraft = spacecraft;
    }

    public SpacecraftNotFoundException(String spacecraft, Throwable cause) {
        super(cause);
        this.spacecraft = spacecraft;
    }

    public SpacecraftNotFoundException(String spacecraft, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.spacecraft = spacecraft;
    }

    public String getSpacecraft() {
        return spacecraft;
    }
}
