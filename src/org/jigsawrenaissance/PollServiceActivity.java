package org.jigsawrenaissance;

import org.jigsawrenaissance.Constants;
import org.jigsawrenaissance.PollService;
import android.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class PollServiceActivity extends Activity {	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.i(Constants.TAG, "Starting polling service");
		Intent intentPollService = new Intent(PollServiceActivity.this, PollService.class);
        startService(intentPollService);
    }
}