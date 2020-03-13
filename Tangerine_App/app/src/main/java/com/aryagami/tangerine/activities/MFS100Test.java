package com.aryagami.tangerine.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.aryagami.R;
import com.aryagami.data.Constants;
import com.aryagami.data.DataModel;
import com.aryagami.data.RegistrationData;
import com.aryagami.data.ScanData;
import com.aryagami.data.UserLogin;
import com.aryagami.data.UserRegistration;
import com.aryagami.restapis.RestServiceHandler;
import com.aryagami.util.BugReport;
import com.aryagami.util.MyToast;
import com.aryagami.util.ProgressDialogUtil;
import com.aryagami.util.ReDirectToParentActivity;
import com.gemalto.wsq.WSQEncoder;
import com.mantra.mfs100.FingerData;
import com.mantra.mfs100.MFS100;
import com.mantra.mfs100.MFS100Event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class MFS100Test extends Activity implements MFS100Event {

    Button btnInit;
    Button btnUninit;
    Button btnSyncCapture;
    Button btnStopCapture;
    Button btnMatchISOTemplate;
    Button btnExtractISOImage;
    Button btnExtractAnsi;
    Button btnClearLog;
    Button btnExtractWSQImage;
    TextView lblMessage;
    EditText txtEventLog;
    private ImageView imgFinger;
    CheckBox cbFastDetection;
    TextView dismsg;
    private static long mLastClkTime = 0;
    private static long Threshold = 1500;
    private ImageView fingerPrintView;
    UserRegistration userRegistration = new UserRegistration();
    ProgressDialog progressDialog;
    Activity activity = this;
    String message;
    AlertDialog.Builder alertDialogMatched, alertDialogNotMatched, alertDialog;
    byte[] finalWsqImageData = null;

    private enum ScannerAction {
        Capture, Verify
    }

    byte[] Enroll_Template;
    byte[] Verify_Template;
    private FingerData lastCapFingerData = null;
    ScannerAction scannerAction = ScannerAction.Capture;

    private static final String LOG_TAG_EXTERNAL_STORAGE = "EXTERNAL_STORAGE";

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;

    int timeout = 10000;
    MFS100 mfs100 = null;

    private boolean isCaptureRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mfs100_sample);


        fingerPrintView = (ImageView) findViewById(R.id.imageView1);
        dismsg = (TextView) findViewById(R.id.dis_msg);
        Bundle bundle = getIntent().getExtras();
        message = bundle.getString("ScanningType");
        userRegistration = (UserRegistration) bundle.getSerializable("refugeeData");


        FindFormControls();
        try {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } catch (Exception e) {
            Log.e("Error", e.toString());
        }
        try {
            mfs100 = new MFS100(this);
            mfs100.SetApplicationContext(MFS100Test.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        try {
            if (mfs100 == null) {
                mfs100 = new MFS100(this);
                mfs100.SetApplicationContext(MFS100Test.this);
            }/* else {
                InitScanner();
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onStart();
    }

    protected void onStop() {
        try {
            if (isCaptureRunning) {
                int ret = mfs100.StopAutoCapture();
            }
            Thread.sleep(500);
            //            UnInitScanner();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        try {
            if (mfs100 != null) {
                mfs100.Dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    public void FindFormControls() {
        try {
            btnInit = (Button) findViewById(R.id.btnInit);
            btnUninit = (Button) findViewById(R.id.btnUninit);
            btnMatchISOTemplate = (Button) findViewById(R.id.btnMatchISOTemplate);
            btnExtractISOImage = (Button) findViewById(R.id.btnExtractISOImage);
            btnExtractAnsi = (Button) findViewById(R.id.btnExtractAnsi);
            btnExtractWSQImage = (Button) findViewById(R.id.btnExtractWSQImage);
            btnClearLog = (Button) findViewById(R.id.btnClearLog);
            lblMessage = (TextView) findViewById(R.id.lblMessage);
            txtEventLog = (EditText) findViewById(R.id.txtEventLog);
            imgFinger = (ImageView) findViewById(R.id.imgFinger);
            btnSyncCapture = (Button) findViewById(R.id.btnSyncCapture);
            btnStopCapture = (Button) findViewById(R.id.btnStopCapture);
            cbFastDetection = (CheckBox) findViewById(R.id.cbFastDetection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onControlClicked(View v) {
        if (SystemClock.elapsedRealtime() - mLastClkTime < Threshold) {
            return;
        }
        mLastClkTime = SystemClock.elapsedRealtime();
        try {
            switch (v.getId()) {
                case R.id.btnInit:
                    InitScanner();
                    break;
                case R.id.btnUninit:
                    UnInitScanner();
                    break;
                case R.id.btnSyncCapture:
                    scannerAction = ScannerAction.Capture;

                    if (!isCaptureRunning) {
                        StartSyncCapture();
                    }
                    break;
                case R.id.btnStopCapture:
                    StopCapture();
                    break;
                case R.id.btnMatchISOTemplate:
                    scannerAction = ScannerAction.Verify;
                    if (!isCaptureRunning) {
                        StartSyncCapture();
                    }
                    break;
                case R.id.btnExtractISOImage:
                    ExtractISOImage();
                    break;
                case R.id.btnExtractAnsi:
                    ExtractANSITemplate();
                    break;
                case R.id.btnExtractWSQImage:
                    ExtractWSQImage();
                    break;
                case R.id.btnClearLog:
                    ClearLog();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_mfs100_sample);
        FindFormControls();
        try {
            if (mfs100 == null) {
                mfs100 = new MFS100(this);
                mfs100.SetApplicationContext(this);
            } else {
                InitScanner();
            }
            if (isCaptureRunning) {
                if (mfs100 != null) {
                    mfs100.StopAutoCapture();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InitScanner() {
        try {
            int ret = mfs100.Init();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetTextOnUIThread("Init success");
                String info = "Serial: " + mfs100.GetDeviceInfo().SerialNo()
                        + " Make: " + mfs100.GetDeviceInfo().Make()
                        + " Model: " + mfs100.GetDeviceInfo().Model()
                        + "\nCertificate: " + mfs100.GetCertification();
                SetLogOnUIThread(info);
            }
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Init failed, unhandled exception",
                    Toast.LENGTH_LONG).show();
            SetTextOnUIThread("Init failed, unhandled exception");
        }
    }

    private void StartSyncCapture() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SetTextOnUIThread("");
                isCaptureRunning = true;

                /*byte[] wsqData = new WSQEncoder(lastCapFingerData.getScanImage())
                        .setBitrate(WSQEncoder.BITRATE_5_TO_1)
                        .encode();*/
                try {
                    FingerData fingerData = new FingerData();
                    int ret = mfs100.AutoCapture(fingerData, timeout, cbFastDetection.isChecked());
                    Log.e("StartSyncCapture.RET", "" + ret);
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        lastCapFingerData = fingerData;


                        final Bitmap bitmap = BitmapFactory.decodeByteArray(fingerData.FingerImage(), 0,
                                fingerData.FingerImage().length);
                        MFS100Test.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imgFinger.setImageBitmap(bitmap);


                            }
                        });
                        byte[] wsqData = new WSQEncoder(bitmap)
                                .setBitrate(WSQEncoder.BITRATE_15_TO_1)
                                .encode();
                        RegistrationData.setByteData(wsqData);

                        if (message.equals("ugandanFinger")) {
                            if (ScanData.getScannedBarcodeData().getScannedFingerData() != null) {
                                byte[] fpBytes = ScanData.getScannedBarcodeData().getScannedFingerData();

                                int fpBytesSize = fpBytes.length;

                                int numMinutiaePoints = fpBytesSize / 5;

                                System.out.println("Size of array is " + numMinutiaePoints);
                                int maxX = 0;
                                int maxY = 0;
                                int byteNum = 0;
                                int xValues[] = new int[numMinutiaePoints];
                                int yValues[] = new int[numMinutiaePoints];

                                for (byteNum = 0; byteNum < fpBytesSize; byteNum = byteNum + 5) {
                                    int xVal = ((fpBytes[byteNum] & 0x3F) << 8) + (fpBytes[byteNum + 1] & 0xFF);
                                    xVal = (int) (((float) xVal / 1000f) * 196.85f);

                                    int minNum = byteNum / 5;
                                    if (minNum >= numMinutiaePoints)
                                        break;

                                    xValues[minNum] = xVal;
                                    if (xVal > maxX) {
                                        maxX = xVal;
                                    }

                                    int yVal = ((fpBytes[byteNum + 2] & 0x3F) << 8) + (fpBytes[byteNum + 3] & 0xFF);
                                    yVal = (int) (((float) yVal / 1000f) * 196.85f);
                                    yValues[minNum] = yVal;
                                    if (yVal > maxY) {
                                        maxY = yVal;
                                    }

                                    System.out.println(Integer.toHexString(fpBytes[byteNum] & 0x3F) + " " + Integer.toHexString(fpBytes[byteNum + 1] & 0xFF) + " " + xVal);
                                    System.out.println(Integer.toHexString(fpBytes[byteNum + 2] & 0x3F) + " " + Integer.toHexString(fpBytes[byteNum + 3] & 0xFF) + " " + yVal);
                                }
                                System.out.println("Max x and y values are:  " + maxX + "," + maxY);
                                byte[] isoform = new byte[24 + 4 + (numMinutiaePoints * 6) + 2];
//magic
                                isoform[0] = 0x46;
                                isoform[1] = 0x4d;
                                isoform[2] = 0x52;
                                isoform[3] = 0x00;

//version
                                isoform[4] = 0x20;
                                isoform[5] = 0x32;
                                isoform[6] = 0x30;
                                isoform[7] = 0x00;

                                //num bytes
                                int numBytes = isoform.length;
                                isoform[8] = 0x00;
                                isoform[9] = 0x00;
                                isoform[10] = (byte) ((numBytes >> 8) & 0xFF);
                                isoform[11] = (byte) (numBytes & 0xFF);

//mystery
                                isoform[12] = 0x00;
                                isoform[13] = 0x00;

//Width
                                ++maxX;
                                isoform[14] = (byte) ((maxX >> 8) & 0xFF);
                                isoform[15] = (byte) (maxX & 0xFF);

//height
                                ++maxY;
                                isoform[16] = (byte) ((maxY >> 8) & 0xFF);
                                isoform[17] = (byte) (maxY & 0xFF);

//dpi-x
                                isoform[18] = 0x00;
                                isoform[19] = (byte) 0xc5;

//dpi-y
                                isoform[20] = 0x00;
                                isoform[21] = (byte) 0xc5;

//fpcount
                                isoform[22] = 0x01;

//mystery
                                isoform[23] = 0x00;

                                isoform[24] = 0x00;

                                isoform[25] = 0x00;

//quality
                                isoform[26] = 0x64;

                                isoform[27] = (byte) (numMinutiaePoints & 0xFF);

                                int minOffset = 28;
                                byte xyCopyMask = (byte) 0xC0;
                                for (int i = 0; i < numMinutiaePoints; i++) {
                                    int xVal = xValues[i];
                                    System.out.println("Xval " + xVal);

                                    int yVal = yValues[i];

                                    isoform[minOffset + (i * 6)] = (byte) ((fpBytes[(i * 5)] & xyCopyMask) | (xVal >> 8) & 0xFF);
                                    isoform[minOffset + (i * 6) + 1] = (byte) (xVal & 0xFF);
                                    isoform[minOffset + (i * 6) + 2] = (byte) ((fpBytes[(i * 5) + 2] & xyCopyMask) | (yVal >> 8) & 0xFF);
                                    isoform[minOffset + (i * 6) + 3] = (byte) (yVal & 0xFF);
                                    isoform[minOffset + (i * 6) + 4] = fpBytes[(i * 5) + 4];
                                    isoform[minOffset + (i * 6) + 5] = 0x64;
                                }

                                String bytedump = "";
                                for (byte abyt : isoform) {
                                    bytedump += Integer.toHexString(0xff & abyt) + " ";
                                }

                                int mret = mfs100.MatchISO(isoform, fingerData.ISOTemplate());

                                if (mret >= 60) {
                                    SetTextOnUIThread("Finger matched with score: " + mret);
                                    RegistrationData.setIsmatched(true);
                                    RegistrationData.setCapturedFingerprintDrawable(imgFinger.getDrawable());
                                    showAlert("Success", "Fingerprint matched Successfully.");
                                } else {
                                    SetTextOnUIThread("Finger not matched, score: " + mret);
                                    RegistrationData.setIsmatched(false);
                                    imgFinger.setImageDrawable(null);
                                    RegistrationData.setCapturedFingerprintDrawable(null);
                                    MFS100Test.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            alertDialogNotMatched = new AlertDialog.Builder(MFS100Test.this);
                                            alertDialogNotMatched.setCancelable(false)
                                                    .setTitle("Failed")
                                                    .setMessage("Last Fingerprint did not Matching, please retry!")
                                                    .setPositiveButton(activity.getResources().getString(R.string.ok),
                                                            new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int id) {
                                                                    dialog.dismiss();
                                                                }
                                                            });
                                            AlertDialog alert = alertDialogNotMatched.create();
                                            alert.show();
                                        }
                                    });
                                    //RegistrationData.setCapturedFingerprintDrawable(imgFinger.getDrawable());
                                    //showAlert("Failed","Fingerprint not matched.");
                                }
                        /*if (mret >= 60) {
                            SetTextOnUIThread("Finger matched with score: " + mret);
                            RegistrationData.setIsmatched(true);
                            Drawable drawable = new BitmapDrawable(bitmap);
                            RegistrationData.setCapturedFingerprintDrawable(drawable);
                            showAlert("Success", "Fingerprint matched Successfully.");
                        } else {
                            SetTextOnUIThread("Finger not matched, score: " + mret);
                            RegistrationData.setIsmatched(false);
                            Drawable drawable = new BitmapDrawable(bitmap);
                            RegistrationData.setCapturedFingerprintDrawable(drawable);
                            showAlert("Failed","Fingerprint not matched.");

                           *//* activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                    alertDialog.setCancelable(false);
                                    alertDialog.setTitle("Failed");
                                    alertDialog.setMessage("Last Fingerprint did not Matching, please retry!");
                                    alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss();
                                                    //DisableCapture();
                                                    //MFS100Test.CaptureFM220(2, true, false);
                                                }
                                            });
                                    alertDialog.show();
                                }
                            });*//*


                        }*/
                        /*}else {
                            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                            alertDialog.setCancelable(false);
                            alertDialog.setTitle("Alert");
                            alertDialog.setMessage("Please, rescan the NIN Document. Reason: Empty Fingerprint Data.");
                            alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                            activity.finish();
                                            RegistrationData.setIsScanICCID(false);
                                            ScanData.setScannedBarcodeData(null);
                                            Intent intent = new Intent(activity,BarcodeScannerActivity.class);
                                            startActivity(intent);
                                        }
                                    });
                            alertDialog.show();
                        }*/
                                /*if (!isCaptureRunning) {
                                    StartSyncCapture();
                                }*/
                                //showAlert("Failed", "Fingerprint not matched.");

                            } else {
                                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                alertDialog.setCancelable(false);
                                alertDialog.setTitle("Alert");
                                alertDialog.setMessage("Please, rescan the NIN Document. Reason: Empty Fingerprint Data.");
                                alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                                activity.finish();
                                                RegistrationData.setIsScanICCID(false);
                                                ScanData.setScannedBarcodeData(null);
                                                Intent intent = new Intent(activity, BarcodeScannerActivity.class);
                                                startActivity(intent);
                                            }
                                        });
                                alertDialog.show();
                            }
                        }else if(message.equals("Thumb")) {
                            try {
                                RegistrationData.setSubscriberThumbImageDrawable(imgFinger.getDrawable());
                                callParentActivity();
                            } catch(Exception ex) {
                                BugReport.postBugReport(activity, Constants.emailId,
                                        "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                        "FingerPrintThumb");
                            }
                            // callParentActivity();

                        }else if(message.equals("Index")) {
                            try {
                                RegistrationData.setSubscriberIndexImageDrawable(imgFinger.getDrawable());
                                callParentActivity();
                            } catch(Exception ex) {
                                BugReport.postBugReport(activity, Constants.emailId,
                                        "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                        "FingerPrintIndex");
                            }


                        }else if(message.equals("userFinger")){
                            try {
                                RegistrationData.setImageDrawable(imgFinger.getDrawable());
                                callParentActivity();
                            } catch(Exception ex) {
                                BugReport.postBugReport(activity, Constants.emailId,
                                        "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                        "FingerPrintUserFinger");
                            }
                        }else if(message.equals("userThumb")){
                            try {
                                RegistrationData.setUserThumbImageDrawable(imgFinger.getDrawable());
                                callParentActivity();
                            } catch(Exception ex) {
                                BugReport.postBugReport(activity, Constants.emailId,
                                        "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                        "FingerPrintUserThumb");
                            }


                        }else if(message.equals("userIndex")){
                            try {
                                RegistrationData.setUserIndexImageDrawable(imgFinger.getDrawable());
                                callParentActivity();
                            } catch(Exception ex) {
                                BugReport.postBugReport(activity, Constants.emailId,
                                        "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                        "FingerPrintUserIndex");
                            }
                        }else if(message.equals("refugeeUser")){

                            try {
                                ExtractWSQImage();

                                if(finalWsqImageData != null){
                                   // String encodedString = Base64.encodeToString(wsqData, Base64.DEFAULT);//wsqData.toString()
                                   byte[] encoded = org.apache.commons.codec.binary.Base64.encodeBase64(finalWsqImageData);
                                   final String encodedString = new String(encoded);

                                    if (isStoragePermissionGranted()) { // check or ask permission

                                        String folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/Tangerine";
                                        File folder = new File(folderPath);
                                        if (!folder.exists()) {
                                            folder.mkdirs();
                                            // MyToast.makeMyToast(activity,"Tangerine Directory Created", Toast.LENGTH_SHORT);
                                        }
                                        // String root = Environment.getExternalStorageDirectory().toString();
                                        File myDir = new File(folder, "Temp");
                                        if (!myDir.exists()) {
                                            myDir.mkdirs();
                                        }
                                        String fname = "Thumbprint.txt";
                                        File file = new File(myDir, fname);
                                        if (file.exists()) {
                                            file.delete();
                                        }
                                        String status = "";
                                        try {
                                            if (file.createNewFile()) {

                                                FileWriter writer = new FileWriter(file);
                                                writer.append(encodedString);
                                                writer.flush();
                                                writer.close();


                                                // textMessage.setText("Created file " + file.getAbsolutePath());
                                            } else {
                                                //  textMessage.setText("Created file " + file.getAbsolutePath() + " failed");
                                            }

                                        } catch (Exception ex) {
                                            // textMessage.setText("Exception creating file " + file.getAbsolutePath() + e.getMessage() + " " + e.getCause());
                                            BugReport.postBugReport(activity, Constants.emailId,
                                                    "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                                    "FingerPrintUserIndex");
                                        }

                                    } else {
                                        // textMessage.setText("Not allowed to write file");
                                    }


                                    /*alertDialogMatched = new AlertDialog.Builder(MFS100Test.this);
                                    alertDialogMatched.setCancelable(false);
                                    alertDialogMatched.setTitle("Message!");
                                    alertDialogMatched.setMessage("Fingerprint Captured, you want to retry?");
                                    alertDialogMatched.setPositiveButton(activity.getResources().getString(R.string.ok),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    RegistrationData.setRefugeeThumbImageDrawable(null);
                                                    RegistrationData.setRefugeeThumbEncodedData(null);
                                                    dialog.dismiss();
                                                }
                                            })
                                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    RegistrationData.setRefugeeThumbImageDrawable(imgFinger.getDrawable());
                                                    RegistrationData.setRefugeeThumbEncodedData(encodedString);

                                                    dialog.dismiss();
                                                    activity.finish();
                                                    Intent intent = new Intent();
                                                    setResult(Activity.RESULT_OK, intent);
                                                }
                                            });
                                    AlertDialog alert = alertDialogMatched.create();
                                    alert.show();*/

                                    RegistrationData.setRefugeeThumbImageDrawable(imgFinger.getDrawable());
                                    RegistrationData.setRefugeeThumbEncodedData(encodedString);

                                    verifyRefugeeIdentity(encodedString);


                                   // callParentActivity();

                                }else {
                                    BugReport.postBugReport(activity, Constants.emailId,
                                            "Message:Stack=======: ",
                                            "FingerPrintUserIndex");
                                }

                            } catch(Exception ex) {
                                BugReport.postBugReport(activity, Constants.emailId,
                                        "Message: " + ex.getMessage() + ", Cause: " + ex.getCause() + "Stack: " + Log.getStackTraceString(ex),
                                        "FingerPrintUserIndex");
                            }

                        }


                        /*int dataLen = mfs100.ExtractANSITemplate(lastCapFingerData.RawData(), tempData);
                        byte[] ansiTemplate = new byte[dataLen];
                        System.arraycopy(tempData, 0, ansiTemplate, 0, dataLen);

                        int mret = mfs100.MatchANSI(isoform, ansiTemplate);
                        File outputFile = new File(root, "saved_images/fingeransi.dat");*/

                        //SetTextOnUIThread("Finger matched with score: " + mret);

                        /*for (byte abyt : isoform) {
                            System.out.print(Integer.toHexString(0xff & abyt) + " ");
                        }*/


                        /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        FingerprintTemplate probe = new FingerprintTemplate().create(stream.toByteArray());

                        Bitmap bitmap1 = ((BitmapDrawable) fingerPrintView.getDrawable()).getBitmap();
                        ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
                        bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream1);
                        FingerprintTemplate candidate = new FingerprintTemplate().create(stream1.toByteArray());
                        double score = new FingerprintMatcher()
                                .index(probe)
                                .match(candidate);
                        //textMessage.setText("Score----:"+score);
                        boolean matches = score >= 40;*/

//                        Log.e("RawImage", Base64.encodeToString(fingerData.RawData(), Base64.DEFAULT));
//                        Log.e("FingerISOTemplate", Base64.encodeToString(fingerData.ISOTemplate(), Base64.DEFAULT));
                        //SetTextOnUIThread(score + "Capture Success");

                        String log = "\nQuality: " + fingerData.Quality()
                                + "\nNFIQ: " + fingerData.Nfiq()
                                + "\nWSQ Compress Ratio: "
                                + fingerData.WSQCompressRatio()
                                + "\nImage Dimensions (inch): "
                                + fingerData.InWidth() + "\" X "
                                + fingerData.InHeight() + "\""
                                + "\nImage Area (inch): " + fingerData.InArea()
                                + "\"" + "\nResolution (dpi/ppi): "
                                + fingerData.Resolution() + "\nGray Scale: "
                                + fingerData.GrayScale() + "\nBits Per Pixal: "
                                + fingerData.Bpp() + "\nWSQ Info: "
                                + fingerData.WSQInfo();
                        SetLogOnUIThread(log);
                        SetData2(fingerData);
                    }
                } catch (Exception ex) {
                    SetTextOnUIThread(ex.getMessage() + " " + ex.getClass().toString() + ex.getCause());
                } finally {
                    isCaptureRunning = false;
                }
            }
        }).start();
    }


    private void verifyRefugeeIdentity(String encodedString) {

        RestServiceHandler serviceHandler = new RestServiceHandler();
        try {

            if(RegistrationData.getRefugeeThumbEncodedData() != null){

                if (userRegistration != null){

                    userRegistration.fingerprint = RegistrationData.getRefugeeThumbEncodedData();


                    MFS100Test.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog = ProgressDialogUtil.startProgressDialog(activity,"Verifying Refugee......");
                        }
                    });

                    serviceHandler.postRefugeeVerification(userRegistration, new RestServiceHandler.Callback() {
                        @Override
                        public void success(DataModel.DataType type, List<DataModel> data) {
                            UserLogin userLogin = (UserLogin) data.get(0);
                            if (userLogin.status.equals("Match")) {



                                MFS100Test.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ProgressDialogUtil.stopProgressDialog(progressDialog);

                                        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                        alertDialog.setCancelable(false);
                                        alertDialog.setMessage("Refugee Identity Verified Successfully!");
                                        alertDialog.setNeutralButton(getResources().getString(R.string.ok),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        dialog.dismiss();
                                                        RegistrationData.setIsRefugeeMatched(true);

                                                        activity.finish();
                                                        Intent intent = new Intent();
                                                        setResult(Activity.RESULT_OK, intent);

                                                    }
                                                });
                                        alertDialog.show();
                                    }
                                });


                            }else{

                                if(userLogin.status.equals("INVALID_SESSION")){
                                    ReDirectToParentActivity.callLoginActivity(activity);

                                }else{


                                    final View dialogView = getLayoutInflater().inflate(R.layout.custom_layout, null);
                                    ImageView pic = (ImageView) dialogView.findViewById(R.id.dialog_pic);
                                    pic.setImageDrawable(RegistrationData.getRefugeeThumbImageDrawable());
                                    TextView desc = (TextView) dialogView.findViewById(R.id.desc);
                                    desc.setText("Status: "+userLogin.status);

                                    MFS100Test.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            ProgressDialogUtil.stopProgressDialog(progressDialog);

                                            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                            alertDialog.setCancelable(false);
                                            alertDialog.setTitle("Message!");
                                            alertDialog.setView(dialogView);
                                            /* alertDialog.setMessage("Status: "+userLogin.status);*/
                                            alertDialog.setNeutralButton(getResources().getString(R.string.ok),
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            dialog.dismiss();
                                                            RegistrationData.setIsRefugeeMatched(false);

                                                        }
                                                    });
                                            alertDialog.show();
                                        }
                                    });

                                }

                                // MyToast.makeMyToast(getActivity(), userLogin.status, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void failure(RestServiceHandler.ErrorCode error, String status) {
                            MFS100Test.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ProgressDialogUtil.stopProgressDialog(progressDialog);
                                }
                            });

                            BugReport.postBugReport(activity, Constants.emailId,"ERROR:"+error+",\t STATUS"+status,"REFUGEE VERIFICATION");
                        }

                    });

                }


            }else{
                MyToast.makeMyToast(activity,"Please Capture Fingerprint.", Toast.LENGTH_SHORT);
            }

        } catch (IOException e) {
            e.printStackTrace();
            BugReport.postBugReport(activity, Constants.emailId,"MESSAGE:"+e.getMessage()+",\t CAUSE:"+e.getCause(),"NIRA VERIFICATION");
        }
    }

    private void showAlert(final String title, final String message) {
        /*final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getApplicationContext());
        alertDialog.setCancelable(false);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setNeutralButton(getApplicationContext().getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        Intent intent = new Intent();
                        setResult(Activity.RESULT_OK, intent);
                        MFS100Test.this.finish();

                    }
                });
        alertDialog.show();*/

        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, title+": "+message, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                setResult(Activity.RESULT_OK, intent);
                MFS100Test.this.finish();
            }
        });
    }

    private void AllowPermissions() {

      /*  if(ExternalStorageUtil.isExternalStorageMounted()) {

            // Check whether this app has write external storage permission or not.

        }*/
    }

    private void callParentActivity() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void StopCapture() {
        try {
            mfs100.StopAutoCapture();
        } catch (Exception e) {
            SetTextOnUIThread("Error");
        }
    }

    private void ExtractANSITemplate() {
        byte[] ansiTemplate = new byte[0];
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[2000]; // length 2000 is mandatory
            int dataLen = mfs100.ExtractANSITemplate(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ANSI Template");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                ansiTemplate = new byte[dataLen];
                System.arraycopy(tempData, 0, ansiTemplate, 0, dataLen);
                WriteFile("ANSITemplate.ansi", ansiTemplate);
                String bytedump = "";

                for (byte b : ansiTemplate) {
                    bytedump += Integer.toHexString(b & 0xFF) + " ";
                }

                SetTextOnUIThread("Extract ANSI Template Success :: { "+ bytedump + " }");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ANSI Template Error", e);
        }
    }

    private void ExtractISOImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] isoImage;

            // ISOType 1 == Regular ISO Image
            // 2 == WSQ Compression ISO Image
            int dataLen = mfs100.ExtractISOImage(lastCapFingerData.RawData(), tempData, 2);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract ISO Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                isoImage = new byte[dataLen];
                System.arraycopy(tempData, 0, isoImage, 0, dataLen);
                WriteFile("ISOImage.iso", isoImage);

                String bytedump = "";

                for (byte b : isoImage) {
                    bytedump += Integer.toHexString(b & 0xFF) + " ";
                }

                SetTextOnUIThread("Extract ISO Image Success:: { "+ bytedump + " }");
            }
        } catch (Exception e) {
            Log.e("Error", "Extract ISO Image Error", e);
        }
    }

    private void ExtractWSQImage() {
        try {
            if (lastCapFingerData == null) {
                SetTextOnUIThread("Finger not capture");
                return;
            }
            byte[] tempData = new byte[(mfs100.GetDeviceInfo().Width() * mfs100.GetDeviceInfo().Height()) + 1078];
            byte[] wsqImage;
            int dataLen = mfs100.ExtractWSQImage(lastCapFingerData.RawData(), tempData);
            if (dataLen <= 0) {
                if (dataLen == 0) {
                    SetTextOnUIThread("Failed to extract WSQ Image");
                } else {
                    SetTextOnUIThread(mfs100.GetErrorMsg(dataLen));
                }
            } else {
                wsqImage = new byte[dataLen];
                System.arraycopy(tempData, 0, wsqImage, 0, dataLen);
                WriteFile("WSQ.wsq", wsqImage);

                String bytedump = "";

                for (byte b : wsqImage) {
                    bytedump += Integer.toHexString(b & 0xFF) + " ";
                }

                finalWsqImageData = wsqImage;
                SetTextOnUIThread("Extract WSQ Image Success :: { "+ bytedump + " }");

            }



               /* SetTextOnUIThread("Extract WSQ Image Success");
            }*/
        } catch (Exception e) {
            Log.e("Error", "Extract WSQ Image Error", e);
        }
    }

    private void UnInitScanner() {
        try {
            int ret = mfs100.UnInit();
            if (ret != 0) {
                SetTextOnUIThread(mfs100.GetErrorMsg(ret));
            } else {
                SetLogOnUIThread("Uninit Success");
                SetTextOnUIThread("Uninit Success");
                lastCapFingerData = null;
            }
        } catch (Exception e) {
            Log.e("UnInitScanner.EX", e.toString());
        }
    }

    private void WriteFile(String filename, byte[] bytes) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            stream.write(bytes);
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void WriteFileString(String filename, String data) {
        try {
            String path = Environment.getExternalStorageDirectory()
                    + "//FingerData";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
            path = path + "//" + filename;
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(path);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            writer.write(data);
            writer.flush();
            writer.close();
            stream.close();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void ClearLog() {
        txtEventLog.post(new Runnable() {
            public void run() {
                try {
                    txtEventLog.setText("", BufferType.EDITABLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void SetTextOnUIThread(final String str) {
        //lblMessage.setText(str);
        lblMessage.post(new Runnable() {
            public void run() {
                try {
                    lblMessage.setText(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /*new Thread(){
            public void run(){
                MFS100Test.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        lblMessage.setText(str);
                    }
                });
            }

        }.start();*/

       /* Thread thread = new Thread() {
            public void run() {
                Looper.prepare();

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        lblMessage.setText(str);
                        Toast.makeText(getApplicationContext(),"---------RAJ---"+str,Toast.LENGTH_SHORT).show();
                        handler.removeCallbacks(this);
                        Looper.myLooper().quit();
                    }
                }, 2000);

                Looper.loop();
            }
        };
        thread.start();*/
    }

    private void SetLogOnUIThread(final String str) {

        txtEventLog.post(new Runnable() {
            public void run() {
                try {
                    txtEventLog.append("\n" + str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void SetData2(FingerData fingerData) {
        try {
            if (scannerAction.equals(ScannerAction.Capture)) {
                Enroll_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Enroll_Template, 0,
                        fingerData.ISOTemplate().length);

            } else if (scannerAction.equals(ScannerAction.Verify)) {
                if (Enroll_Template == null) {
                    return;
                }

                /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                FingerprintTemplate probe = new FingerprintTemplate().create(stream.toByteArray());

                Bitmap bitmap1 = ((BitmapDrawable) fingerPrintView.getDrawable()).getBitmap();
                ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
                bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, stream1);
                FingerprintTemplate candidate = new FingerprintTemplate().create(stream1.toByteArray());
                double score = new FingerprintMatcher()
                        .index(probe)
                        .match(candidate);
                //textMessage.setText("Score----:"+score);
                boolean matches = score >= 40;
*/

                Verify_Template = fingerData.ISOTemplate();
                Verify_Template = new byte[fingerData.ISOTemplate().length];
                System.arraycopy(fingerData.ISOTemplate(), 0, Verify_Template, 0,
                        fingerData.ISOTemplate().length);
                int ret = mfs100.MatchISO(Enroll_Template, Verify_Template);
                if (ret < 0) {
                    SetTextOnUIThread("Error: " + ret + "(" + mfs100.GetErrorMsg(ret) + ")");
                } else {
                    if (ret >= 96) {
                        SetTextOnUIThread("Finger matched with score: " + ret);
                    } else {
                        SetTextOnUIThread("Finger not matched, score: " + ret);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            WriteFile("Raw.raw", fingerData.RawData());
            WriteFile("bitmap.bmp", fingerData.FingerImage());
            WriteFile("ISOTemplate.iso", fingerData.ISOTemplate());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long mLastAttTime=0l;

    @Override
    public void OnDeviceAttached(int vid, int pid, boolean hasPermission) {

        if (SystemClock.elapsedRealtime() - mLastAttTime < Threshold) {
            return;
        }
        mLastAttTime = SystemClock.elapsedRealtime();
        int ret;
        if (!hasPermission) {
            SetTextOnUIThread("Permission denied");
            return;
        }
        try {
            if (vid == 1204 || vid == 11279) {
                if (pid == 34323) {
                    ret = mfs100.LoadFirmware();
                    if (ret != 0) {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    } else {
                        SetTextOnUIThread("Load firmware success");
                    }
                } else if (pid == 4101) {
                    String key = "Without Key";
                    ret = mfs100.Init();
                    if (ret == 0) {
                        showSuccessLog(key);
                    } else {
                        SetTextOnUIThread(mfs100.GetErrorMsg(ret));
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSuccessLog(String key) {
        try {
            SetTextOnUIThread("Init success");
            String info = "\nKey: " + key + "\nSerial: "
                    + mfs100.GetDeviceInfo().SerialNo() + " Make: "
                    + mfs100.GetDeviceInfo().Make() + " Model: "
                    + mfs100.GetDeviceInfo().Model()
                    + "\nCertificate: " + mfs100.GetCertification();
            SetLogOnUIThread(info);
        } catch (Exception e) {
        }
    }
    long mLastDttTime=0l;
    @Override
    public void OnDeviceDetached() {
        try {

            if (SystemClock.elapsedRealtime() - mLastDttTime < Threshold) {
                return;
            }
            mLastDttTime = SystemClock.elapsedRealtime();
            UnInitScanner();

            SetTextOnUIThread("Device removed");
        } catch (Exception e) {
        }
    }

    @Override
    public void OnHostCheckFailed(String err) {
        try {
            SetLogOnUIThread(err);
            Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
        }
    }
    public boolean isStoragePermissionGranted() {
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

}
