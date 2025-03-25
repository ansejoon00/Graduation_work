package com.example.graduation_work;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotifyService {
    @GET("v1/search")
    Call<SearchResponse> searchPlaylists(@Query("q") String query, @Query("type") String type, @Header("Authorization") String authorization);

    @GET("v1/playlists/{playlist_id}/tracks")
    Call<PlaylistResponse> getPlaylistTracks(@Path("playlist_id") String playlistId, @Header("Authorization") String authorization);

    @GET("v1/search")
    Call<SearchResponse> searchTracks(@Query("q") String query, @Query("type") String type, @Header("Authorization") String authorization);
}