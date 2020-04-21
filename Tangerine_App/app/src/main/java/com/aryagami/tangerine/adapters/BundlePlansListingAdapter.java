package com.aryagami.tangerine.adapters;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aryagami.R;
import com.aryagami.data.AppConstants;
import com.aryagami.data.Constants;
import com.aryagami.data.DataModel;
import com.aryagami.data.NewOrderCommand;
import com.aryagami.data.PlanGroup;
import com.aryagami.data.RegistrationData;
import com.aryagami.data.UserLogin;
import com.aryagami.restapis.RestServiceHandler;
import com.aryagami.tangerine.activities.BundleRechargeActivity;
import com.aryagami.tangerine.activities.NavigationMainActivity;
import com.aryagami.util.BugReport;
import com.aryagami.util.GpsUtils;
import com.aryagami.util.MyToast;
import com.aryagami.util.ProgressDialogUtil;
import com.aryagami.util.ReDirectToParentActivity;
import com.aryagami.util.UserSession;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;

public class BundlePlansListingAdapter extends ArrayAdapter {


    PlanGroup[] planGroupsList;
    private Activity activity = null;
    NewOrderCommand.LocationCoordinates coordinates = new NewOrderCommand.LocationCoordinates();
    ProgressDialog progressDialog;
    String subscriptionId;
    Double longitudeValue;
    Double latitudeValue;
    Boolean isGPS = false;
    UserLogin command;
    String getSubs;

    // for GPS Enable & Capture Location coordinates

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isContinue = false;

    UserLogin userLogin = new UserLogin();

    public BundlePlansListingAdapter(Context activity, PlanGroup[] list1) {
        super(activity, R.layout.item_list_for_bundle_plans,list1);
        this.planGroupsList = list1;
        this.activity = (Activity) activity;
        //this.activity = activity;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final PlanGroup item = (PlanGroup) getItem(position);
        final View rowView;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000); // 5 seconds

        command = new UserLogin();

        turnOnGpsLocation();
        getLocation();
        locationCallback();

        if (convertView != null){
            rowView = convertView;
        }else {
            LayoutInflater inflater = activity.getLayoutInflater();
            rowView  = inflater.inflate(R.layout.item_list_for_bundle_plans, null, true);
        }



        Button selectBtn = (Button)rowView.findViewById(R.id.select_plan);
        PlanGroup planGroup = (PlanGroup) getItem(position);





        TextView bundleName = (TextView)rowView.findViewById(R.id.bundle_name);
        if (planGroup.groupName != null){
            bundleName.setText(planGroup.groupName);
        }else {
            bundleName.setText("");
        }

        TextView description = (TextView)rowView.findViewById(R.id.description_value);
        if (planGroup.planDescription != null){
            description.setText(planGroup.planDescription);
        }else {
            description.setText("");
        }

        TextView type = (TextView)rowView.findViewById(R.id.type_value);
        if (planGroup.groupType != null){
            type.setText(planGroup.groupType);
        }else {
            type.setText("");
        }

