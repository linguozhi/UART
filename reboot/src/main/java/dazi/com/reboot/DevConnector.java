package dazi.com.reboot;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by lingz on 17/3/7.
 */

public class DevConnector extends Activity {
    int baudRate = 9600; /* baud rate */
    byte stopBit = 1; /* 1:1stop bits, 2:2 stop bits */
    byte dataBit = 8; /* 8:8bit, 7: 7bit */
    byte parity = 0; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    byte flowControl = 0; /* 0:none, 1: flow control(CTS,RTS) */
    public Context global_context;
    public boolean bConfiged = false;
    public SharedPreferences sharePrefSettings;
    Drawable originalDrawable;
    public String act_string;

    public FT311UARTInterface uartInterface;

    String writeText = "01 05 0f a1 ff 00 de cc";

    int numBytes;
    byte[] writeBuffer = new byte[64];
    byte status;
    byte[] readBuffer = new byte[4096];

    int[] actualNumBytes= new int[1];
    char[] readBufferToChar = new char[4096];
    StringBuffer readSB = new StringBuffer();


    // 无命令指令
    String noOrderText = "01 05 0f a0 00 00 8f 3c";
    // 程序开始时指令1
    String appStartText1 = "01 05 0f a0 ff 00 8f 0c";
    // 程序开始时指令2
    String appStartText2 = "01 05 0f a0 00 00 ce fc";
    // 扫描开始时指令1
    String scanStartText1 = "01 05 0f a1 ff 00 de cc";
    // 扫描开始时指令2
    String scanStartText2 = "01 05 0f a1 00 00 9f 3c";
    // 程序结束时指令1
    String appStopText1 = "01 05 0f a2 ff 00 2e cc";
    // 程序结束时指令2
    String appStopText2 = "01 05 0f a2 00 00 6f 3c";


    public handler_thread handlerThread;

    /** Called when the activity is first created. */


    public DevConnector(Context parentContext, SharedPreferences sharePrefSettings) {
        this.global_context = parentContext;
        this.sharePrefSettings = sharePrefSettings;
        uartInterface = new FT311UARTInterface(global_context, sharePrefSettings);


        handlerThread = new handler_thread(handler);
        handlerThread.start();

    }

    public void savePreference() {
        if(true == bConfiged){
            sharePrefSettings.edit().putString("configed", "TRUE").commit();
            sharePrefSettings.edit().putInt("baudRate", baudRate).commit();
            sharePrefSettings.edit().putInt("stopBit", stopBit).commit();
            sharePrefSettings.edit().putInt("dataBit", dataBit).commit();
            sharePrefSettings.edit().putInt("parity", parity).commit();
            sharePrefSettings.edit().putInt("flowControl", flowControl).commit();
        }
        else{
            sharePrefSettings.edit().putString("configed", "FALSE").commit();
        }
    }


    public void restorePreference() {
        String key_name = sharePrefSettings.getString("configed", "");
        if(true == key_name.contains("TRUE")){
            bConfiged = true;
        }
        else{
            bConfiged = false;
        }

        baudRate = sharePrefSettings.getInt("baudRate", 9600);
        stopBit = (byte)sharePrefSettings.getInt("stopBit", 1);
        dataBit = (byte)sharePrefSettings.getInt("dataBit", 8);
        parity = (byte)sharePrefSettings.getInt("parity", 0);
        flowControl = (byte)sharePrefSettings.getInt("flowControl", 0);

    }

    public void cleanPreference(){
        SharedPreferences.Editor editor = sharePrefSettings.edit();
        editor.remove("configed");
        editor.remove("baudRate");
        editor.remove("stopBit");
        editor.remove("dataBit");
        editor.remove("parity");
        editor.remove("flowControl");
        editor.commit();
    }


    public void linkDev() {
        if(false == bConfiged){
            bConfiged = true;
            uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
            savePreference();
        }

    }

    // 初始化设备：执行当前步骤后，设备唤醒，蜂鸣器响
    public boolean wakeupDev() {
        linkDev();

        // order1
        writeData(appStartText1);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        // order2
        writeData(appStartText2);

        return true;
    }

    /**
     * 机器人扫描动作
     * @return
     */
    public boolean scan() {
        // order1
        writeData(scanStartText1);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        // order2
        writeData(scanStartText2);

        return true;
    }

