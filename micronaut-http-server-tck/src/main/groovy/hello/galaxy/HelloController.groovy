package hello.galaxy

import groovy.transform.CompileStatic
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

@CompileStatic
@Controller("/hello")
class HelloController {
    @Get("/")
    String index() {
        return "Hello Galaxy!"
    }

    @Post("/greet")
    HttpResponse<Greetings> newGreeting(@Body Greetings body) {
        return HttpResponse.created(body)
    }

    @Get("/greet/{message}/{language}")
    Greetings greet(String message, String language) {
        return new Greetings(message: message, language: language)
    }

    @Put('/mfa')
    HttpResponse mfa(Optional<Boolean> enable, Optional<Integer> multiFactorCode) {
        if (!multiFactorCode.present) {
            return HttpResponse.badRequest()
        }
        if (enable.present && enable.get()) {
            int code = multiFactorCode.get()
            return HttpResponse.ok([enable: true, mfa: code])
        }
        return HttpResponse.ok([enable: false])
    }
}
