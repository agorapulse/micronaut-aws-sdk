package com.agorapulse.micronaut.http.examples.planets;

public class PlanetNotFoundException extends RuntimeException {

    private final String planet;

    public PlanetNotFoundException(String planet) {
        this.planet = planet;
    }

    public PlanetNotFoundException(String planet, String message) {
        super(message);
        this.planet = planet;
    }

    public PlanetNotFoundException(String planet, String message, Throwable cause) {
        super(message, cause);
        this.planet = planet;
    }

    public PlanetNotFoundException(String planet, Throwable cause) {
        super(cause);
        this.planet = planet;
    }

    public PlanetNotFoundException(String planet, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.planet = planet;
    }

    public String getPlanet() {
        return planet;
    }
}
