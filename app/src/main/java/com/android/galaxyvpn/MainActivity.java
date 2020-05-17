package com.android.galaxyvpn;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.strongswan.android.R;
import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType;
import org.strongswan.android.logic.VpnStateService;
import org.strongswan.android.ui.VpnProfileControlActivity;
import org.strongswan.android.ui.VpnStateFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity implements VpnStateService.VpnStateListener {
    private Button connect;
    private static final String TAG = "NAOMI";
    private VpnProfile profile;
    private boolean mVisible;

    private VpnStateService mService;
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mService = ((VpnStateService.LocalBinder)service).getService();
            if (mVisible)
            {
                mService.registerListener(MainActivity.this);
                updateView();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connect = findViewById(R.id.connect);
        bindService(new Intent(this, VpnStateService.class),
                mServiceConnection, Service.BIND_AUTO_CREATE);
        VpnProfileDataSource vpnProfileDataSource = new VpnProfileDataSource(this);
        vpnProfileDataSource.open();
        profile = vpnProfileDataSource.getVpnProfile(1);

        if (profile == null) {
            Log.d(TAG, "onCreate: profile is NULL");
            profile = new VpnProfile();
            profile.setName("vpn.togrash.com");
            profile.setUsername("user");
            profile.setPassword("F8xd6QFcwVn6hwCx");
            profile.setVpnType(VpnType.IKEV2_EAP);
            profile.setGateway("vpn.togrash.com");
            profile.setFlags(0);
            profile.setSelectedAppsHandling(VpnProfile.SelectedAppsHandling.SELECTED_APPS_DISABLE);
            vpnProfileDataSource.insertProfile(profile);
        } else {
            Log.d(TAG, "onCreate: profile is not null : " + profile.getGateway());
        }
        vpnProfileDataSource.close();
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VpnProfileControlActivity.class);
                intent.setAction(VpnProfileControlActivity.START_PROFILE);
                intent.putExtra(VpnProfileControlActivity.EXTRA_VPN_PROFILE_ID, profile.getUUID().toString());
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mVisible = true;
        if (mService != null)
        {
            mService.registerListener(this);
            updateView();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mVisible = false;
        if (mService != null)
        {
            mService.unregisterListener(this);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mService != null)
        {
            unbindService(mServiceConnection);
        }
    }

    @Override
    public void stateChanged() {
        updateView();
    }

    private void updateView() {
        VpnStateService.State state = mService.getState();
        Log.d(TAG, "updateView: state : "+state.name());

        switch (state){
            case DISABLED:
                connect.setText("Connect to VPN");
                connect.setEnabled(true);
                break;
            case CONNECTED:
                connect.setText("Disconnect");
                connect.setEnabled(true);
                break;
            case CONNECTING:
                connect.setText("Connecting... Please wait");
                connect.setEnabled(false);
                break;
            case DISCONNECTING:
                connect.setText("Disconnecting... Please wait");
                connect.setEnabled(false);
                break;

        }

    }


}
