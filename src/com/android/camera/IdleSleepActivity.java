/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AbsListView.LayoutParams;
import android.graphics.Color;
import android.view.Gravity;
import com.android.camera2.R;
import android.view.View;
import android.content.Intent;
import com.android.camera.util.AndroidServices;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context; 
import android.text.TextUtils;
import android.app.KeyguardManager;
import javax.annotation.Nullable;
import android.graphics.Color;

// Use this Activity to show idlesleep information
public class IdleSleepActivity extends Activity {
    private LinearLayout mLayout;
    private TextView mTextView;
    private boolean isUpFinish = false;
    @Override
    protected final void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mTextView = new TextView(this);
        mTextView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        mTextView.setText(R.string.idle_sleep_text);
        mTextView.setTextSize(15);
        mTextView.setTextColor(Color.WHITE);
        mTextView.setGravity(Gravity.CENTER);
        mLayout = new LinearLayout(this);
        mLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent me) {
                switch (me.getAction()) {
                    case MotionEvent.ACTION_UP:
                        IdleSleepActivity.this.finish();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        mLayout.addView(mTextView);
        setContentView(mLayout);
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mHomeKeyEventReceiver, homeFilter);
        setDisablePreviewScreenshots(true);
    }

    @Override
    protected final void onRestart() {
        if (isKeyguardLocked()) {
            finish();
        } else {
            mTextView.setVisibility(View.VISIBLE);
            mLayout.setBackgroundColor(Color.parseColor("#1b1b1b"));
        }
        super.onRestart();

    }

    @Override
    protected final void onStop() {
        mTextView.setVisibility(View.GONE);
        mLayout.setBackgroundColor(Color.parseColor("#01000000"));
        super.onStop();
    }
    @Override
    protected final void onDestroy() {
        if (null != mHomeKeyEventReceiver) {
            unregisterReceiver(mHomeKeyEventReceiver);
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		finish();
        return super.onKeyDown(keyCode, event);
    }

    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_RECENT_APPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();  
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY) ||
                        TextUtils.equals(reason, SYSTEM_RECENT_APPS)) {
                    IdleSleepActivity.this.finish();
                }
            }
        }
    };

    @Nullable
    private KeyguardManager mKeyguardManager = null;
    protected boolean isKeyguardLocked() {
        if (mKeyguardManager == null) {
            mKeyguardManager = AndroidServices.instance().provideKeyguardManager();
        }
        if (mKeyguardManager != null) {
            return mKeyguardManager.isKeyguardLocked();
        }
        return false;
    }
}