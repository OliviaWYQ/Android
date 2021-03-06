/**
 * Copyright (C) 2015 Iasc CHEN
 * Created on 15/4/27.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.iasc.microduino.bluejoypad;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * These commands is compatible
 */
public class JoypadCommand {
    private final static String TAG = JoypadCommand.class.getSimpleName();

    static final byte[] CMD_HEAD = {0x24, 0x4d, 0x3c};
    static final byte CMD_CODE = (byte) 0xc8;

    static final byte CHANNEL_COUNT = 12;
    static final byte CHANNEL_LEN = CHANNEL_COUNT * 2; // CHANNEL_COUNT * Short.SIZE / Byte.SIZE;
    static final byte CMD_LEN = 6 + CHANNEL_LEN;

    public static final int LR = 0, FB = 1, ROTATE = 2, POWER = 3, BU = 7,GP = 6,
            LAT1 = 4, LAT2 = 5, LAT3 = 8, LON1 = 9, LON2 = 10, LON3 = 11 ;


    static short[] channel = {1500, 1500, 1000, 1500, 1500, 1500, 1500, 1500, 1500, 1500, 1500, 1500};
    public static short[] UNLOCK_CMD = {1500, 1500, 2000, 1000, 1500, 1500, 1500, 1500, 1500, 1500, 1500, 1500};
    public static short[] NORMAL_CMD = {1500, 1500, 1500, 1150, 1500, 1500, 1500, 1500, 1500, 1500, 1500, 1500};
    public static short[] DOWN_CMD = {1500, 1500, 1500, 1200, 1000, 1500, 1500, 1500, 1500, 1500, 1500, 1500};
    public static short[] LOCK_CMD = {1500, 1500, 1000, 1000, 1500, 1500, 1500, 1500, 1500, 1500, 1500, 1500};

    /*
    public static short[] channel = {1500, 1500, 1000, 1500, 1000, 1000, 1000, 1000};
    public static short[] UNLOCK_CMD = {1500, 1500, 2000, 1000, 1500, 1500, 1500, 1500};
    public static short[] NORMAL_CMD = {1500, 1500, 1500, 1150, 1000, 1000, 1000, 1000};
    public static short[] DOWN_CMD = {1500, 1500, 1500, 1200, 1000, 1000, 1000, 1000};
    public static short[] LOCK_CMD = {1500, 1500, 1000, 1000, 1500, 1500, 1500, 1500};
    */

    public static byte[] compose() {

        ByteBuffer bbuffer = ByteBuffer.allocate(CMD_LEN);
        bbuffer.order(ByteOrder.LITTLE_ENDIAN);

        bbuffer.put(CMD_HEAD);
        bbuffer.put(CHANNEL_LEN);
        bbuffer.put(CMD_CODE);

        ByteBuffer bb = ByteBuffer.allocate(CHANNEL_LEN);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            bb.putShort(channel[i]);
        }
        bbuffer.put(bb.array());

        bbuffer.put(getChecksum(CHANNEL_LEN, CMD_CODE, bb.array()));

        // Log.v("BBuffer", "power = " + channel[POWER] + ", Rotate = " + channel[ROTATE]);
        Log.v("BBuffer", "" + byteArrayToHexString(bbuffer.array()));

        return bbuffer.array();
    }

    public static byte getChecksum(byte length, byte cmd, byte mydata[]) {
        byte checksum = 0;
        checksum ^= (length & 0xFF);
        checksum ^= (cmd & 0xFF);
        for (int i = 0; i < length; i++)
            checksum ^= (mydata[i] & 0xFF);
        return checksum;
    }

    public static void resetChannel(short[] cmd) {
        for (int i = 0; i < CHANNEL_COUNT; i++) {
            channel[i] = cmd[i];
        }
    }

    public static short minusPower() {
        int value = channel[POWER] - 100;

        if (value > 1000)
            channel[POWER] = (short) value;
        else
            channel[POWER] = 1000;

        return channel[POWER];
    }

    public static short stopPower() {
        channel[POWER] = 1000;
        return channel[POWER];
    }

    public static short useBU() {
        channel[BU] = 2000;
        return channel[BU];
    }

    public static short stopBU() {
        channel[BU] = 1000;
        return channel[BU];
    }

    public static void setGPS() {
        DeviceControlActivity DeviceControlActivity = new DeviceControlActivity();
        //channel[LAT1] = (short) DeviceControlActivity.LatValue1;
        //channel[LAT2] = (short) DeviceControlActivity.LatValue2;
        //channel[LAT3] = (short) DeviceControlActivity.LatValue3;
        //channel[LON1] = (short) DeviceControlActivity.LonValue1;
        //channel[LON2] = (short) DeviceControlActivity.LonValue2;
        //channel[LON3] = (short) DeviceControlActivity.LonValue3;
    }

    public static void initGPS() {
        DeviceControlActivity DeviceControlActivity = new DeviceControlActivity();
        DeviceControlActivity.LatValue = 0;
        DeviceControlActivity.LonValue = 0;
        channel[LAT1] = 0;
        channel[LAT2] = 0;
        channel[LAT3] = 0;
        channel[LON1] = 0;
        channel[LON1] = 0;
        channel[LON2] = 0;
        channel[LON3] = 0;
    }

    public static short useGP(){
        channel[GP] = 2000;
        return channel[GP];
    }

    public static short stopGP(){
        channel[GP] = 1000;
        return channel[GP];
    }


    public static void changeChannel(int index, int value) {
        channel[index] = (short) value;
    }

    public static String byteArrayToHexString(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
