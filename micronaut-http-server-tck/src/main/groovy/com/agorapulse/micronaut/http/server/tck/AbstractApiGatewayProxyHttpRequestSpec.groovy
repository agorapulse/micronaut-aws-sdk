package com.agorapulse.micronaut.http.server.tck

import com.agorapulse.gru.Gru
import spock.lang.PendingFeature
import spock.lang.Specification

abstract class AbstractApiGatewayProxyHttpRequestSpec extends Specification {

    abstract Gru getGru();

    void 'should return hello'() {
        expect:
            gru.test {
                get '/hello'
                expect {
                    text inline("Hello Galaxy!")
                }
            }
    }

    void 'should transform object to json'() {
        expect:
            gru.test {
                get '/hello/greet/hello/en'
                expect {
                    json inline('{ "message" : "hello", "language" : "en" }')
                }
            }
    }

    void 'transform body into object and returns status'() {
        expect:
            gru.test {
                post '/hello/greet', {
                    headers 'Content-Type': 'application/json'
                    json inline('{ "message" : "hello", "language" : "en" }')
                }
                expect {
                    status CREATED
                    json inline('{ "message" : "hello", "language" : "en" }')
                }
            }
    }

    @PendingFeature
    void 'optional int parameter from body'() {
        expect:
            gru.test {
                put '/hello/mfa', {
                    json inline('{"enable": true }')
                }
                expect {
                    status BAD_REQUEST
                }
            }
    }

}
