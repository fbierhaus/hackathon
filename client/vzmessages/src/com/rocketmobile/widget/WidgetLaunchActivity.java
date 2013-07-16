package com.rocketmobile.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.verizon.mms.ui.ConversationListFragment;
import com.verizon.mms.ui.activity.Provisioning;

public class WidgetLaunchActivity extends Activity {
    public static Class<?> runningActivity = Provisioning.class;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(getApplicationContext(), runningActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ConversationListFragment.IS_WIDGET, true);
        startActivity(intent);
        finish();
    }
    
    
    
}
