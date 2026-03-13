package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.*;

public class MoviesServer {

    private final HttpServer server;
    private final MoviesStore store;

    private static final int MIN_YEAR = 1888;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public void clearStore() {
        store.clear();
    }

    private class MoviesHandler extends BaseHttpHandler {

        @Override
        public void handle(HttpExchange ex) throws IOException {

            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String query = ex.getRequestURI().getQuery();

            try {
                switch (method) {
                    case "GET":
                        handleGet(ex, path, query);
                        break;
                    case "POST":
                        handlePost(ex);
                        break;
                    case "DELETE":
                        handleDelete(ex, path);
                        break;
                    default:
                        sendError(ex, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                sendError(ex, 500, "Внутренняя ошибка сервера");
            }
        }

        private void handleGet(HttpExchange ex, String path, String query) throws IOException {
            String[] parts = path.split("/");

            if (parts.length == 3) {
                try {
                    int id = Integer.parseInt(parts[2]);
                    Optional<Movie> movie = store.findById(id);
                    if (movie.isPresent()) {
                        sendJson(ex, 200, movie.get());
                    } else {
                        sendError(ex, 404, "Фильм не найден");
                    }
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный ID");
                }
                return;
            }

            if (query != null) {
                if (!query.startsWith("year=")) {
                    sendError(ex, 400, "Некорректный параметр запроса - 'year'");
                    return;
                }
                try {
                    int year = Integer.parseInt(query.substring(5));
                    sendJson(ex, 200, store.findByYear(year));
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Некорректный параметр запроса - 'year'");
                }
                return;
            }
            sendJson(ex, 200, store.findAll());
        }

        private void handlePost(HttpExchange ex) throws IOException {
            String contentType = ex.getRequestHeaders().getFirst("Content-Type");

            if (contentType == null || !contentType.contains("application/json")) {
                sendError(ex, 415, "Unsupported Media Type");
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Movie movie;

            try {
                movie = gson.fromJson(body, Movie.class);
            } catch (JsonSyntaxException e) {
                sendError(ex, 422, "Некорректный JSON");
                return;
            }

            List<String> errors = validate(movie);

            if (!errors.isEmpty()) {
                sendJson(ex, 422, new ErrorResponse("Ошибка валидации", errors));
                return;
            }

            Movie saved = store.save(movie);
            sendJson(ex, 201, saved);
        }

        private void handleDelete(HttpExchange ex, String path) throws IOException {
            String[] parts = path.split("/");

            if (parts.length != 3) {
                sendError(ex, 400, "Некорректный ID");
                return;
            }

            try {
                int id = Integer.parseInt(parts[2]);

                if (store.delete(id)) {
                    sendNoContent(ex);
                } else {
                    sendError(ex, 404, "Фильм не найден");
                }
            } catch (NumberFormatException e) {
                sendError(ex, 400, "Некорректный ID");
            }
        }

        private int maxYear() {
            return Year.now().getValue() + 1;
        }

        private List<String> validate(Movie m) {
            List<String> errors = new ArrayList<>();

            if (m == null) {
                errors.add("Некорректный JSON");
                return errors;
            }

            if (m.getTitle() == null || m.getTitle().isBlank()) {
                errors.add("название не должно быть пустым");
            }

            if (m.getTitle() != null && m.getTitle().length() > 100) {
                errors.add("название слишком длинное");
            }

            if (m.getYear() < MIN_YEAR || m.getYear() > maxYear()) {
                errors.add("год должен быть между " + MIN_YEAR + " и " + maxYear());
            }
            return errors;
        }
    }
}