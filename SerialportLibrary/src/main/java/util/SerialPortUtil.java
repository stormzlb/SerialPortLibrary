package util;

import android.util.Log;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;
import callback.SerialPortCallBackUtils;


/**
 * @author storm
 */
public class SerialPortUtil {

    public static String TAG = "SerialPortUtil";

    /**
     * 标记当前串口状态(true:打开,false:关闭)
     **/
    public static boolean isFlagSerial = false;

    public static SerialPort serialPort = null;
    public static InputStream inputStream = null;
    public static OutputStream outputStream = null;
    public static Thread receiveThread = null;

    /**
     * 打开串口
     */
    public static boolean open(String pathname, int baudrate, int flags) {
        boolean isOpen = false;
        if (isFlagSerial) {
            return false;
        }
        try {
            serialPort = new SerialPort(new File(pathname), baudrate, flags);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            receive();
            isOpen = true;
            isFlagSerial = true;
        } catch (IOException e) {
            e.printStackTrace();
            isOpen = false;
        }
        return isOpen;
    }

    /**
     * 关闭串口
     */
    public static boolean close() {
        if (isFlagSerial) {
            return false;
        }
        boolean isClose = false;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            isClose = true;
            isFlagSerial = false;//关闭串口时，连接状态标记为false
        } catch (IOException e) {
            e.printStackTrace();
            isClose = false;
        }
        return isClose;
    }

    /**
     * 发送串口指令
     */
    public static void sendString(String data) {
        if (!isFlagSerial) {
            return;
        }
        try {
            outputStream.write(ByteUtil.hex2byte(data));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendByte(byte[] data) {
        if (!isFlagSerial) {
            return;
        }
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getByteLog(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            String st = String.format("%02X ", data[i]);
            sb.append(st);
        }
        return sb.toString();
    }

    public static String getByteLog(byte[] data, int len) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++) {
            String st = String.format("%02X ", data[i]);
            sb.append(st);
        }
        return sb.toString();
    }

    /**
     * 接收串口数据的方法
     */
    public static void receive() {
        if (receiveThread != null && !isFlagSerial) {
            return;
        }
        receiveThread = new Thread() {
            @Override
            public void run() {
                while (isFlagSerial) {
                    try {
                        byte[] readData = new byte[1024];
                        if (inputStream == null) {
                            return;
                        }
                        int size = inputStream.read(readData);
                        if (size > 0 && isFlagSerial) {
                            write(readData,size);
                            while (read()){
                                if (SerialPortCallBackUtils.mCallBack != null) {
                                    data = new byte[dataLength];
                                    System.arraycopy(caBuffer, 0, data, 0, dataLength);
                                    SerialPortCallBackUtils.doCallBackMethod(data);
                               }
                            };
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveThread.start();
    }

    private static int count = 0; // 已经缓存的数据字节长度
    private static final int SIZE = 1024;
    private static byte[] buffer = new byte[SIZE];
    private static byte[] caBuffer = new byte[SIZE];
    private static byte[] data;
    private static int dataLength = 0;

    public static void write(byte[] b, int length) {
        if (count + length > SIZE) {
            return;
        }
        System.arraycopy(b, 0, buffer, count, length);
        count += length;
    }

    public static boolean read() {
        int pos = 0;
        int data_len = count - pos;
        boolean ret = false;
        while (data_len >= 7) {// 7一帧完整的数据的最小长度
            // 找到起始符并且是一个合法的包
            if (buffer[pos] == (byte) 0xAA) {
                if (isPartialObject(buffer, pos, data_len)) {
                    int object_size = getObjectSize(buffer);
                    if (caBuffer != null) {
                        dataLength = object_size;
                        Log.e(TAG, "cabuffer  = " + getByteLog(caBuffer, data_len));
                        System.arraycopy(buffer, pos, caBuffer, 0, object_size);
                    }
                    pos += object_size;
                    ret = true;
                    break;
                } else {
                    // 如果只收到一个包的部分数据,等待下一包, 组合成完整的数据
                    break;
                }
            }
            pos++;
            data_len = count - pos;
        }

        // 处理了部分数据,把有效数据移动到缓冲区的最前面
        // 这些数据也有可能是垃圾数据,直到下一个起始符,下一次处理会处理掉这些数据.
        System.arraycopy(buffer, pos, buffer, 0, count - pos);
        count -= pos;
        return ret;
    }


    private static boolean isPartialObject(byte[] buffer, int pos, int data_len) {
        int object_size = getObjectSize(buffer);
        if (data_len < object_size) {
            return false;
        }
        return true;
    }

    /**
     * 真实数据长度
     * @param buffer
     * @return
     */
    private static int getObjectSize(byte[] buffer) {
        return buffer[3] & 0xFF;
    }

    public static void sendDATA(int len, byte[] datas) {
        byte[] buff = new byte[len + 4];
        buff[0] = (byte) 0xAA;
        buff[1] = (byte) 0x44;
        buff[2] = (byte) 0x4D;
        System.arraycopy(datas, 0, buff, 3, datas.length);
        for (int i = 0; i < buff.length - 1; i++) {
            buff[buff.length - 1] = (byte) (buff[buff.length - 1] + buff[i]);
        }
        Log.e("TAG", " send: " + getByteLog(buff, buff.length));
        SerialPortUtil.sendByte(buff);
    }

}