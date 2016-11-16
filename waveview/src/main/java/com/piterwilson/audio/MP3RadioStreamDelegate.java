package com.piterwilson.audio;

public interface MP3RadioStreamDelegate {
    public void onRadioPlayerPlaybackStarted(MP3RadioStreamPlayer player);

    public void onRadioPlayerStopped(MP3RadioStreamPlayer player);

    public void onRadioPlayerError(MP3RadioStreamPlayer player);

    public void onRadioPlayerBuffering(MP3RadioStreamPlayer player);
}
