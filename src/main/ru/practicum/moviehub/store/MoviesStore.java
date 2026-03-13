package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;
import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public List<Movie> findByYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }

    public Movie save(Movie movie) {
        if (movie.getId() == null) {
            movie.setId(nextId++);
        }
        movies.put(movie.getId(), movie);
        return movie;
    }

    public boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}