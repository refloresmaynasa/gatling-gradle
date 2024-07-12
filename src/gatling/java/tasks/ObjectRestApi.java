package tasks;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ObjectRestApi extends Simulation {
    // Define environment variables
    String baseUrl = System.getProperty("baseUrl", "https://api.restful-api.dev/objects");
    String quantityUsers = System.getProperty("quantityUsers", "2");

    // Define the data feeder
    FeederBuilder.FileBased<Object> objects = jsonFile("data/objects.json").random();

    // Define de Protocol with base URL and headers
    private final HttpProtocolBuilder httpProtocol = http.baseUrl(baseUrl);

    // We want to test the POST, PUT and GET of objects in Object Rest API
    // Define scenario
    ScenarioBuilder scn = scenario("Object Rest API Test")
        .feed(objects)
        .exec(http("POST #{name}")
            .post("")
            .header("content-type", "application/json")
            .body(StringBody("""
                {
                   "name": "#{name}",
                   "data": {
                        "category": "#{data.category}",
                        "brand": "#{data.brand}",
                        "color": "#{data.color}",
                        "price": #{data.price}
                   }
                }
                """))
            .check(status().is(200))
            .check(bodyLength().gt(100))
            .check(jmesPath("id").saveAs("ID"))
            .check(jmesPath("name").isEL("#{name}")))
        .exec(http( "PUT #{name}")
                .put(session ->  "/" + session.getString("ID"))
                .header("content-type", "application/json")
                .body(StringBody("""
                    {
                       "name": "#{name}",
                       "data": {
                            "category": "#{data.category}",
                            "brand": "#{data.brand}",
                            "color": "#{data.color}",
                            "price": #{data.price},
                            "inStock": #{data.inStock}
                       }
                    }
                    """))
                .check(status().is(200))
                .check(bodyLength().gt(20))
                .check(jmesPath("data.inStock").isEL("#{data.inStock}"))
                .check(jmesPath("updatedAt").exists()))
        .exec(http("GET #{name}")
                .get(session -> "/" + session.getString("ID"))
                .check(status().is(200))
                .check(bodyLength().gt(20))
                .check(bodyString().saveAs("BODY"))
                .check(jmesPath("id").notNull())
                .check(jmesPath("name").isEL("#{name}")))
        .exec(session -> {
            System.out.println("GET Body: " + session.getString("BODY"));
            return session;
        });

    {
        setUp(
            scn.injectClosed(
                constantConcurrentUsers(Integer.parseInt(quantityUsers)).during(5)
            )
        ).protocols(httpProtocol);
    }
}
