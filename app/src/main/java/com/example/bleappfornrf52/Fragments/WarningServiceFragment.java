/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.example.bleappfornrf52.Fragments;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.example.bleappfornrf52.R;
import com.example.bleappfornrf52.MainActivity;

import java.util.Arrays;
import java.util.UUID;

public class WarningServiceFragment extends ServiceFragment {


    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //        변수 선언
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int MIN_UINT = 0;
    private static final int MAX_UINT8 = (int) Math.pow(2, 8) - 1;
    private static final int MAX_UINT16 = (int) Math.pow(2, 16) - 1;
    private static final int MAX_UINT32 = (int) Math.pow(2, 32) - 1;
    /**
     * See <a href="https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.health_thermometer.xml">
     * Health Thermometer Service</a>
     * This service exposes two characteristics with descriptors:
     * - Measurement Interval Characteristic:
     * - Listen to notifications to from which you can subscribe to notifications
     * - CCCD Descriptor:
     * - Read/Write to get/set notifications.
     * - User Description Descriptor:
     * - Read/Write to get/set the description of the Characteristic.
     * - Temperature Measurement Characteristic:
     * - Read value to get the current interval of the temperature measurement timer.
     * - Write value resets the temperature measurement timer with the new value. This timer
     * is responsible for triggering value changed events every "Measurement Interval" value.
     * - CCCD Descriptor:
     * - Read/Write to get/set notifications.
     * - User Description Descriptor:
     * - Read/Write to get/set the description of the Characteristic.
     */
    private static final int INITIAL_SEND = 0;
    private static final int INITIAL_RECEIVE = 0;
    //이게 프라이머리 서비스
    private static final UUID UART_SERVICE_UUID = UUID
            .fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");

    /**
     * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.temperature_measurement.xml">
     * Temperature Measurement</a>
     */

    //이건 TxChar UUID 설정 부분 (보내는 Char)
    private static final UUID SEND_UUID = UUID
            .fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");  //RxChar UUID
    private static final int SEND_VALUE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;
    private static final String SEND_DESCRIPTION = "This characteristic is used " +
            "as TxChar Nordic Uart device";


    //이건 RxChar UUID 설정 부분 (받아오는 Char)
    /**
     * See <a href="https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.measurement_interval.xml">
     * Measurement Interval</a>
     */
    private static final UUID RECIEVE_UUID = UUID
            .fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");  //TxChar UUID
    private static final int RECEIVE_VALUE_FORMAT = BluetoothGattCharacteristic.FORMAT_UINT8;


    private static final String RECEIVE_DESCRIPTION = "This characteristic is used " +
            "as RxChar of Nordic Uart device";

    Vibrator vibrator = null;
    MediaPlayer player = null;
    public Thread triggerService = null;
    Animation anim = null;

    public static int alert_value1 = 0;
    int alert_value2 = 0;
    int alert_value3 = 0;
    int alert_value1_temp = 0;
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //       텍스트 뷰, 클릭 리스너 설정 ( 사용자 상호작용 설정 )
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////


//  private BluetoothGattService mNordicUartService;
//  private BluetoothGattCharacteristic mSendCharacteristic;
//  private BluetoothGattCharacteristic mReceiveCharacteristic;
//  private BluetoothGattDescriptor mReceiveCCCDescriptor;


    private ServiceFragmentDelegate mDelegate;

    //원래있던 TemperatureMeasurement를 Send로 바꿔줌.
    private EditText mEditTextSendValue;
    private TextView mTextViewReceiveValue1;
    private TextView mTextViewReceiveValue2;
    private TextView mTextViewReceiveValue3;
    private TextView mTextViewDistanceValue1;
    private TextView mTextViewDistanceValue2;
    private TextView mTextViewDistanceValue3;

