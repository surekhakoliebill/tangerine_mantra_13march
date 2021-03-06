package com.aryagami.tangerine.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aryagami.R;
import com.aryagami.data.CacheNewOrderData;
import com.aryagami.data.DataModel;
import com.aryagami.data.NewOrderCommand;
import com.aryagami.data.UserLogin;
import com.aryagami.data.UserRegistration;
import com.aryagami.restapis.RestServiceHandler;
import com.aryagami.tangerine.activities.CacheUpdateUserDataActivity;
import com.aryagami.tangerine.activities.NavigationMainActivity;
import com.aryagami.util.MyToast;
import com.aryagami.util.ProgressDialogUtil;

import java.util.ArrayList;
import java.util.List;

public class CacheUpdateUserDataAdapter extends ArrayAdapter {
    Activity activity;
    NewOrderCommand[] orderArray;
    ProgressDialog progressDialog;
    int pdfDocUploadedCount = 0;
    int passportUploadSuccessCount = 0;
    int fingerprintUploadSuccessCount = 0;

    long randomNumber = generateRandomNumber();

    private long generateRandomNumber() {
        double n = Math.random();
        long n3 = Math.round(Math.random() * 1000);
        return n3;
    }

    public CacheUpdateUserDataAdapter(Activity activity, NewOrderCommand[] orderCommands) {
        super(activity, R.layout.item_cache_data, orderCommands);
        this.activity = activity;
        this.orderArray = orderCommands;
    }

    public  void onTrimMemory(int level) {
        System.gc();
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView;
        if (convertView != null) {
            rowView = convertView;
        } else {
            LayoutInflater inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.item_cache_data, null, true);
        }

        final NewOrderCommand rowItem = (NewOrderCommand) getItem(position);
        TextView username = (TextView) rowView.findViewById(R.id.username_value);
        TextView fullname = (TextView) rowView.findViewById(R.id.fullname_value);
        TextView userGroup = (TextView) rowView.findViewById(R.id.usergroup_value);
        TextView surname = (TextView) rowView.findViewById(R.id.surname_value);
        Button uploadButton = (Button) rowView.findViewById(R.id.update_button);
        Button deleteButton = (Button) rowView.findViewById(R.id.delete_button);