    /**
     * 机器人休眠指令
     * @return
     */
    public boolean sleepDev() {
        // order1
        writeData(appStopText1);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

//        status = uartInterface.ReadData(4096, readBuffer,actualNumBytes);
//
//        if (status != 0x00 || actualNumBytes[0] < 1 ) {
//            msgToast("设备休眠失败,指令1发送失败,status:" + status + ",numByte:"+actualNumBytes[0], Toast.LENGTH_LONG);
//            return false;
//        }

        // order2
        writeData(appStopText2);

//        status = uartInterface.ReadData(4096, readBuffer,actualNumBytes);
//
//        if (status != 0x00 || actualNumBytes[0] < 1 ) {
//            msgToast("设备休眠失败,指令2发送失败,status:" + status + ",numByte:"+actualNumBytes[0], Toast.LENGTH_LONG);
//            return false;
//        }

        return true;
    }

    void msgToast(String str, int showTime)
    {
        Toast.makeText(global_context, str, showTime).show();
    }

    public void writeData(String data)
    {
        String srcStr = data;
        String destStr = "";

        String[] tmpStr = srcStr.split(" ");

        for(int i = 0; i < tmpStr.length; i++)
        {
            if(tmpStr[i].length() == 0)
            {
                msgToast("Incorrect input for HEX format."
                        +"\nThere should be only 1 space between 2 HEX words.",Toast.LENGTH_SHORT);
                return;
            }
            else if(tmpStr[i].length() != 2)
            {
                msgToast("Incorrect input for HEX format."
                        +"\nIt should be 2 bytes for each HEX word.",Toast.LENGTH_SHORT);
                return;
            }
        }

        try
        {
            destStr = HexUtil.hexToAscii(srcStr.replaceAll(" ", ""));
        }
        catch(IllegalArgumentException e)
        {
            msgToast("Incorrect input for HEX format."
                    +"\nAllowed charater: 0~9, a~f and A~F",Toast.LENGTH_SHORT);
            return;
        }


        numBytes = destStr.length();
        for (int i = 0; i < numBytes; i++) {
            writeBuffer[i] = (byte)destStr.charAt(i);
        }
        uartInterface.SendData(numBytes, writeBuffer);

    }
    /* usb input data handler */
    private class handler_thread extends Thread {
        Handler mHandler;

        /* constructor */
        handler_thread(Handler h) {
            mHandler = h;
        }

        public void run() {
            Message msg;

            while (true) {

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }

                status = uartInterface.ReadData(4096, readBuffer,actualNumBytes);

                if (status == 0x00 && actualNumBytes[0] > 0) {
                    msg = mHandler.obtainMessage();
                    mHandler.sendMessage(msg);
                } else {
                    msgToast("device return failed, status:" + status + ",actualNumBytes[0]:" + actualNumBytes[0], Toast.LENGTH_SHORT);
                    Log.e("device return failed", "status:" + status + ",actualNumBytes[0]:" + actualNumBytes[0]);
                }

            }
        }
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            for(int i=0; i<actualNumBytes[0]; i++)
            {
                readBufferToChar[i] = (char)readBuffer[i];
            }
            appendData(readBufferToChar, actualNumBytes[0]);
        }
    };

    public void appendData(char[] data, int len)
    {
        readSB.setLength(0);
        if(len >= 1)
            readSB.append(String.copyValueOf(data, 0, len));

            char[] ch = readSB.toString().toCharArray();
            String temp;
            StringBuilder tmpSB = new StringBuilder();
            for(int i = 0; i < ch.length; i++)
            {
                temp = String.format("%02x", (int) ch[i]);

                if(temp.length() == 4)
                {
                    tmpSB.append(temp.substring(2, 4));
                }
                else
                {
                    tmpSB.append(temp);
                }

                if(i+1 < ch.length)
                {
                    tmpSB.append(" ");
                }
            }

        Log.i("device return:" , tmpSB.toString());
//            msgToast("res:" + tmpSB.toString(), Toast.LENGTH_SHORT);
            tmpSB.delete(0, tmpSB.length());
    }

    public int ResumeAccessory() {
        int res = uartInterface.ResumeAccessory();
        if (res == 2) {
            cleanPreference();
            restorePreference();
        }
        return res;
    }

    public void DestroyAccessory() {
        uartInterface.DestroyAccessory(bConfiged);
    }

    public void setConfig() {
        uartInterface.SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
    }
}