        TextView price = (TextView)rowView.findViewById(R.id.price_value);
        if (planGroup.planPrice != null){
            price.setText(planGroup.planPrice.toString());
        }else {
            price.setText("");
        }



        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(coordinates!= null){
                    if(coordinates.longitudeValue != null && coordinates.latitudeValue != null){
                        RestServiceHandler serviceHandler = new RestServiceHandler();
                        //collectData();

                        try {
                            progressDialog = ProgressDialogUtil.startProgressDialog(activity, "Please Wait.....");
                            serviceHandler.postResellerServiceBundle(coordinates, UserSession.getResellerId(activity), RegistrationData.getSubscripiptionID(), planGroup.planGroupId, false,new RestServiceHandler.Callback() {
                                @Override
                                public void success(DataModel.DataType type, List<DataModel> data) {

                                    UserLogin amountCredit = (UserLogin) data.get(0);
                                    if (amountCredit.status.equals("success")) {

                                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                                        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                        alertDialog.setCancelable(false);
                                        alertDialog.setIcon(R.drawable.success_icon);
                                        alertDialog.setMessage(activity.getString(R.string.amount_credit));
                                        alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        dialog.dismiss();
                                                        Intent intent = new Intent(activity, NavigationMainActivity.class);
                                                        activity.startActivity(intent);
                                                    }
                                                });
                                        alertDialog.show();

                                    } else {
                                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                                        if (amountCredit.status.equals("INVALID_SESSION")) {
                                            ReDirectToParentActivity.callLoginActivity(activity);
                                        } else {

                                            if(amountCredit.immediateRecharge != null){
                                                if(amountCredit.immediateRecharge){

                                                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                                    alertDialog.setCancelable(false);
                                                    alertDialog.setTitle("Message!");
                                                    alertDialog.setMessage("You already have the same amount recharged, would you like to continue again?");
                                                    alertDialog.setPositiveButton("Yes",
                                                            new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int id) {
                                                                    dialog.dismiss();
                                                                    bundleRecharge(coordinates, UserSession.getResellerId(activity), RegistrationData.getSubscripiptionID(), planGroup.planGroupId, true);
                                                                }
                                                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                            callParentActivity();
                                                        }
                                                    });
                                                    alertDialog.show();
                                                }else{
                                                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                                    alertDialog.setCancelable(false);
                                                    alertDialog.setTitle("Message!");
                                                    alertDialog.setMessage("STATUS:" + amountCredit.status.toString());
                                                    alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                                            new DialogInterface.OnClickListener() {
                                                                public void onClick(DialogInterface dialog, int id) {
                                                                    dialog.dismiss();
                                                                }
                                                            });
                                                    alertDialog.show();
                                                }

                                            }else{
                                                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                                alertDialog.setCancelable(false);
                                                alertDialog.setTitle("Message!");
                                                alertDialog.setMessage("STATUS:" + amountCredit.status.toString());
                                                alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.dismiss();
                                                            }
                                                        });
                                                alertDialog.show();
                                            }


                                        }
                                    }
                                }

                                @Override
                                public void failure(RestServiceHandler.ErrorCode error, String status) {
                                    ProgressDialogUtil.stopProgressDialog(progressDialog);
                                    BugReport.postBugReport(activity, Constants.emailId,"ERROR"+error+"STATUS:"+status,"Activity");
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            BugReport.postBugReport(activity, Constants.emailId,"Message"+e.getMessage()+"\n ERROR:-"+e.getCause(),"Activity");
                        }

                    }
                }else{
                    MyToast.makeMyToast(activity,"Please turn on GPS Location.", Toast.LENGTH_SHORT);
                }
            }

            private void collectData() {
            }
        });


        return rowView;
    }

    private void callParentActivity() {
            activity.finish();
            Intent intent = new Intent(activity, BundleRechargeActivity.class);
            activity.startActivity(intent);

    }

    private void bundleRecharge(NewOrderCommand.LocationCoordinates coordinates, String resellerId, String subscripiptionID, String planGroupId, boolean value) {
        RestServiceHandler serviceHandler = new RestServiceHandler();
        //collectData();

        try {
            progressDialog = ProgressDialogUtil.startProgressDialog(activity, "Please Wait.....");
            serviceHandler.postResellerServiceBundle(coordinates, resellerId, subscripiptionID, planGroupId, value,new RestServiceHandler.Callback() {
                @Override
                public void success(DataModel.DataType type, List<DataModel> data) {

                    UserLogin amountCredit = (UserLogin) data.get(0);
                    if (amountCredit.status.equals("success")) {

                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                        alertDialog.setCancelable(false);
                        alertDialog.setIcon(R.drawable.success_icon);
                        alertDialog.setMessage(activity.getString(R.string.amount_credit));
                        alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                        Intent intent = new Intent(activity, NavigationMainActivity.class);
                                        activity.startActivity(intent);
                                    }
                                });
                        alertDialog.show();

                    } else {
                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                        if (amountCredit.status.equals("INVALID_SESSION")) {
                            ReDirectToParentActivity.callLoginActivity(activity);
                        } else {

                            if(amountCredit.immediateRecharge != null){
                                if(amountCredit.immediateRecharge){

                                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                    alertDialog.setCancelable(false);
                                    alertDialog.setTitle("Message!");
                                    alertDialog.setMessage("You already have the same amount recharged, would you like to continue again?");
                                    alertDialog.setPositiveButton("Yes",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss();
                                                    bundleRecharge(coordinates, resellerId, subscripiptionID, planGroupId, true);
                                                }
                                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                                    alertDialog.show();
                                }else{
                                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                    alertDialog.setCancelable(false);
                                    alertDialog.setTitle("Message!");
                                    alertDialog.setMessage("STATUS:" + amountCredit.status.toString());
                                    alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                }

                            }else{
                                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                                alertDialog.setCancelable(false);
                                alertDialog.setTitle("Message!");
                                alertDialog.setMessage("STATUS:" + amountCredit.status.toString());
                                alertDialog.setNeutralButton(activity.getResources().getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }


                        }
                    }
                }

                @Override
                public void failure(RestServiceHandler.ErrorCode error, String status) {
                    ProgressDialogUtil.stopProgressDialog(progressDialog);
                    BugReport.postBugReport(activity, Constants.emailId,"ERROR"+error+"STATUS:"+status,"Activity");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            BugReport.postBugReport(activity, Constants.emailId,"Message"+e.getMessage()+"\n ERROR:-"+e.getCause(),"Activity");
        }
    }

    public void turnOnGpsLocation(){
        new GpsUtils(activity).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });
    }


    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstants.LOCATION_REQUEST);
            return;

        } else {
            if (isContinue) {
                mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            } else {

                mFusedLocationClient.getLastLocation().addOnSuccessListener(activity, location -> {
                    if (location != null) {
                        coordinates.latitudeValue = location.getLatitude();
                        coordinates.longitudeValue = location.getLongitude();

                        MyToast.makeMyToast(activity, coordinates.latitudeValue+" * "+coordinates.longitudeValue, Toast.LENGTH_SHORT);

                    } else {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    }
                });
            }
        }
    }

    public void locationCallback() {

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        coordinates.latitudeValue = location.getLatitude();
                        coordinates.longitudeValue = location.getLongitude();
                        MyToast.makeMyToast(activity, coordinates.latitudeValue+"L"+coordinates.longitudeValue, Toast.LENGTH_SHORT);
                        //  txtLocation.setText(String.format(Locale.US, "%s L %s", coordinates.latitudeValue, coordinates.longitudeValue));

                        if (!isContinue && mFusedLocationClient != null) {
                            mFusedLocationClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };
    }




    @SuppressLint("MissingPermission")
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case 1000: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (isContinue) {
                        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    } else {


                        if (isContinue) {
                            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                        } else {
                            mFusedLocationClient.getLastLocation().addOnSuccessListener(activity, location -> {
                                if (location != null) {
                                    coordinates.latitudeValue = location.getLatitude();
                                    coordinates.longitudeValue = location.getLongitude();

                                    // MyToast.makeMyToast(activity, coordinates.latitudeValue+" p "+coordinates.longitudeValue, Toast.LENGTH_SHORT);
                                } else {
                                    mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                                }
                            });
                        }
                    }
                } else {
                    Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    public void onResume() {
        turnOnGpsLocation();
        getLocation();
        //CheckNetworkConnection.cehckNetwork(getActivity());
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AppConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }

}
