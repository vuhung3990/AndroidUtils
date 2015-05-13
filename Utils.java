package YOUR PACKAGE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by YuHung on 5/7/2015.
 */
public class Utils {

	/**
     * Detects and toggles immersive mode (also known as "hidey bar" mode).
     *
     * <pre>
     * sample:
     *     private boolean fullscreen;
     *     ................
     *     Activity activity = (Activity)context;
     *     toggleHideyBar(activity, !fullscreen);
     *     fullscreen = !fullscreen;
     * </pre>
     */
    public void toggleHideyBar(Activity activity, boolean fullscreen) {
        if(Build.VERSION.SDK_INT >= 11){
            // The UI options currently enabled are represented by a bitfield.
            // getSystemUiVisibility() gives us that bitfield.
            int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
            int newUiOptions = uiOptions;
            boolean isImmersiveModeEnabled =
                    ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
            if (isImmersiveModeEnabled) {
                Log.i(context.getPackageName(), "Turning immersive mode mode off. ");
            } else {
                Log.i(context.getPackageName(), "Turning immersive mode mode on.");
            }

            // Navigation bar hiding:  Backwards compatible to ICS.
            if (Build.VERSION.SDK_INT >= 14) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }

            // Status bar hiding: Backwards compatible to Jellybean
            if (Build.VERSION.SDK_INT >= 16) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }

