package com.github.warada_v2;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Native integration test: exercises the packaged native executable (or the JVM-packaged
 * application when the native profile is not active) over HTTP, verifying the health endpoint
 * responds correctly outside of the in-JVM test harness used by {@code @QuarkusTest}.
 *
 * <p>Run with: {@code ./mvnw -Pnative -Dquarkus.native.container-build=true verify}
 */
@QuarkusIntegrationTest
class HealthCheckResourceIT {

  @Test
  void healthEndpointIsAvailableInNativeMode() {
    given().when().get("/health").then().statusCode(200).body("status", equalTo("UP"));
  }
}