    //이건 Text Editor에 수정을 할 시에 그걸 가지고 보낼 값(Characteristic Value)을 바꾸는 것.
    private final OnEditorActionListener mOnEditorActionListenerSend = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String newSENDValueString = textView.getText().toString();
                if (isValidCharacteristicValue(newSENDValueString,
                        SEND_VALUE_FORMAT)) {
//          //첫번째, String 형식을 byte 형식으로 바꾸기
//          byte[] newSENDbytes = mEditTextSendValue.getText().toString().getBytes(StandardCharsets.US_ASCII);
//          mSendCharacteristic.setValue(newSENDbytes);
                    //두번째. int로 바꾸기
                    int newSendValue = Integer.parseInt(newSENDValueString);

                    MainActivity.mSendCharacteristic.setValue(newSendValue,
                            SEND_VALUE_FORMAT,
                            /* offset */ 0);
                } else {
                    Toast.makeText(getActivity(), "Chracteristic 형식이 틀립니다.",
                            Toast.LENGTH_SHORT).show();
                    Log.v(TAG, "Charicteristic 형식 변경 필요: " + newSENDValueString);
                }
            }
            return false;
        }
    };

    //이건 사실 필요없음. TxChar값이 들어가는 건 EditText가 아닌 입력 불가능한 TextView라서 굳이 리스닝 해줄 필요 없기 떄문.
    private final OnEditorActionListener mOnEditorActionListenerReceive = new OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String newReceiveValueString = textView.getText().toString();
                if (isValidCharacteristicValue(newReceiveValueString,
                        RECEIVE_VALUE_FORMAT)) {
                    int newReceiveValue = Integer.parseInt(newReceiveValueString);
                    MainActivity.mReceiveCharacteristic.setValue(newReceiveValue,
                            RECEIVE_VALUE_FORMAT,
                            /* offset */ 1);
                } else {
                    Toast.makeText(getActivity(), "Chracteristic 형식이 틀립니다.",
                            Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        }
    };


    private static final String TAG = MainActivity.class.getCanonicalName();
    //이 부분은 Send하는 부분이라 Warning에서는 주석처리함.
