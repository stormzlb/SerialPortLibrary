package callback;


/**
 * @author storm
 */
public class SerialPortCallBackUtils {

    public static SerialCallBack mCallBack;

    public static SerialCallBack mVideoCallBack;

    public static SerialCallBack mSettingCallBack;

    public static void setCallBack(SerialCallBack callBack) {
        mCallBack = callBack;
    }

    public static void setVideoCallBack(SerialCallBack callBack) {
        mVideoCallBack = callBack;
    }

    public static void setSettingCallBack(SerialCallBack callBack) {
        mSettingCallBack = callBack;
    }

    public static void doCallBackMethod(byte[] info) {
        if (mCallBack != null) {
            mCallBack.onSerialPortData(info);
        }
        if (mVideoCallBack != null) {
            mVideoCallBack.onSerialPortData(info);
        }
        if (mSettingCallBack != null) {
            mSettingCallBack.onSerialPortData(info);
        }
    }
}