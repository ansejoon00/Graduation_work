package com.example.graduation_work;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Track {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("uri")
    private String uri;

    @SerializedName("artists")
    private List<Artist> artists;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }
}