//  //이건 Notify 버튼 즉 Send 버튼을 리스닝 해주는 함수
//  private final View.OnClickListener mNotifyButtonListener = new View.OnClickListener() {
//    @Override
//    public void onClick(View v) {
//
////      //첫번째 방법, String 형식을 byte 형식으로 바꿔서 보내기
////      byte[] newSENDbytes = mEditTextSendValue.getText().toString().getBytes(StandardCharsets.US_ASCII);
////      mSendCharacteristic.setValue(newSENDbytes);
//
//      //두번째 방법, Integer로 보내기([값]형태)
//      int integer_to_send = Integer.parseInt(mEditTextSendValue.getText().toString());
//      mSendCharacteristic.setValue(integer_to_send,
//              SEND_VALUE_FORMAT,
//              /* offset */ 0);
//
//      //★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
//      //정확히는 여기에서 NOTIFICATION을 SEND 해준다. (TxChar을 통해서)
//      //★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
//      mDelegate.sendNotificationToDevices(mSendCharacteristic);
//      //Log.v(TAG, "sent: " + Arrays.toString(mSendCharacteristic.getValue()) + " / that is: " + bytesToString(mSendCharacteristic.getValue()));
//      Log.v(TAG, "sent: " + Arrays.toString(mSendCharacteristic.getValue()));
//    }
//  };


    private final View.OnClickListener mAlertStopButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //알람 정지

            if (MainActivity.alert_mode == 1){
                alert_stop_special();
                alert_value1_temp = alert_value1;
                getView().setBackgroundColor(Color.TRANSPARENT);
            }
        }
    };


  /*
  원래있던 MeasurementInterval을 ReceiveValue로 바꿔줌
  BLUETOOTH GATT 다루는 부분은 아래 사이트 참고
  https://developer.android.com/reference/android/bluetooth/BluetoothGattCharacteristic#setValue(int,%20int,%20int)
  sendValue getValue 등 쓸 수 있는 함수들 인자 관련 설명 있음.
  */

    public WarningServiceFragment() {

//    //이거는 Send
//    mSendCharacteristic =
//            new BluetoothGattCharacteristic(SEND_UUID,
//                    BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_READ,
//                    /* No permissions */ BluetoothGattCharacteristic.PERMISSION_READ);
//
//    mSendCharacteristic.addDescriptor(
//            WarningActivity.getClientCharacteristicConfigurationDescriptor());
//
//    mSendCharacteristic.addDescriptor(
//            WarningActivity.getCharacteristicUserDescriptionDescriptor(SEND_DESCRIPTION));
//
//    //이거는 Receive
//    mReceiveCharacteristic =
//            new BluetoothGattCharacteristic(
//                    RECIEVE_UUID,
//                    BluetoothGattCharacteristic.PROPERTY_WRITE,
//                    BluetoothGattCharacteristic.PERMISSION_WRITE);
//
//    mReceiveCharacteristic.addDescriptor(WarningActivity.getClientCharacteristicConfigurationDescriptor());
//
//    mReceiveCharacteristic.addDescriptor(
//            WarningActivity.getCharacteristicUserDescriptionDescriptor(RECEIVE_DESCRIPTION));
//
//    mNordicUartService = new BluetoothGattService(UART_SERVICE_UUID,
//            BluetoothGattService.SERVICE_TYPE_PRIMARY);
//    mNordicUartService.addCharacteristic(mSendCharacteristic);
//    mNordicUartService.addCharacteristic(mReceiveCharacteristic);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //        여기는 ON CREATE VIEW 등등 시작화면 관련 설정
    //  https://developer.android.com/reference/android/app/Fragment 참고
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_warning, container, false);
//    mEditTextSendValue = (EditText) view
//            .findViewById(R.id.EditText_Sendvalue);
//    mEditTextSendValue
//            .setOnEditorActionListener(mOnEditorActionListenerSend);
        mTextViewReceiveValue1 = (TextView) view
                .findViewById(R.id.Textview_Recievevalue_a1);
        mTextViewReceiveValue2 = (TextView) view
                .findViewById(R.id.Textview_Recievevalue_a2);
        mTextViewReceiveValue3 = (TextView) view
                .findViewById(R.id.Textview_Recievevalue_a3);
        mTextViewDistanceValue1 = (TextView) view
                .findViewById(R.id.Textview_distance_danger);
        mTextViewDistanceValue2 = (TextView) view
                .findViewById(R.id.Textview_distance_warning);
        mTextViewDistanceValue3 = (TextView) view
                .findViewById(R.id.Textview_distance_safe);
        ImageView imageView3 = (ImageView) view.findViewById(R.id.imageView3);
        ViewCompat.setTranslationZ(imageView3, -1f); // Z축을 -1로 설정하여 뒤로 보냄
        //여기서 Editor 리스너를 써줬기는 한데 여기선 사실상 필요가 없다.(EditText 대신 TextView 써줘서 ㅇㅇ)
//    mTextViewReceiveValue1
//            .setOnEditorActionListener(mOnEditorActionListenerReceive);
//    Button notifyButton = (Button) view.findViewById(R.id.button_poweron);
        Button alertStopButton = (Button) view.findViewById(R.id.button_AlertSTOP);
        alertStopButton.setOnClickListener(mAlertStopButtonListener);
//    notifyButton.setOnClickListener(mNotifyButtonListener);
//    setSendValue(INITIAL_SEND, INITIAL_RECEIVE);
        byte[] value = {0,0,0,0,0,0,0,0};
        //int[] members = classification(value);