        if (rowItem != null) {

            if (rowItem.userInfo.surname != null) {
                surname.setText(rowItem.userInfo.surname);
            } else {
                surname.setText("NA");
            }

            if (rowItem.userInfo.fullName != null) {
                fullname.setText(rowItem.userInfo.fullName);
            } else {
                fullname.setText("NA");
            }

            if (rowItem.userInfo.userGroup != null) {
                userGroup.setText(rowItem.userInfo.userGroup);
            } else {
                userGroup.setText("NA");
            }

            if (rowItem.userInfo.userName != null) {
                username.setText(rowItem.userInfo.userName);
            } else {
                username.setText("NA");
            }

            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        updateUserDetails(rowItem, position);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progressDialog = ProgressDialogUtil.startProgressDialog(activity, "please wait, Deleting entry from Cache!");

                    List<NewOrderCommand> commandList = new ArrayList<NewOrderCommand>();
                    commandList = CacheNewOrderData.loadUpdateUserCacheList(activity);
                    if(commandList != null)
                        if(commandList.size() != 0){
                            commandList.remove(position);
                            CacheNewOrderData.saveUpdateUserDataCache(activity,commandList);

                            ProgressDialogUtil.stopProgressDialog(progressDialog);

                            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                            alertDialog.setCancelable(false);
                            alertDialog.setTitle("Delete Alert");
                            alertDialog.setMessage("Entry Deleted Successfully.");
                            alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.dismiss();
                                            startCacheActivity();
                                        }
                                    });
                            alertDialog.show();

                        }
                }
            });
        }


        return rowView;
    }

    private void startCacheActivity() {
        activity.finish();
        Intent intent = new Intent(activity, CacheUpdateUserDataActivity.class);
        activity.startActivity(intent);
    }

    private void updateUserDetails(final NewOrderCommand rowItem, final int position) throws Exception {

        RestServiceHandler serviceHandler = new RestServiceHandler();
        progressDialog = ProgressDialogUtil.startProgressDialog(activity, "please wait...!");

        serviceHandler.postUpdateUser(rowItem.userInfo, new RestServiceHandler.Callback() {
            @Override
            public void success(DataModel.DataType type, List<DataModel> data) {
                UserLogin orderDetails = (UserLogin) data.get(0);
                if (orderDetails.status.equals("success")) {
                    uploadDocuments(rowItem);
                    List<NewOrderCommand> commandList = new ArrayList<NewOrderCommand>();
                    commandList = CacheNewOrderData.loadUpdateUserCacheList(activity);
                   if(commandList != null)
                    if(commandList.size() != 0){
                          commandList.remove(position);
                          CacheNewOrderData.saveUpdateUserDataCache(activity,commandList);
                    }

                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                    alertDialog.setCancelable(false);
                    alertDialog.setTitle("Success Alert");
                    alertDialog.setMessage("User Updated Successfully.");
                    alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    startNavigationActivity();
                                }
                            });
                    alertDialog.show();

                } else {
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                    alertDialog.setCancelable(false);
                    alertDialog.setTitle("Alert");
                    alertDialog.setMessage("Status:"+orderDetails.status);
                    alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    startNavigationActivity();
                                }
                            });
                    alertDialog.show();                }
            }

            @Override
            public void failure(RestServiceHandler.ErrorCode error, String status) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                alertDialog.setCancelable(false);
                alertDialog.setTitle("Error Alert");
                alertDialog.setMessage("Error:"+error+"\n Status:"+status);
                alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                startNavigationActivity();
                            }
                        });
                alertDialog.show();            }
        });

    }

    private void startNavigationActivity() {

        activity.finish();
        Intent i = new Intent(activity, NavigationMainActivity.class);
        activity.startActivity(i);
    }

    private void uploadDocuments(NewOrderCommand rowItem) {
        List<UserRegistration.UserDocCommand> userDocCommandList = new ArrayList<UserRegistration.UserDocCommand>();
        if (rowItem.userInfo.userDocs != null)
            if (rowItem.userInfo.userDocs.size() != 0) {
                userDocCommandList = rowItem.userInfo.userDocs;
            }
        final List<UserRegistration.UserDocCommand> profileList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> activationList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> ninList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> passportList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> visaList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> pdfList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> refugeeList = new ArrayList<UserRegistration.UserDocCommand>();
        final List<UserRegistration.UserDocCommand> fingerPrintsList = new ArrayList<UserRegistration.UserDocCommand>();


        if (rowItem.userInfo.registrationType.equals("company")) {
            for (UserRegistration.UserDocCommand docCommand : userDocCommandList) {
                switch (docCommand.docFormat) {
                    case "pdf":
                        pdfList.add(docCommand);
                        break;
                }
                switch (docCommand.docType) {
                    case "activation":
                        activationList.add(docCommand);
                        break;
                }
            }
        }else if(rowItem.userInfo.registrationType.equals("personal")){
            for (UserRegistration.UserDocCommand docCommand : userDocCommandList) {
                switch (docCommand.docType) {
                    case "profile":
                        profileList.add(docCommand);
                        break;
                    case "activation":
                        activationList.add(docCommand);
                        break;
                    case "nin":
                        ninList.add(docCommand);
                        break;
                    case "passport":
                        passportList.add(docCommand);
                        break;
                    case "refugee":
                        refugeeList.add(docCommand);
                        break;
                    case "visa":
                        visaList.add(docCommand);
                        break;
                    case "fingerprints":
                        fingerPrintsList.add(docCommand);
                        break;
                }
            }
        }

        if (rowItem.userInfo.registrationType.equals("company")) {
            if (pdfList != null || pdfList.size() != 0)
                for (UserRegistration.UserDocCommand docData : pdfList) {

                    if (docData != null) {
                        String fileUrl = "documents/" + docData.docType + "/" + rowItem.userInfo.userId + "/," + (docData.displayName.toString()).replace(" ", "_") + ("_" + randomNumber);
                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();

                        uploadImageServiceHandler.uploadPdf("pdf", fileUrl, docData.pdfRwaData.toString(), new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin login1 = (UserLogin) data.get(0);
                                if (login1.status.equals("success")) {
                                    if (++pdfDocUploadedCount == pdfList.size()) {
                                        MyToast.makeMyToast(activity, "Company Documents Uploaded Successfully.", Toast.LENGTH_LONG);
                                    }
                                } else {

                                    MyToast.makeMyToast(activity, "Company Documents not Uploaded.", Toast.LENGTH_LONG);
                                }

                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                // ProgressDialogUtil.stopProgressDialog(progressDialog);
                                activity.finish();

                            }
                        });
                    }
                }

            if (activationList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/activation/" + rowItem.userInfo.userId + "/";


                if (activationList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : activationList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == activationList.size()) {
                                        activationList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }
        } else if (rowItem.userInfo.registrationType.equals("personal")) {

            // upload NIN Images

            if (ninList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/nin/" + rowItem.userInfo.userId + "/";

                if (ninList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : ninList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == ninList.size()) {
                                        ninList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }

            // Upload Profile

            if (profileList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/profile/" + rowItem.userInfo.userId + "/";

                if (profileList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : profileList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == profileList.size()) {
                                        profileList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }
            // upload Activation Doc
            if (activationList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/activation/" + rowItem.userInfo.userId + "/";


                if (activationList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : activationList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == activationList.size()) {
                                        activationList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }
            //upload Passport
            if (passportList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/passport/" + rowItem.userInfo.userId + "/";


                if (passportList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : passportList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == passportList.size()) {
                                        passportList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }
            // upload Visa

            if (visaList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/visa/" + rowItem.userInfo.userId + "/";

                if (visaList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : visaList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == visaList.size()) {
                                        visaList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }

            // upload Refugee
            if (refugeeList.size() != 0) {
                int passportDocsNum = 0;
                String passportPicDir = ":documents/refugee/" + rowItem.userInfo.userId + "/";

                if (refugeeList.size() != 0)
                    for (UserRegistration.UserDocCommand docData : refugeeList) {

                        String[] file =docData.docFiles.split(";");
                        String filename = file[0];

                        RestServiceHandler uploadImageServiceHandler = new RestServiceHandler();
                        uploadImageServiceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docData.imageData, new RestServiceHandler.Callback() {
                            @Override
                            public void success(DataModel.DataType type, List<DataModel> data) {
                                UserLogin userLogin = (UserLogin) data.get(0);
                                if (userLogin.status.equals("success")) {

                                    if (++passportUploadSuccessCount == refugeeList.size()) {
                                        refugeeList.clear();
                                        MyToast.makeMyToast(activity, "Images uploaded.", Toast.LENGTH_LONG);
                                    }

                                }
                            }

                            @Override
                            public void failure(RestServiceHandler.ErrorCode error, String status) {
                                //   ProgressDialogUtil.stopProgressDialog(progressDialog);
                            }
                        });
                    }
            }

            if(fingerPrintsList.size() != 0){
                String passportPicDir = ":documents/fingerprints/" + rowItem.userInfo.userId + "/";

                for(UserRegistration.UserDocCommand docCommand: fingerPrintsList){

                    String[] file =docCommand.docFiles.split(";");
                    String filename = file[0];

                    RestServiceHandler serviceHandler = new RestServiceHandler();
                    serviceHandler.uploadPdf("jpeg", passportPicDir + "," + filename, docCommand.imageData, new RestServiceHandler.Callback() {
                        @Override
                        public void success(DataModel.DataType type, List<DataModel> data) {
                            if(++fingerprintUploadSuccessCount ==fingerPrintsList.size()){
                                fingerPrintsList.clear();
                                MyToast.makeMyToast(activity,"Fingerprint documents are uploaded successfully.", Toast.LENGTH_SHORT);
                            }
                        }

                        @Override
                        public void failure(RestServiceHandler.ErrorCode error, String status) {
                        }
                    });
                }

            }


        }
    }
}
