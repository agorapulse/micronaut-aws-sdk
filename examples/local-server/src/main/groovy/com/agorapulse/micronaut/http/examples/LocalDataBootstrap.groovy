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




}
