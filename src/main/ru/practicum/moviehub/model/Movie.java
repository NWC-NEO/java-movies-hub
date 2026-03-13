package ru.practicum.moviehub.model;

public class Movie {
    private Integer id;
    private String title;
    private int year;

    public Movie() {
    }

    public Movie(Integer id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}