//        int value1 = members[0];
//        int value2 = members[1];
//        int value3 = members[2];
        //두번째 방법, Integer로 받기([값]형태)
        //mTextViewReceiveValue.setText(Arrays.toString(value));

        mTextViewReceiveValue1.setText("0");
        mTextViewReceiveValue2.setText("0");
        mTextViewReceiveValue3.setText("0");
        mTextViewDistanceValue1.setText(MainActivity.distance_setting_value1);
        mTextViewDistanceValue2.setText(MainActivity.distance_setting_value2);
        mTextViewDistanceValue3.setText(MainActivity.distance_setting_value3);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mDelegate = (ServiceFragmentDelegate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ServiceFragmentDelegate");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDelegate = null;
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onPause(){
        super.onPause();
    }
    @Override
    public void onResume(){
        super.onResume();
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //        화면설정 끝
    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public BluetoothGattService getBluetoothGattService() {
        return MainActivity.mBluetoothGattService;
    }

    @Override
    public ParcelUuid getServiceUUID() {
        return new ParcelUuid(UART_SERVICE_UUID);
    }

    //이건 처음에 onCreate에서 값 세팅 해주는 것. 그냥 시작에만 한번 딱 설정해준다.
    private void setSendValue(int SendValue, int ReceiveValue) {

        /* Set the org.bluetooth.characteristic.temperature_measurement
         * characteristic to a byte array of size 5 so
         * we can use setValue(value, format, offset);
         *
         * Flags (8bit) + Temperature Measurement Value (float) = 5 bytes
         *
         * Flags:
         *   Temperature Units Flag (0) -> Celsius
         *   Time Stamp Flag (0) -> Time Stamp field not present
         *   Temperature Type Flag (0) -> Temperature Type field not present
         *   Unused (00000)
         */

        //보낼 값이니까 Send Characteristic(TxChar)의 value 값을 변경해주는데 byte array형식으로 집어넣는다.
        //이건 앞으로 (uint8형식으로 넣는다는 뜻) flag를 8(uint8)로 맞춰주는 것.
        //mSendCharacteristic.setValue(new byte[]{0b00001000, 0, 0, 0});
        MainActivity.mSendCharacteristic.setValue(new byte[]{0});
        //mReceiveCharacteristic.setValue(new byte[]{0b00001000, 0, 0, 0});
        MainActivity.mReceiveCharacteristic.setValue(new byte[]{0});

        // Characteristic Value: [flags, 0, 0, 0]


        MainActivity.mSendCharacteristic.setValue(SendValue,
                SEND_VALUE_FORMAT,
                /* offset */ 1);

        MainActivity.mReceiveCharacteristic.setValue(ReceiveValue,
                RECEIVE_VALUE_FORMAT,
                /* offset */ 1);
        // Characteristic Value: [flags, heart rate value, 0, 0]
        mEditTextSendValue.setText(Integer.toString(SendValue));
        mTextViewReceiveValue1.setText(Integer.toString(ReceiveValue));
        //여기서 보낼 값으로 에딧 텍스트 값도 변경해줌
    }


    //★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    //이건 Receive 값 받아오는 함수(Write 권한 있는 RxChar)
    //★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    @Override
    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value) {
        if (offset != 0) {
            return BluetoothGatt.GATT_INVALID_OFFSET;
        }
        // Heart Rate control point is a 8bit characteristic
        //글자수 제한인데 원래 1글자만 받던거 풀어줌.
        if (value.length > 1000) {
            return BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
        }
        else if(value.length == 20) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
//        //첫번째 방법, byte 형식을 String 형식으로 바꿔서 받기
//        //이 부분에서 아스키 코드로 된 btye array를 string으로 변환해줌
//        mTextViewReceiveValue.setText(bytesToString(value));
//        //로그에서 원래 아스키코드 배열과 / 변환되어 나온 string값을 보여줌
//        Log.v(TAG, "Received: " + Arrays.toString(value) + " / converted into:" + bytesToString(value));
                    //여기서 이제 Value를 [태그1, 위험정보, 태그2, 위험정보, 태그3, 위험정보, 태그4, 위험정보, 위험, 경고, 접근] 로 받기로 했으므로,
                    int[] members = classification(value);
                    alert_value1 = members[0];
                    alert_value2 = members[1];
                    alert_value3 = members[2];
//                MainActivity.distance_setting_value1 = Integer.toString(Byte.toUnsignedInt(value[8])) + "m";
//                MainActivity.distance_setting_value2 = Integer.toString(Byte.toUnsignedInt(value[9])) + "m";
//                MainActivity.distance_setting_value3 = Integer.toString(Byte.toUnsignedInt(value[10])) + "m";
                    Log.v(TAG, "members are: " + alert_value1 + ", " + alert_value2 + ", " + alert_value3);
                    //두번째 방법, Integer로 받기([값]형태)
                    //mTextViewReceiveValue.setText(Arrays.toString(value));

                    mTextViewReceiveValue1.setText(String.valueOf(alert_value1));
                    mTextViewReceiveValue2.setText(String.valueOf(alert_value2));
                    mTextViewReceiveValue3.setText(String.valueOf(alert_value3));
                    mTextViewDistanceValue1.setText(MainActivity.distance_setting_value1);
                    mTextViewDistanceValue2.setText(MainActivity.distance_setting_value2);
                    mTextViewDistanceValue3.setText(MainActivity.distance_setting_value3);

                    //위험반경 내부로 들어간 작업자기 생기면 알림
                    if (alert_value1 > 0 && MainActivity.alert_mode == 0) {
                        //진동 및 알림
                        alert();
                    }
                    //위험반경 내부에 있지 않게 되면 알람을 정지
                    else if (alert_value1 == 0 && MainActivity.alert_mode == 1) {
                        alert_stop();
                        getView().setBackgroundColor(Color.TRANSPARENT);
                    } else if (MainActivity.alert_mode == 3) {
                        if (alert_value1 > alert_value1_temp)
                            alert();
                        else if (alert_value1 < alert_value1_temp) {
                            alert_value1_temp = alert_value1;
                        }
                    }

                    //로그에서 원래 아스키코드 배열과 / 변환되어 나온 string값을 보여줌
                    Log.v(TAG, "Received: " + Arrays.toString(value));
                }
            });
        }
        return BluetoothGatt.GATT_SUCCESS;
    }

    // 노티피케이션을 쓸 수 있게 되면 앱 푸시 알림
    @Override
    public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
        if (characteristic.getUuid() != SEND_UUID) {
            Log.v(TAG, "UUID가 SendUUID와 다릅니다: " + characteristic.getUuid());
            return;
        }
        if (indicate) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
