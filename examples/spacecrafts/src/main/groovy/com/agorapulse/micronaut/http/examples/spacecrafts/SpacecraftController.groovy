package com.agorapulse.micronaut.http.examples.spacecrafts

import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Status

/**
 * Spacecraft controller.
 */
@Controller('/spacecraft')
class SpacecraftController {

    private final SpacecraftDBService spacecraftDBService

    SpacecraftController(SpacecraftDBService spacecraftDBService) {
        this.spacecraftDBService = spacecraftDBService
    }

    @Get('/{country}')
    List<Spacecraft> list(String country) {
        return spacecraftDBService.findAllByCountry(country)
    }

    @Get('/{country}/{name}')
    Spacecraft show(String country, String name) {
        return spacecraftDBService.get(country, name)
    }

    @Post('/{country}/{name}') @Status(HttpStatus.CREATED)
    Spacecraft save(String country, String name) {
        Spacecraft planet = new Spacecraft(country: country, name: name)
        spacecraftDBService.save(planet)
        return planet
    }

    @Delete('/{country}/{name}') @Status(HttpStatus.NO_CONTENT)
    Spacecraft delete(String country, String name) {
        Spacecraft planet = show(country, name)
        spacecraftDBService.delete(planet)
        return planet
    }

}
