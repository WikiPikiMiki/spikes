package com.novoda.tpbot.bot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.novoda.tpbot.LastServerPersistence;
import com.novoda.tpbot.LastServerPreferences;

public class BotServiceBinder {

    private final Context context;

    private BotServiceConnection botServiceConnection;

    BotServiceBinder(Context context) {
        this.context = context;
    }

    void bind(BotView botView, String serverAddress) {
        if (botServiceConnection == null) {
            botServiceConnection = new BotServiceConnection(botView, serverAddress);
        }
        Intent botServiceIntent = new Intent(context, BoundAndroidBotService.class);
        context.bindService(botServiceIntent, botServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private class BotServiceConnection implements ServiceConnection {

        private final BotView botView;
        private final String serverAddress;

        private BotServiceConnection(BotView botView, String serverAddress) {
            this.botView = botView;
            this.serverAddress = serverAddress;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            LastServerPersistence lastServerPersistence = new LastServerPreferences(sharedPreferences);
            BotPresenter botPresenter = new BotPresenter(
                    SocketIOTelepresenceService.getInstance(),
                    botView,
                    lastServerPersistence,
                    serverAddress
            );
            BoundAndroidBotService.BotServiceBinder binder = (BoundAndroidBotService.BotServiceBinder) service;
            binder.setBotPresenter(botPresenter);
            binder.onDependenciesBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    }

    void unbind() {
        if (BoundAndroidBotService.isBound()) {
            context.unbindService(botServiceConnection);
            context.stopService(new Intent(context, BoundAndroidBotService.class));
            botServiceConnection = null;
        }
    }

}
