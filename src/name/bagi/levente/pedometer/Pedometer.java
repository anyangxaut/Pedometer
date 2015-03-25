package name.bagi.levente.pedometer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class Pedometer extends Activity {
	// 测试标签
	private static final String TAG = "Pedometer";
	// 应用设置信息
    private SharedPreferences mSettings;
    private PedometerSettings mPedometerSettings;
    // 获取ui控件，并显示步数统计结果
    private TextView mStepValueView;
    private int mStepValue;
   
    private int mMaintain;
    // 退出标志
    private boolean mQuitting = false;

    // 当service正在运行，该tag为true
    private boolean mIsRunning;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
       
        super.onCreate(savedInstanceState);
        
        // 初始化步数
        mStepValue = 0;
        
        setContentView(R.layout.main);
        
    }


    @Override
    protected void onResume() {
    
        super.onResume();
        
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mPedometerSettings = new PedometerSettings(mSettings);
        
        // 读取preferences中的信息，判断service是否正在运行
        mIsRunning = mPedometerSettings.isServiceRunning();
        
        // 启动service
        if (!mIsRunning && mPedometerSettings.isNewStart()) {
            startStepService();
            bindStepService();
        }
        else if (mIsRunning) {
            bindStepService();
        }
        
        mPedometerSettings.clearServiceRunning();

        mStepValueView = (TextView) findViewById(R.id.step_value);    
        
        resetValues(true);
        
        mMaintain = mPedometerSettings.getMaintainOption();     
               
    }
    
    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning) {
            unbindStepService();
        }
        if (mQuitting) {
            mPedometerSettings.saveServiceRunningWithNullTimestamp(mIsRunning);
        }
        else {
            mPedometerSettings.saveServiceRunningWithTimestamp(mIsRunning);
        }

        super.onPause();
    }


    private StepService mService;
    
    // service与activity建立联系
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((StepService.StepBinder)service).getService();

            mService.registerCallback(mCallback);
            mService.reloadSettings();
            
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    
    // 启动服务
    private void startStepService() {
        if (! mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
            mIsRunning = true;
            startService(new Intent(Pedometer.this,
                    StepService.class));
        }
    }
    // 绑定服务到activity
    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(Pedometer.this, 
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }
    // 解除service与activity之间的绑定关系
    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }
    // 停止服务运行
    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (mService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(Pedometer.this,
                  StepService.class));
        }
        mIsRunning = false;
    }
    // 重置步数变量
    private void resetValues(boolean updateDisplay) {
        if (mService != null && mIsRunning) {
            mService.resetValues();                    
        }
        else {
            mStepValueView.setText("0");
            SharedPreferences state = getSharedPreferences("state", 0);
            SharedPreferences.Editor stateEditor = state.edit();
            if (updateDisplay) {
                stateEditor.putInt("steps", 0);
                stateEditor.commit();
            }
        }
    }

    private static final int MENU_SETTINGS = 8;
    private static final int MENU_QUIT     = 9;

    private static final int MENU_PAUSE = 1;
    private static final int MENU_RESUME = 2;
    private static final int MENU_RESET = 3;
    
    /* Creates the menu items */
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mIsRunning) {
            menu.add(0, MENU_PAUSE, 0, R.string.pause)
            .setIcon(android.R.drawable.ic_media_pause)
            .setShortcut('1', 'p');
        }
        else {
            menu.add(0, MENU_RESUME, 0, R.string.resume)
            .setIcon(android.R.drawable.ic_media_play)
            .setShortcut('1', 'p');
        }
        menu.add(0, MENU_RESET, 0, R.string.reset)
        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
        .setShortcut('2', 'r');
        menu.add(0, MENU_SETTINGS, 0, R.string.settings)
        .setIcon(android.R.drawable.ic_menu_preferences)
        .setShortcut('8', 's')
        .setIntent(new Intent(this, Settings.class));
        menu.add(0, MENU_QUIT, 0, R.string.quit)
        .setIcon(android.R.drawable.ic_lock_power_off)
        .setShortcut('9', 'q');
        return true;
    }

    // 处理菜单选项事件
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PAUSE:
                if (mIsRunning) {
                    unbindStepService();
                }
//                unbindStepService();
                stopStepService();
                return true;
            case MENU_RESUME:
                startStepService();
                bindStepService();
                return true;
            case MENU_RESET:
                resetValues(true);
                return true;
            case MENU_QUIT:
                resetValues(false);
                if (mIsRunning) {
                    unbindStepService();
                }
//                unbindStepService();
                stopStepService();
                mQuitting = true;
                finish();
                return true;
        }
        return false;
    }
 
    
    private StepService.ICallback mCallback = new StepService.ICallback() {
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
    };
    
    private static final int STEPS_MSG = 1;
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case STEPS_MSG:
                    mStepValue = (int)msg.arg1;
                    mStepValueView.setText("" + mStepValue);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
        
    };
    

}