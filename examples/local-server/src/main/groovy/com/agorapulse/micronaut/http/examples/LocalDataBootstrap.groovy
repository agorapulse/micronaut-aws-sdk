package com.agorapulse.micronaut.http.examples

import com.agorapulse.micronaut.http.examples.planets.Planet
import com.agorapulse.micronaut.http.examples.planets.PlanetDBService
import com.agorapulse.micronaut.http.examples.spacecrafts.Spacecraft
import com.agorapulse.micronaut.http.examples.spacecrafts.SpacecraftDBService

import javax.annotation.PostConstruct
import javax.inject.Singleton

@Singleton
class LocalDataBootstrap {

    private final PlanetDBService planetDBService
    private final SpacecraftDBService spacecraftDBService

    LocalDataBootstrap(PlanetDBService planetDBService, SpacecraftDBService spacecraftDBService) {
        this.planetDBService = planetDBService
        this.spacecraftDBService = spacecraftDBService
    }

    @PostConstruct
    void bootstrap() {
        planetDBService.save(new Planet(star: 'sun', name: 'mercury'))
        planetDBService.save(new Planet(star: 'sun', name: 'venus'))
        planetDBService.save(new Planet(star: 'sun', name: 'earth'))
        planetDBService.save(new Planet(star: 'sun', name: 'mars'))
        planetDBService.save(new Planet(star: 'sun', name: 'jupiter'))
        planetDBService.save(new Planet(star: 'sun', name: 'saturn'))
        planetDBService.save(new Planet(star: 'sun', name: 'uranus'))
        planetDBService.save(new Planet(star: 'sun', name: 'neptune'))

        spacecraftDBService.save(new Spacecraft(country: 'russia', name: 'vostok'))
        spacecraftDBService.save(new Spacecraft(country: 'usa', name: 'dragon'))
    }


}
