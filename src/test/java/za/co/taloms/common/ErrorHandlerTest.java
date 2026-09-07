package za.co.taloms.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErrorHandlerTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void nonExistentPageShouldRedirectUnauthenticatedUserToLogin() throws Exception {
        HttpResponse<String> response = get("/this-page-does-not-exist");
        assertEquals(302, response.statusCode());
        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location.contains("/login"),
                "Unauthenticated users should be redirected to login gracefully");
    }

    @Test
    void errorEndpointShouldRenderStyledErrorPageWithNavigation() throws Exception {
        HttpResponse<String> response = get("/error");
        String body = response.body();
        assertTrue(body.contains("Go Back Home"), "Should contain home button");
        assertTrue(body.contains("Go Back"), "Should contain go-back button");
        assertTrue(body.contains("href"), "Should contain links");
    }
}
