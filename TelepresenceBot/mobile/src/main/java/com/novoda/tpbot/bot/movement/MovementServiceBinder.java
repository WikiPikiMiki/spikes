package com.novoda.tpbot.bot.movement;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.novoda.tpbot.bot.device.DeviceConnection;

public class MovementServiceBinder {

    private final Context context;
    private final DeviceConnection deviceConnection;

    private MovementServiceConnection movementServiceConnection;

    public MovementServiceBinder(Context context, DeviceConnection deviceConnection) {
        this.context = context;
        this.deviceConnection = deviceConnection;
    }

    public void bind() {
        if (movementServiceConnection == null) {
            movementServiceConnection = new MovementServiceConnection(deviceConnection);
        }
        Intent botServiceIntent = new Intent(context, BoundAndroidMovementService.class);
        context.bindService(botServiceIntent, movementServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void sendCommand(String command) {
        deviceConnection.send(command);
    }

    private class MovementServiceConnection implements ServiceConnection {

        private final DeviceConnection deviceConnection;

        private MovementServiceConnection(DeviceConnection deviceConnection) {
            this.deviceConnection = deviceConnection;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BoundAndroidMovementService.ServiceBinder binder = (BoundAndroidMovementService.ServiceBinder) service;
            binder.setDeviceConnection(deviceConnection);
            binder.onDependenciesBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    }

    public void unbind() {
        if (BoundAndroidMovementService.isBound()) {
            context.unbindService(movementServiceConnection);
            context.stopService(new Intent(context, BoundAndroidMovementService.class));
            movementServiceConnection = null;
        }
    }

}
