package id.neotica.orpheum.feature.player.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import id.neotica.orpheum.feature.player.PlaybackManager;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (AudioService.ACTION_PLAY_PAUSE.equals(action)) {
            PlaybackManager.toggle();
        } else if (AudioService.ACTION_NEXT.equals(action)) {
            PlaybackManager.next();
        }
    }
}
