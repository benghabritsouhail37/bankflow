package com.bankflow.api;

import com.bankflow.repository.UserRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.UUID;

import static io.restassured.RestAssured.given;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.hamcrest.Matchers.hasItem;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class UserApiRestAssuredTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private String createdEmail;


    @BeforeEach
    void configureRestAssured() {

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }


    @AfterEach
    void cleanDatabase() {

        if (createdEmail != null) {

            userRepository.findByEmail(createdEmail)
                    .ifPresent(userRepository::delete);
        }
    }


    // TEST 1 : utilisateur inexistant -> 404
    @Test
    void shouldReturn404WhenUserDoesNotExist() {

        given()
                .accept(ContentType.JSON)

        .when()
                .get("/api/users/{id}", -1L)

        .then()
                .statusCode(404)
                .body("title", equalTo("User not found"))
                .body("status", equalTo(404))
                .body(
                        "detail",
                        equalTo("User not found with id: -1")
                );
    }


    // TEST 2 : création réelle -> 201
    @Test
    void shouldCreateUserWithRealHttpRequest() {

        // GIVEN
        createdEmail =
                "rest-" + UUID.randomUUID() + "@example.com";

        String requestBody = """
                {
                  "firstName": "Rest",
                  "lastName": "Assured",
                  "email": "%s"
                }
                """.formatted(createdEmail);

        // WHEN + THEN
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(requestBody)

        .when()
                .post("/api/users")

        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("firstName", equalTo("Rest"))
                .body("lastName", equalTo("Assured"))
                .body("email", equalTo(createdEmail));

        // Vérification réelle dans PostgreSQL
        assertTrue(
                userRepository.findByEmail(createdEmail).isPresent()
        );
    }
    // TEST 3 : email dupliqué -> 409
