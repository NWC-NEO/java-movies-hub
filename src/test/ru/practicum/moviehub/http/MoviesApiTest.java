package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static HttpClient client;

    private final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @BeforeEach
    void beforeEach() {
        server.clearStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }


    // HELPER

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(Object body) throws Exception {

        String json = gson.toJson(body);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private List<Movie> parseMovies(String json) {
        return gson.fromJson(json, new ListOfMoviesTypeToken().getType());
    }


    // GET /movies

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        HttpResponse<String> resp = get("/movies");

        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void getMovies_whenMoviesExist_returnsList() throws Exception {

        post(new Movie(null, "Inception", 2010));
        post(new Movie(null, "Matrix", 1999));

        HttpResponse<String> resp = get("/movies");

        List<Movie> movies = parseMovies(resp.body());

        assertEquals(2, movies.size());
    }


    // POST /movies

    @Test
    void postMovie_whenValid_returns201() throws Exception {

        HttpResponse<String> resp = post(new Movie(null, "Inception", 2010));

        assertEquals(201, resp.statusCode());
        assertTrue(resp.body().contains("\"id\":1"));
    }

    @Test
    void postMovie_whenTitleEmpty_returns422() throws Exception {

        HttpResponse<String> resp = post(new Movie(null, "", 2010));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovie_whenTitleTooLong_returns422() throws Exception {

        String longTitle = "a".repeat(101);

        HttpResponse<String> resp = post(new Movie(null, longTitle, 2010));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovie_whenYearTooSmall_returns422() throws Exception {

        HttpResponse<String> resp = post(new Movie(null, "Film", 1800));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovie_whenYearTooLarge_returns422() throws Exception {

        int futureYear = Year.now().getValue() + 5;

        HttpResponse<String> resp = post(new Movie(null, "Film", futureYear));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovie_whenWrongContentType_returns415() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("test"))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void postMovie_whenInvalidJson_returns422() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{invalid json}"))
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }


    // GET /movies/{id}

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {

        post(new Movie(null, "Inception", 2010));

        HttpResponse<String> resp = get("/movies/1");

        Movie movie = gson.fromJson(resp.body(), Movie.class);

        assertEquals(200, resp.statusCode());
        assertEquals("Inception", movie.getTitle());
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {

        HttpResponse<String> resp = get("/movies/999");

        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovieById_whenIdNotNumber_returns400() throws Exception {

        HttpResponse<String> resp = get("/movies/abc");

        assertEquals(400, resp.statusCode());
    }


    // DELETE /movies/{id}

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {

        post(new Movie(null, "Film", 2000));

        HttpResponse<String> resp = delete("/movies/1");

        assertEquals(204, resp.statusCode());
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {

        HttpResponse<String> resp = delete("/movies/10");

        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovie_whenIdNotNumber_returns400() throws Exception {

        HttpResponse<String> resp = delete("/movies/abc");

        assertEquals(400, resp.statusCode());
    }


    // GET /movies?year

    @Test
    void getMoviesByYear_whenExists_returnsMovies() throws Exception {

        post(new Movie(null, "Film1", 2000));
        post(new Movie(null, "Film2", 2000));
        post(new Movie(null, "Film3", 2010));

        HttpResponse<String> resp = get("/movies?year=2000");

        List<Movie> movies = parseMovies(resp.body());

        assertEquals(2, movies.size());
    }

    @Test
    void getMoviesByYear_whenNone_returnsEmptyList() throws Exception {

        HttpResponse<String> resp = get("/movies?year=1990");

        assertEquals("[]", resp.body().trim());
    }

    @Test
    void getMoviesByYear_whenYearInvalid_returns400() throws Exception {
        HttpResponse<String> resp = get("/movies?year=abc");
        assertEquals(400, resp.statusCode());
    }


    // METHOD NOT ALLOWED

    @Test
    void unsupportedMethod_returns405() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
    }
}