            // Immersive mode: Backward compatible to KitKat.
            // Note that this flag doesn't do anything by itself, it only augments the behavior
            // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
            // all three flags are being toggled together.
            // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
            // Sticky immersive mode differs in that it makes the navigation and status bars
            // semi-transparent, and the UI flag does not get cleared when the user interacts with
            // the screen.
            if (Build.VERSION.SDK_INT >= 18) {
                newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
        }else {
            // for android pre 11
            WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            if (fullscreen)
            {
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            }
            else
            {
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
            }
            activity.getWindow().setAttributes(attrs);
        }
    }

    /**
     * get all image and video file on device, require READ_EXTERNAL , WRITE_EXTERNAL
     *
     * @param context
     * @return List file
     */
    public static List<File> getAllMediaFilesOnDevice(Context context) {
        List<File> files = new ArrayList<>();
        try {

            final String[] columns = {MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME};

            MergeCursor cursor = new MergeCursor(new Cursor[]{context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, null),
                    context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, null, null, null),
                    context.getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI, columns, null, null, null),
                    context.getContentResolver().query(MediaStore.Video.Media.INTERNAL_CONTENT_URI, columns, null, null, null)
            });
            cursor.moveToFirst();
            files.clear();
            while (!cursor.isAfterLast()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                int lastPoint = path.lastIndexOf(".");
                path = path.substring(0, lastPoint) + path.substring(lastPoint).toLowerCase();
                files.add(new File(path));
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * Write content into file
     *
     * @param path          require WRITE_EXTERNAL
     * @param contentString
     */
    public static void writeContentFile(String path, String contentString) {
        File file = new File(path);

        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(contentString.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get content string from file
     *
     * @param path require READ_EXTERNAL
     * @return String content of file
     */
    public static String getContentFromFile(String path) {
        String result = null;
        File file = new File(path);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                fis.close();
                result = sb.toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * delete file, require READ_EXTERNAL , WRITE_EXTERNAL
     *
     * @param path
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        file.delete();
    }

    /**
     * get mine type of file
     *
     * @param path
     * @param useThisIfNull if cannot detect , it will use this instead
     * @return mine type for upload file, example : a.jpg => image/jpeg
     */
    public static String getMimeType(String path, String useThisIfNull) {
        String type = useThisIfNull;
        String extension = null;
        int i = path.lastIndexOf('.');
        if (i > 0) {
            extension = path.substring(i + 1);
        }
        if (extension != null || extension == "") {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }

    /**
     * copy text to clipboard
     *
     * @param context
     * @param text    string to copy
     * @param label   reuqire android sdk >= 11
     */
    public static void copyToClipBoard(Context context, String text, CharSequence label) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * convert milisec (unix time) to hh:MM:ss
     *
     * @param millisUntilFinished
     * @return String time
     */
    public static String convertSecondsToHMmSs(long millisUntilFinished) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
    }

    /**
     * Example: 1,2,5,6 to 01,02,05,06
     *
     * @param number
     * @return String of number
     */
    public static String formatAtleast2digit(int number) {
        return String.format("%02d", number);
    }

    /**
     * format long bytes to several unit such as: KB, MB, GB, TB...
     *
     * @param bytes
     * @param place
     * @return Example: 2048bytes => 2MB
     */
    public static String formatSize(long bytes, int place) {
        int level = 0;
        float number = bytes;
        String[] unit = {"bytes", "KB", "MB", "GB", "TB", "PB"};

        while (number >= 1024f) {
            number /= 1024f;
            level++;
        }

        String formatStr = null;
        if (place == 0) {
            formatStr = "###0";
        } else {
            formatStr = "###0.";
            for (int i = 0; i < place; i++) {
                formatStr += "#";
            }
        }
        DecimalFormat nf = new DecimalFormat(formatStr);
        String[] value = new String[2];
        value[0] = nf.format(number);
        value[1] = unit[level];

        return value[0] + " " + value[1];
    }

    /**
     * get folder total size
     *
     * @param folderPath require READ_EXTERNAL
     * @return Long bytes
     */
    public static long getFolderSize(File folderPath) {
        long totalSize = 0;
        if (folderPath == null) {
            return 0;
        }
        if (!folderPath.isDirectory()) {
            return folderPath.length();
        }
        try {
            File[] files = folderPath.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    totalSize += file.length();
                } else if (file.isDirectory()) {
                    totalSize += file.length();
                    totalSize += getFolderSize(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalSize;
    }

    /**
     * delete all file in folder
     *
     * @param folderPath require READ_EXTERNAL , WRITE_EXTERNAL
     */
    public static void clearFolder(File folderPath) {
        if (folderPath == null) {
            return;
        }
        if (!folderPath.isDirectory()) {
            return;
        }

        File[] files = folderPath.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                file.delete();
            } else if (file.isDirectory()) {
                clearFolder(file);
                file.delete();
            }
        }
    }

    /**
     * hide soft keyboard
     *
     * @param activity
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    /**
     * get curent language on device
     *
     * @return Sample: ja, vi, en...
     */
    public static String getCurrentLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * set fullscreen, PLEASE PUT THIS FUNCTION BEFORE " setcontentview() "
     *
     * @param activity
     */
    public static void setFullScreen(Activity activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    /**
     * dip to px conversion.
     *
     * @param context The current Context or Activity that this method is called from
     * @param dp      dips (Density-independent pixels) to convert to pixels.
     * @return value of dp's in pixels for the current screen density.
     */
    public int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    /**
     * dip to px conversion.
     *
     * @param context The current Context or Activity that this method is called from
     * @param px      px to convert to dp.
     * @return value of dps
     */
    public int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    /**
     * Get the "android:versionName" value from the Manifest file.
     *
     * @param context The current Context or Activity that this method is called from
     * @return the application version string, or "Unknown" if versionName cannot be found for
     * the given context.
     */
    public static String getVersionName(Context context) {
        String versionName;
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "Unknown";
        }
        return versionName;
    }

    /**
     * Get the "android:versionCode" value from the Manifest file.
     *
     * @param context The current Context or Activity that this method is called from
     * @return the application version code, or -999 if versionName cannot be found for the given context.
     */
    public static int getVersionCode(Context context) {
        int versionCode;
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = -999;
        }
        return versionCode;
    }

    /**
     * Get application name from Manifest file.
     *
     * @param context The current Context or Activity that this method is called from
     * @return application name.
     */
    public static String getApplicationName(Context context) {
        int stringId = context.getApplicationInfo().labelRes;
        return context.getString(stringId);
    }

    /**
     * check email valid form
     *
     * @param email email to check
     * @return true if email form is valid
     */
    public static boolean isValidEmail(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    /**
     * Uses androids android.telephony.PhoneNumberUtils to check if an phone number is valid.
     *
     * @param number Phone number to check
     * @return true if the <code>number</code> is a valid phone number.
     */
    public final static boolean isValidPhoneNumber(String number) {
        if (number == null) {
            return false;
        } else {
            return PhoneNumberUtils.isGlobalPhoneNumber(number);
        }
    }

    /**
     * Uses androids android.util.Patterns.WEB_URL to check if an url is valid.
     *
     * @param url Address to check
     * @return true if the <code>url</code> is a valid web address.
     */
    public final static boolean isValidURL(String url) {
        if (url == null) {
            return false;
        } else {
            return Patterns.WEB_URL.matcher(url).matches();
        }
    }

    /**
     * Checks to see if the user has rotation enabled/disabled in their phone settings.
     *
     * @param context The current Context or Activity that this method is called from
     * @return true if rotation is enabled, otherwise false.
     */
    public static boolean isRotationEnabled(Context context) {
        return android.provider.Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
    }

    /**
     * Checks to see if the device is connected to a network (cell, wifi, etc).
     *
     * @param context The current Context or Activity that this method is called from
     * @return true if a network connection is available, otherwise false.
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Check if there is any connectivity to a Wifi network.
     * <p/>
     * Can be used in combination with {@link #isConnectedMobile}
     * to provide different features if the device is on a wifi network or a cell network.
     *
     * @param context The current Context or Activity that this method is called from
     * @return true if a wifi connection is available, otherwise false.
     */
    public static boolean isConnectedWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /**
     * Check if there is any connectivity to a mobile network
     * <p/>
     * Can be used in combination with {@link #isConnectedWifi}
     * to provide different features if the device is on a wifi network or a cell network.
     *
     * @param context The current Context or Activity that this method is called from
     * @return true if a mobile connection is available, otherwise false.
     */
    public static boolean isConnectedMobile(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * get unique device id, reuire permission READ_PHONE_STATE
     *
     * @param context
     * @return unique id string
     */
    public static String getDeviceId(Context context) {
        String deviceID = null;

        // TODO: check from pref if null -> generate
        String PREFERENCE_FILE_NAME = "preference_deviceXXX";
        String KEY_PREF = "uiid";

        // don't generate uiid if exist in preference
        String saved_id = (String)getPreferenceValue(context, PREFERENCE_FILE_NAME, KEY_PREF);
        if(saved_id == null || saved_id == ""){
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String id = telephonyManager.getDeviceId();
            // tablet device will return null
            if (id == null || id == "") {
                // maybe change when factory reset
                String id1 = Settings.Secure.ANDROID_ID;
                if (id1 == null || id1 == "") {
                    // last solution is using random string
                    deviceID = UUID.randomUUID().toString();
                } else {
                    deviceID = id1;
                }
            } else {
                deviceID = id;
            }

            // after generate uiid, save for use later
            writePreference(context, PREFERENCE_FILE_NAME, KEY_PREF, deviceID);
        }else {
            deviceID = saved_id;
        }
        return deviceID;
    }

    /**
     * write preference content value
     *
     * @param context
     * @param preferenceName preference file name
     * @param key            key to get value
     * @param value          must be Integer, String, Long, Float, Boolean
     */
    public static void writePreference(Context context, String preferenceName, String key, Object value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit();

        if (value instanceof Integer) {
            editor.putInt(key, Integer.parseInt(value.toString()));
        } else if (value instanceof String) {
            editor.putString(key, value.toString());
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, Boolean.parseBoolean(value.toString()));
        } else if (value instanceof Long) {
            editor.putLong(key, Long.parseLong(value.toString()));
        } else if (value instanceof Float) {
            editor.putFloat(key, Float.parseFloat(value.toString()));
        } else {
            Log.e(context.getPackageName(), "your \"value\" param type not support (must be Integer, String, Long, Float, Boolean)");
            // end now
            return;
        }
        editor.commit();
    }

    /**
     * get preference value
     *
     * @param context
     * @param preferenceName preference file name
     * @param key            key to get value
     * @return Object ( Integer, String, Long, Float, Boolean ), please parse to your type which you want
     */
    public static Object getPreferenceValue(Context context, String preferenceName, String key) {
        SharedPreferences sharePreference = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        return sharePreference.getAll().get(key);
    }
}