@Test
void shouldReturn409WhenEmailAlreadyExists() {

    // GIVEN
    createdEmail =
            "duplicate-rest-" + UUID.randomUUID() + "@example.com";

    String firstRequestBody = """
            {
              "firstName": "First",
              "lastName": "User",
              "email": "%s"
            }
            """.formatted(createdEmail);

    String secondRequestBody = """
            {
              "firstName": "Second",
              "lastName": "User",
              "email": "%s"
            }
            """.formatted(createdEmail);


    // Première création : doit réussir
    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(firstRequestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(201);


    // Deuxième création : doit être refusée
    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(secondRequestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(409)
            .body("title", equalTo("Email already exists"))
            .body("status", equalTo(409))
            .body(
                    "detail",
                    equalTo(
                            "A user with email "
                                    + createdEmail
                                    + " already exists"
                    )
            );


    // Vérification PostgreSQL
    assertTrue(
            userRepository.findByEmail(createdEmail).isPresent()
    );
}
// TEST 4 : email invalide -> 400
@Test
void shouldReturn400WhenEmailIsInvalid() {

    String requestBody = """
            {
              "firstName": "Rest",
              "lastName": "Assured",
              "email": "invalid-email"
            }
            """;

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(400)
            .body("title", equalTo("Validation failed"))
            .body("status", equalTo(400))
            .body(
                    "detail",
                    equalTo("One or more fields are invalid")
            )
            .body(
                    "errors.email",
                    equalTo("Email must be valid")
            );
}
// TEST 5 : plusieurs erreurs de validation -> 400
@Test
void shouldReturn400WithMultipleValidationErrors() {

    String requestBody = """
            {
              "firstName": "   ",
              "lastName": "User",
              "email": "invalid-email"
            }
            """;

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(400)
            .body("title", equalTo("Validation failed"))
            .body("status", equalTo(400))
            .body(
                    "detail",
                    equalTo("One or more fields are invalid")
            )
            .body(
                    "errors.firstName",
                    equalTo("First name is required")
            )
            .body(
                    "errors.email",
                    equalTo("Email must be valid")
            );
}// TEST 6 : créer un utilisateur puis le récupérer par son ID -> 200
@Test
void shouldReturnCreatedUserById() {

    // GIVEN
    createdEmail =
            "get-rest-" + UUID.randomUUID() + "@example.com";

    String requestBody = """
            {
              "firstName": "Souhail",
              "lastName": "Test",
              "email": "%s"
            }
            """.formatted(createdEmail);

    // Première étape : créer réellement l'utilisateur
    Long userId =
            given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(requestBody)

            .when()
                    .post("/api/users")

            .then()
                    .statusCode(201)

                    // Récupérer l'ID généré par PostgreSQL
                    .extract()
                    .jsonPath()
                    .getLong("id");


    // Deuxième étape : rechercher cet utilisateur avec son ID
    given()
            .accept(ContentType.JSON)

    .when()
            .get("/api/users/{id}", userId)

    .then()
            .statusCode(200)
            .body("id", equalTo(userId.intValue()))
            .body("firstName", equalTo("Souhail"))
            .body("lastName", equalTo("Test"))
            .body("email", equalTo(createdEmail));
}
// TEST 7 : récupérer la liste des utilisateurs -> 200
@Test
void shouldReturnListContainingCreatedUser() {

    // GIVEN : créer un utilisateur réel
    createdEmail =
            "list-rest-" + UUID.randomUUID() + "@example.com";

    String requestBody = """
            {
              "firstName": "List",
              "lastName": "Test",
              "email": "%s"
            }
            """.formatted(createdEmail);

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(201);


    // WHEN + THEN : récupérer tous les utilisateurs
    given()
            .accept(ContentType.JSON)

    .when()
            .get("/api/users")

    .then()
            .statusCode(200)
            .body("email", hasItem(createdEmail))
            .body("firstName", hasItem("List"))
            .body("lastName", hasItem("Test"));
}
// TEST 8 : exactement 50 caractères -> 201
@Test
void shouldAcceptFirstNameWithExactly50Characters() {

    // GIVEN
    createdEmail =
            "boundary-50-" + UUID.randomUUID() + "@example.com";

    String firstName = "A".repeat(50);

    String requestBody = """
            {
              "firstName": "%s",
              "lastName": "Boundary",
              "email": "%s"
            }
            """.formatted(firstName, createdEmail);

    // WHEN + THEN
    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(201)
            .body("firstName", equalTo(firstName))
            .body("email", equalTo(createdEmail));
}


// TEST 9 : 51 caractères -> 400
@Test
void shouldRejectFirstNameWith51Characters() {

    // GIVEN
    String firstName = "A".repeat(51);

    String requestBody = """
            {
              "firstName": "%s",
              "lastName": "Boundary",
              "email": "boundary-invalid@example.com"
            }
            """.formatted(firstName);

    // WHEN + THEN
    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(400)
            .body("title", equalTo("Validation failed"))
            .body("status", equalTo(400))
            .body(
                    "errors.firstName",
                    equalTo(
                            "First name must not exceed 50 characters"
                    )
            );
}
// TEST 10 : email vide -> 400
@Test
void shouldRejectBlankEmail() {

    String requestBody = """
            {
              "firstName": "Rest",
              "lastName": "Assured",
              "email": "   "
            }
            """;

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(400)
            .body("title", equalTo("Validation failed"))
            .body("status", equalTo(400))
            .body("errors.email", notNullValue());
}
// TEST 11 : JSON mal formé -> 400
@Test
void shouldReturn400WhenJsonIsMalformed() {

    String malformedJson = """
            {
              "firstName": "Rest",
              "lastName": "Assured",
              "email": "test@example.com"
            """;
    // Il manque volontairement la } finale

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(malformedJson)

    .when()
            .post("/api/users")

    .then()
            .statusCode(400);
}
// TEST 12 : mauvais Content-Type -> 415
@Test
void shouldReturn415WhenContentTypeIsNotJson() {

    String requestBody = """
            {
              "firstName": "Rest",
              "lastName": "Assured",
              "email": "test@example.com"
            }
            """;

    given()
            .contentType(ContentType.TEXT)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .post("/api/users")

    .then()
            .statusCode(415);
}
// TEST 13 : méthode HTTP non autorisée -> 405
@Test
void shouldReturn405WhenHttpMethodIsNotAllowed() {

    String requestBody = """
            {
              "firstName": "Rest",
              "lastName": "Assured",
              "email": "test@example.com"
            }
            """;

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(requestBody)

    .when()
            .put("/api/users")

    .then()
            .statusCode(405);
}
// TEST 14 : route inexistante -> 404
@Test
void shouldReturn404WhenEndpointDoesNotExist() {

    given()
            .accept(ContentType.JSON)

    .when()
            .get("/api/unknown-route")

    .then()
            .statusCode(404);
}
// TEST 15 : body vide -> 400
@Test
void shouldReturn400WhenRequestBodyIsEmpty() {

    given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("")

    .when()
            .post("/api/users")

    .then()
            .statusCode(400);
}
}