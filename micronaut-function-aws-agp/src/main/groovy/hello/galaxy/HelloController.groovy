package hello.galaxy

import com.agorapulse.micronaut.http.basic.HttpResponder
import groovy.transform.CompileStatic
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*

@CompileStatic
@Controller("/hello")
class HelloController {

    private final HttpResponder responder

    HelloController(HttpResponder responder) {
        this.responder = responder
    }

    @Get("/")
    String index() {
        return "Hello Galaxy!"
    }

    @Post("/greet")
    HttpResponse<Greetings> newGreeting(@Body Greetings body) {
        return responder.created(body)
    }

    @Get("/greet/{message}/{language}")
    Greetings greet(String message, String language) {
        return new Greetings(message: message, language: language)
    }

    @Put('/mfa')
    HttpResponse mfa(Optional<Boolean> enable, Optional<Integer> multiFactorCode) {
        if (!multiFactorCode.present) {
            return responder.badRequest()
        }
        if (enable.present && enable.get()) {
            int code = multiFactorCode.get()
            return responder.ok([enable: true, mfa: code])
        }
        return responder.ok([enable: false])
    }
}
