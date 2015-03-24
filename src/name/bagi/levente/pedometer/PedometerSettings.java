package name.bagi.levente.pedometer;

import android.content.SharedPreferences;

public class PedometerSettings {

    SharedPreferences mSettings;
    
    public static int M_NONE = 1;
    public static int M_PACE = 2;
    public static int M_SPEED = 3;
    
    public PedometerSettings(SharedPreferences settings) {
        mSettings = settings;
    }
    
    public boolean isMetric() {
        return mSettings.getString("units", "imperial").equals("metric");
    }
    // 获取步长，默认值20
    public float getStepLength() {
        try {
            return Float.valueOf(mSettings.getString("step_length", "20").trim());
        }
        catch (NumberFormatException e) {
            
            return 0f;
        }
    }
    // 获取体重，默认50
    public float getBodyWeight() {
        try {
            return Float.valueOf(mSettings.getString("body_weight", "50").trim());
        }
        catch (NumberFormatException e) {
            
            return 0f;
        }
    }
    // 获取运动类型，默认为跑步
    public boolean isRunning() {
        return mSettings.getString("exercise_type", "running").equals("running");
    }

    public int getMaintainOption() {
        String p = mSettings.getString("maintain", "none");
        return 
            p.equals("none") ? M_NONE : (
            p.equals("pace") ? M_PACE : (
            p.equals("speed") ? M_SPEED : ( 
            0)));
    }
    
    // 获取理想步速 // steps/minute
    public int getDesiredPace() {
        return mSettings.getInt("desired_pace", 180); 
    }
    // 获取理想速度 // km/h or mph
    public float getDesiredSpeed() {
        return mSettings.getFloat("desired_speed", 4f); 
    }
    // 保存理想参数设置
    public void savePaceOrSpeedSetting(int maintain, float desiredPaceOrSpeed) {
        SharedPreferences.Editor editor = mSettings.edit();
        if (maintain == M_PACE) {
            editor.putInt("desired_pace", (int)desiredPaceOrSpeed);
        }
        else
        if (maintain == M_SPEED) {
            editor.putFloat("desired_speed", desiredPaceOrSpeed);
        }
        editor.commit();
    }
      
    public boolean wakeAggressively() {
        return mSettings.getString("operation_level", "run_in_background").equals("wake_up");
    }
    public boolean keepScreenOn() {
        return mSettings.getString("operation_level", "run_in_background").equals("keep_screen_on");
    }

    // 内部处理
    public void saveServiceRunningWithTimestamp(boolean running) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("service_running", running);
        editor.putLong("last_seen", Utils.currentTimeInMillis());
        editor.commit();
    }
    
    public void saveServiceRunningWithNullTimestamp(boolean running) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("service_running", running);
        editor.putLong("last_seen", 0);
        editor.commit();
    }

    public void clearServiceRunning() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean("service_running", false);
        editor.putLong("last_seen", 0);
        editor.commit();
    }

    public boolean isServiceRunning() {
        return mSettings.getBoolean("service_running", false);
    }
    
    public boolean isNewStart() {
        // activity最后一次暂停至现在超过10分钟
        return mSettings.getLong("last_seen", 0) < Utils.currentTimeInMillis() - 1000*60*10;
    }

}
