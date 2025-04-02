package com.example.graduation_work;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PlaylistResponse {

    @SerializedName("tracks")
    private Tracks tracks;

    public Tracks getTracks() {
        return tracks;
    }

    public void setTracks(Tracks tracks) {
        this.tracks = tracks;
    }

    public static class Tracks {
        @SerializedName("items")
        private List<TrackItem> items;

        public List<TrackItem> getItems() {
            return items;
        }

        public void setItems(List<TrackItem> items) {
            this.items = items;
        }
    }

    public static class TrackItem {
        @SerializedName("track")
        private Track track;

        public Track getTrack() {
            return track;
        }

        public void setTrack(Track track) {
            this.track = track;
        }
    }

    public static class Track {
        @SerializedName("uri")
        private String uri;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }
}