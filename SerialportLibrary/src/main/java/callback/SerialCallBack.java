package callback;

/**
 * @author storm
 */
public interface SerialCallBack {

    /**
     * @param serialPortData
     */
    void onSerialPortData(byte[] serialPortData);
}