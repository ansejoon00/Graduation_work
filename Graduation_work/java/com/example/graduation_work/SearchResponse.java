package com.example.graduation_work;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchResponse {
    @SerializedName("tracks")
    private TrackContainer tracks;

    @SerializedName("playlists")
    private PlaylistContainer playlists;

    public TrackContainer getTracks() {
        return tracks;
    }

    public void setTracks(TrackContainer tracks) {
        this.tracks = tracks;
    }

    public PlaylistContainer getPlaylists() {
        return playlists;
    }

    public void setPlaylists(PlaylistContainer playlists) {
        this.playlists = playlists;
    }

    public class TrackContainer {
        @SerializedName("items")
        private List<TrackItem> items;

        public List<TrackItem> getItems() {
            return items;
        }

        public void setItems(List<TrackItem> items) {
            this.items = items;
        }
    }

    public class TrackItem {
        @SerializedName("uri")
        private String uri;
        @SerializedName("name")
        private String name;
        @SerializedName("artists")
        private List<Artist> artists;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public void setArtists(List<Artist> artists) {
            this.artists = artists;
        }
    }

    public class Artist {
        @SerializedName("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public class PlaylistContainer {
        @SerializedName("items")
        private List<Playlist> items;

        public List<Playlist> getItems() {
            return items;
        }

        public void setItems(List<Playlist> items) {
            this.items = items;
        }
    }
}