//        int newSENDValueString = Integer.parseInt(mEditTextSendValue.getText().toString());
//
//        mSendCharacteristic.setValue(newSENDValueString,
//                SEND_VALUE_FORMAT,
//                /* offset */ 1);
                Toast.makeText(getActivity(), R.string.notificationsEnabled, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    //노티피케이션을 못 쓰게 되면 앱 푸시 알림
    @Override
    public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid() != SEND_UUID) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), R.string.notificationsNotEnabled, Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    // 유효한 특성 값인지 알아내는 함수. 예를 들어 String값을 보낸다면
    // 그게 하나하나 파싱 했을 때 저 비트 안에 들어가는지 ㅇㅇ
    private boolean isValidCharacteristicValue(String s, int format) {
        try {
            int value = Integer.parseInt(s);
            if (format == BluetoothGattCharacteristic.FORMAT_UINT8) {
                return (value >= MIN_UINT) && (value <= MAX_UINT8);
            } else if (format == BluetoothGattCharacteristic.FORMAT_UINT16) {
                return (value >= MIN_UINT) && (value <= MAX_UINT16);
            } else if (format == BluetoothGattCharacteristic.FORMAT_FLOAT) {
                return(value >=MIN_UINT) && (value <= MAX_UINT32);
            } else {
                throw new IllegalArgumentException(format + " is not a valid argument");
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    //이건 그냥 btye array에서 Hex값으로 바꿔주는 함수(안 씀)
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    /*
    이건 string to ASCII. Notify 할 때 쓸 수 있음. string을 editText에 입력 받으면
    그걸 다시 ASCII로 변환한 다음에 그걸 전송하고
    */
    public String bytesToString(byte[] value) {
        String converted = "";
        for (int i : value) {
            converted = converted.concat(Character.toString((char) i));
        }
        return converted;
    }

    public int[] classification(byte[] value){
        //테스트값: 4, 9, 16, 18
        //0001010202030303010203
        //0002010202030303010203
        int[] xyzlocation = {0,0,0};

        //원래는 이렇게 x,y,z 를 "x:","y:","z:" 으로 구분하였음.. 엄청 비효율적.
        int[] member_for_distances = {0, 0, 0};
//
//        byte[] x_mag = new byte[value.length-2];
//        byte[] y_mag = new byte[value.length-2];
//        byte[] z_mag = new byte[value.length-2];

//        for(int i = 0; i < value.length; i++ ){
//            if(value[0] == 0x78){
//                System.arraycopy(value, 2, x_mag, 0, value.length-2);
//                MainActivity.magnetic_value_x = bytesToString(x_mag);
//                MainActivity.mMagValue_x.setText(MainActivity.magnetic_value_x);
//                Log.v(TAG, "Magnetic X is: " + MainActivity.magnetic_value_x);
//            }else if(value[i] == 0x79){
//                System.arraycopy(value, 2, y_mag, 0, value.length-2);
//                MainActivity.magnetic_value_y = bytesToString(y_mag);
//                MainActivity.mMagValue_y.setText(MainActivity.magnetic_value_y);
//                Log.v(TAG, "Magnetic Y is: " + MainActivity.magnetic_value_y);
//            }else if(value[i] == 0x7A){
//                System.arraycopy(value, 2, z_mag, 0, value.length-2);
//                MainActivity.magnetic_value_z = bytesToString(z_mag);
//                MainActivity.mMagValue_z.setText(MainActivity.magnetic_value_z);
//                Log.v(TAG, "Magnetic Z is: " + MainActivity.magnetic_value_z);
//            }
//        }
        float[] x = new float[3];
        float[] y = new float[3];
        int index = 0;

        for (int i = 0; i < 3; i++) {
            short decompressed = (short)((value[index++] << 8) | (value[index++] & 0xFF));
            x[i] = (float)(decompressed / 100.0); // 소수점을 복원
        }
        for (int i = 0; i < 3; i++) {
            short decompressed = (short)((value[index++] << 8) | (value[index++] & 0xFF));
            y[i] = (float)(decompressed / 100.0); // 소수점을 복원
        }

        // 첫 번째 자력계에서의 자기장 벡터 크기 계산
        float B1 = calculateMagnitude(x[0], x[1], x[2]);
        // 두 번째 자력계에서의 자기장 벡터 크기 계산
        float B2 = calculateMagnitude(y[0], y[1], y[2]);
        // 두 자력계의 평균 자기장 크기 계산
        float Magnitude = (B1 + B2) / 2.0f;
        Log.v(TAG, "Magnitude = " + Magnitude);
        MainActivity.magnetic_value_x = String.valueOf(x[0]);
        MainActivity.magnetic_value_y = String.valueOf(x[1]);
        MainActivity.magnetic_value_z = String.valueOf(x[2]);
        MainActivity.magnetic_value_x2 = String.valueOf(y[0]);
        MainActivity.magnetic_value_y2 = String.valueOf(y[1]);
        MainActivity.magnetic_value_z2 = String.valueOf(y[2]);
        MainActivity.mMagValue_x.setText(MainActivity.magnetic_value_x);
        MainActivity.mMagValue_y.setText(MainActivity.magnetic_value_y);
        MainActivity.mMagValue_z.setText(MainActivity.magnetic_value_z);

        Log.v(TAG, "Mag1: x- " + MainActivity.magnetic_value_x + " y-" + MainActivity.magnetic_value_y + " z-" + MainActivity.magnetic_value_z);
        Log.v(TAG, "Mag2: x- " + MainActivity.magnetic_value_x2 + " y-" + MainActivity.magnetic_value_y2 + " z-" + MainActivity.magnetic_value_z2);
        //위험반경인 사람 수
        if(Magnitude > 150){
            member_for_distances[0]++;
        }
        //경고반경인 사람 수
        else if(Magnitude > 100){
            member_for_distances[1]++;
        }
        //접근반경인 사람 수;
        else if(Magnitude > 80){
            member_for_distances[2]++;
        }
        return member_for_distances;
    }
    @Override
    public void SendDisconnection(){
        byte[] disconnectionValue = {99};
        MainActivity.mSendCharacteristic.setValue(disconnectionValue);
        mDelegate.sendNotificationToDevices(MainActivity.mSendCharacteristic);
        Log.v(TAG, "sent disconnetionValue: " + Arrays.toString(disconnectionValue));
    }
    public static float calculateMagnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

}
