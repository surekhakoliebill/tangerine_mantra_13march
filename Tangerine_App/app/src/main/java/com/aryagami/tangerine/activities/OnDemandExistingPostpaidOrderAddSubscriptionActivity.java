package com.aryagami.tangerine.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aryagami.R;
import com.aryagami.data.Constants;
import com.aryagami.data.DataModel;
import com.aryagami.data.NewOrderCommand;
import com.aryagami.data.OrderNumberDetails;
import com.aryagami.data.PlanGroup;
import com.aryagami.data.RegistrationData;
import com.aryagami.data.ServiceBundleDetailVo;
import com.aryagami.data.SimReplacementForm;
import com.aryagami.data.SubscriptionCommand;
import com.aryagami.data.UserLogin;
import com.aryagami.restapis.RestServiceHandler;
import com.aryagami.util.BugReport;
import com.aryagami.util.CheckNetworkConnection;
import com.aryagami.util.MyToast;
import com.aryagami.util.ProgressDialogUtil;
import com.aryagami.util.ReDirectToParentActivity;
import com.aryagami.util.UserSession;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OnDemandExistingPostpaidOrderAddSubscriptionActivity extends AppCompatActivity {

    SearchableSpinner iccidSpinner, msisdnSpinner;
    ImageButton searchICCIDButton, searchMSISDNButton,scannedICCIDButton;
    TextInputEditText iccidText,msisdnNumberText;
    LinearLayout selectMSISDNLayout, searchMSISDINLayout, toggleButtonLayout,planGroupDetailsLayout;
    String postServiceBundleSerialNumber,postPlanGroupId,servedMSISDN,reserveMSISDN;
    NewOrderCommand newOrderCommand = new NewOrderCommand();
    Button backBtn, reserveAndContinue,scanICCIDButton;
    Activity activity = this;
    PlanGroup planGroup;
    public ProgressDialog progressDialog, progressDialog1, progressDialog2,progressDialog5;
    TextView planGroupName, planPrice, planSetupPrice, airtimeValue,bundleComponentsValue, planGroupName1, planPrice1, planSetupPrice1;
    Float totalPlanPrice = 0f, setupPrice = 0f,postAirtimeValue= 0f;
    OrderNumberDetails orderNumberDetails = new OrderNumberDetails();
    NewOrderCommand command = new NewOrderCommand();
    Boolean isSearch= false;
    List<String> iccidList= new ArrayList<String>();
    List<String> msisdnList= new ArrayList<String>();
    LinearLayout searchICCIDContainer, scanICCIDContainer, iccidContainer;
    EditText scannedICCIDText;
    List<String> bundleComponents = new ArrayList<String>();


    public  void onTrimMemory(int level) {
        System.gc();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ondemand_ex_order_addsubscription);

        orderNumberDetails = (OrderNumberDetails) getIntent().getSerializableExtra("OrderDetails");
        msisdnSpinner = (SearchableSpinner) findViewById(R.id.msisdn_spinner);
        iccidSpinner = (SearchableSpinner)findViewById(R.id.iccid_spinner);
        // Search ImageButtons
        searchICCIDButton  =  (ImageButton) findViewById(R.id.search_btn1);
        searchMSISDNButton = (ImageButton) findViewById(R.id.search1_btn);
        //Layouts
        selectMSISDNLayout = (LinearLayout) findViewById(R.id.select_msisdn_container);
        planGroupDetailsLayout = (LinearLayout) findViewById(R.id.plan_group_details_layout);
        searchMSISDINLayout = (LinearLayout) findViewById(R.id.search_msisdn_container);
        toggleButtonLayout = (LinearLayout)findViewById(R.id.toggle_container);
        //Buttons
        backBtn = (Button) findViewById(R.id.back_btn);
        reserveAndContinue = (Button) findViewById(R.id.reserve_and_continue_btn);
        reserveAndContinue.setVisibility(View.INVISIBLE);
        msisdnNumberText = (TextInputEditText) findViewById(R.id.isdn);
        iccidText = (TextInputEditText) findViewById(R.id.iccid_text);

        // After scan barcode of ICCID
        scanICCIDButton = (Button) findViewById(R.id.scan_iccid_btn);
        scannedICCIDText = (EditText) findViewById(R.id.scanned_iccid_text);
        scannedICCIDButton = (ImageButton) findViewById(R.id.search_iccid_details_btn);

        searchICCIDContainer = (LinearLayout) findViewById(R.id.search_iccid_container);
        scanICCIDContainer = (LinearLayout) findViewById(R.id.scan_iccid_container);
        iccidContainer = (LinearLayout) findViewById(R.id.iccid_container_layout);

        ToggleButton toggleButton = (ToggleButton)findViewById(R.id.toggle);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    selectMSISDNLayout.setVisibility(View.VISIBLE);
                    searchMSISDINLayout.setVisibility(View.GONE);
                    reserveAndContinue.setVisibility(View.VISIBLE);
                    isSearch = false;
                }else {
                    isSearch = true;
                    searchMSISDINLayout.setVisibility(View.VISIBLE);
                    selectMSISDNLayout.setVisibility(View.GONE);
                    reserveAndContinue.setVisibility(View.INVISIBLE);
                }
            }
        });

        ToggleButton iccidToggleButton = (ToggleButton) findViewById(R.id.iccid_toggle);
        iccidToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    searchICCIDContainer.setVisibility(View.VISIBLE);
                    scanICCIDContainer.setVisibility(View.GONE);
                    iccidContainer.setVisibility(View.GONE);
                    RegistrationData.setIsScanICCID(false);

                    planGroupDetailsLayout.setVisibility(View.GONE);
                    toggleButtonLayout.setVisibility(View.GONE);
                    selectMSISDNLayout.setVisibility(View.GONE);
                    reserveAndContinue.setVisibility(View.GONE);
                    searchMSISDINLayout.setVisibility(View.GONE);
                } else {
                    RegistrationData.setIsScanICCID(true);
                    searchICCIDContainer.setVisibility(View.GONE);
                    scanICCIDContainer.setVisibility(View.VISIBLE);
                    iccidContainer.setVisibility(View.GONE);

                    planGroupDetailsLayout.setVisibility(View.GONE);
                    toggleButtonLayout.setVisibility(View.GONE);
                    selectMSISDNLayout.setVisibility(View.GONE);
                    reserveAndContinue.setVisibility(View.GONE);
                    searchMSISDINLayout.setVisibility(View.GONE);
                }
            }
        });

        try {
            getAllAvailableSimIccIdsList();
            getAllAvailableMsisdns();
        }catch (IOException e){
            e.printStackTrace();
        }

        searchICCIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             checkICCIDAvailability();
            }
        });

        searchMSISDNButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchAvailableMSISDN();
            }
        });
        scanICCIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, BarcodeScannerActivity.class);
                startActivity(intent);
            }
        });

        scannedICCIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkICCIDAvailability();
            }
        });

        reserveAndContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RestServiceHandler serviceHandler = new RestServiceHandler();
                if(isSearch){
                        reserveMSISDN = servedMSISDN;
                }else{
                        reserveMSISDN = msisdnSpinner.getSelectedItem().toString();
                }

                if(reserveMSISDN != null && !reserveMSISDN.isEmpty()) {
                    progressDialog2 = ProgressDialogUtil.startProgressDialog(activity, "please wait, reserving MSISDN...");
                    serviceHandler.reserveMSISDN("NONE", reserveMSISDN, new RestServiceHandler.Callback() {
                        @Override
                        public void success(DataModel.DataType type, List<DataModel> data) {
                            UserLogin response = (UserLogin) data.get(0);
                            ProgressDialogUtil.stopProgressDialog(progressDialog2);
                            if(response.status.equals("success")){
                                collectSubscriptionData();
                                NewOrderCommand.setOnDemandNewOrderCommand(newOrderCommand);

                                /*  Add Multiple devices for Sim
                                Intent intent = new Intent(getApplicationContext(), AddDeviceExistingPostpaidOrderActivity.class);
                                */
                                Intent intent = new Intent(getApplicationContext(), OnDemandExistingPostpaidOrderPaymentActivity.class);
                                intent.putExtra("ExistingOrder", newOrderCommand);
                                startActivity(intent);
                            }else{
                                MyToast.makeMyToast(getApplicationContext(),"Status: "+response.status, Toast.LENGTH_SHORT);
                            }
                        }

                        @Override
                        public void failure(RestServiceHandler.ErrorCode error, String status) {
                            ProgressDialogUtil.stopProgressDialog(progressDialog2);
                            BugReport.postBugReport(activity, Constants.emailId,"Status:"+status,"OnDemand_Add_Subscription: Reserving MSISDN");
                        }
                    });
                }else{
                    MyToast.makeMyToast(getApplicationContext(),"Please select MSISDN", Toast.LENGTH_SHORT);
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finish();
                onBackPressed();
            }
        });
    }


    public void getAllAvailableSimIccIdsList() throws IOException {
        RestServiceHandler serviceHandler = new RestServiceHandler();
        String binRef = UserSession.getResellerId(getApplicationContext());
        serviceHandler.getSimIccidList(binRef, new RestServiceHandler.Callback() {
            @Override
            public void success(DataModel.DataType type, List<DataModel> data) {
                if(data != null){
                    List<ServiceBundleDetailVo> bundleDetailVos = new ArrayList<ServiceBundleDetailVo>();
                    ServiceBundleDetailVo[] bundleDetailVosArrays = new ServiceBundleDetailVo[data.size()];
                    data.toArray(bundleDetailVosArrays);

                    for(ServiceBundleDetailVo bundleDetailVo : bundleDetailVosArrays){
                        if(bundleDetailVo.simIccids != null){
                            for(String sim : bundleDetailVo.simIccids){
                                iccidList.add(sim);
                            }
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, iccidList);
                    adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                    iccidSpinner.setAdapter(adapter);

                    iccidSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            ((TextView) view).setTextColor(Color.BLACK); //Change selected text color
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });


                }else{
                    MyToast.makeMyToast(activity,"Iccidlist is Empty", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void failure(RestServiceHandler.ErrorCode error, String status) {
                BugReport.postBugReport(activity, Constants.emailId,"Error:"+error+"Status:"+status,"OnDemand - Addsubscription- getServiceBundleDetails");
            }
        });
    }

    private void searchAvailableMSISDN() {
            if (msisdnNumberText.getText().toString().length() < 9 | msisdnNumberText.getText().toString().isEmpty()) {
                MyToast.makeMyToast(activity, "Please Enter Correct MSISDN Number", Toast.LENGTH_SHORT);
            } else {
                servedMSISDN = "256" + msisdnNumberText.getText().toString();
                RestServiceHandler serviceHandler = new RestServiceHandler();
                progressDialog1 = ProgressDialogUtil.startProgressDialog(activity, "please wait,Fetching Available MSIDSN's List.");
                try {
                    serviceHandler.getCheckISDNServedMSISDN(servedMSISDN, new RestServiceHandler.Callback() {
                        @Override
                        public void success(DataModel.DataType type, List<DataModel> data) {
                            SubscriptionCommand command = (SubscriptionCommand) data.get(0);
                            ProgressDialogUtil.stopProgressDialog(progressDialog1);
                            if (command.statusReason.equals("success")) {
                                reserveAndContinue.setVisibility(View.VISIBLE);
                            } else {
                                reserveAndContinue.setVisibility(View.INVISIBLE);
                                if (command.statusReason.equals("INVALID_SESSION")) {
                                    ReDirectToParentActivity.callLoginActivity(activity);
                                } else {
                                    MyToast.makeMyToast(activity, "\nStatus:" + command.statusReason.toString(), Toast.LENGTH_SHORT);
                                }
                            }
                        }

                        @Override
                        public void failure(RestServiceHandler.ErrorCode error, String status) {
                            ProgressDialogUtil.stopProgressDialog(progressDialog1);
                            BugReport.postBugReport(activity, Constants.emailId,"STATUS"+status+"\n ERROR:-"+error,"Activity");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    BugReport.postBugReport(activity, Constants.emailId,"Message"+e.getMessage()+"\n ERROR:-"+e.getCause(),"Activity");
                }
            }

    }

    private void checkICCIDAvailability() {
        RestServiceHandler serviceHandler = new RestServiceHandler();
        String binRef = UserSession.getResellerId(getApplicationContext());
        String iccIdText="";
        if(RegistrationData.getIsScanICCID()){
            iccIdText = scannedICCIDText.getText().toString();

        }else {
            if (iccidSpinner.getSelectedItem() != null) {
                iccIdText = iccidSpinner.getSelectedItem().toString();
            }
        }
        if (!iccIdText.isEmpty()) {
            try {
                progressDialog = ProgressDialogUtil.startProgressDialog(activity, "please wait, Checking ICCID..");
                serviceHandler.getCheckIccIdAvailability(binRef, iccIdText, new RestServiceHandler.Callback() {
                    @Override
                    public void success(DataModel.DataType type, List<DataModel> data) {
                        SubscriptionCommand command = (SubscriptionCommand) data.get(0);

                        if (command.statusReason.equals("success")) {
                            postServiceBundleSerialNumber = command.serviceBundleSerialNumber;
                            postAirtimeValue = command.airtimeValue;
                            if(command.bundleList != null){
                                bundleComponents = command.bundleList;
                            }
                            postPlanGroupId = command.planGroupId.toString();
                            /*if(command.planGroupId != null){
                                getPlanGroupDetails(command.planGroupId);
                            }*/
                            planGroupDetailsLayout.setVisibility(View.VISIBLE);
                            setPlanGroupDetails(command);
                            toggleButtonLayout.setVisibility(View.VISIBLE);
                            if(!isSearch) {
                                selectMSISDNLayout.setVisibility(View.VISIBLE);
                                reserveAndContinue.setVisibility(View.VISIBLE);
                            } else{
                                searchMSISDINLayout.setVisibility(View.VISIBLE);
                                selectMSISDNLayout.setVisibility(View.GONE);
                                reserveAndContinue.setVisibility(View.INVISIBLE);
                            }
                            ProgressDialogUtil.stopProgressDialog(progressDialog);
                        }else{
                            ProgressDialogUtil.stopProgressDialog(progressDialog);
                            if (command.statusReason.equals("INVALID_SESSION")) {
                                ReDirectToParentActivity.callLoginActivity(activity);
                            } else {
                                MyToast.makeMyToast(activity, "\nStatus:" + command.statusReason.toString(), Toast.LENGTH_SHORT);
                            }
                        }
                    }

                    @Override
                    public void failure(RestServiceHandler.ErrorCode error, String status) {
                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                        BugReport.postBugReport(activity, Constants.emailId, "Error:" + error + "\n STATUS:" + status, "OnDemandAddSubscription-CheckICCIDAvailability");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                BugReport.postBugReport(activity, Constants.emailId,"Message"+e.getMessage()+"\n ERROR:-"+e.getCause(),"OnDemandAddSubscription-CheckICCIDAvailability");
            }
        } else {
            MyToast.makeMyToast(getApplication(), "Please Enter ICCID", Toast.LENGTH_SHORT);
        }
    }

    private void searchAvailableMsisdn(){

        searchMSISDNButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(UserSession.getUserGroup(activity) != null) {
                    if (msisdnNumberText.getText().toString().length() < 6 | msisdnNumberText.getText().toString().isEmpty()) {
                        MyToast.makeMyToast(activity, "Please Enter Correct MSISDN Number", Toast.LENGTH_SHORT);
                    } else {
                        servedMSISDN = "256726" + msisdnNumberText.getText().toString();

                        RestServiceHandler serviceHandler = new RestServiceHandler();
                        progressDialog1 = ProgressDialogUtil.startProgressDialog(activity, "please wait,Fetching Available MSIDSN's List.");
                        try {
                            serviceHandler.getCheckISDNServedMSISDN(servedMSISDN, new RestServiceHandler.Callback() {
                                @Override
                                public void success(DataModel.DataType type, List<DataModel> data) {
                                    SubscriptionCommand command = (SubscriptionCommand) data.get(0);
                                    ProgressDialogUtil.stopProgressDialog(progressDialog1);
                                    if (command.statusReason.equals("success")) {
                                        reserveAndContinue.setVisibility(View.VISIBLE);
                                    } else {
                                        if (command.statusReason.equals("INVALID_SESSION")) {
                                            ReDirectToParentActivity.callLoginActivity(activity);
                                        } else {
                                            MyToast.makeMyToast(activity, "\nStatus:" + command.statusReason.toString(), Toast.LENGTH_SHORT);
                                        }
                                    }
                                }

                                @Override
                                public void failure(RestServiceHandler.ErrorCode error, String status) {
                                    ProgressDialogUtil.stopProgressDialog(progressDialog1);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        });
    }

    private void getAllAvailableMsisdns() {

        if(msisdnList != null && msisdnList.size() >= 1){
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, msisdnList);
            adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
            msisdnSpinner.setAdapter(adapter);
            msisdnSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ((TextView) view).setTextColor(Color.BLACK); //Change selected text color
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            selectMSISDNLayout.setVisibility(View.GONE);
            reserveAndContinue.setVisibility(View.INVISIBLE);
        }else{
            RestServiceHandler serviceHandler = new RestServiceHandler();
            serviceHandler.getAllAvailableMSISDNList("10", new RestServiceHandler.Callback() {
                @Override
                public void success(DataModel.DataType type, List<DataModel> data) {
                    if (data != null) {
                        if (data.size() != 0) {
                            SimReplacementForm simReplacementForm = (SimReplacementForm) data.get(0);
                            if (simReplacementForm.status.equals("success")) {
                                if (simReplacementForm.msisdns != null && simReplacementForm.msisdns.size() != 0) {
                                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, simReplacementForm.msisdns);
                                    adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                                    msisdnSpinner.setAdapter(adapter);
                                    msisdnSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                        @Override
                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                            ((TextView) view).setTextColor(Color.BLACK); //Change selected text color
                                        }

                                        @Override
                                        public void onNothingSelected(AdapterView<?> parent) {
                                        }
                                    });

                                    selectMSISDNLayout.setVisibility(View.GONE);
                                    reserveAndContinue.setVisibility(View.INVISIBLE);
                                }

                            } else {
                                // ProgressDialogUtil.stopProgressDialog(progressDialog1);
                                MyToast.makeMyToast(getApplicationContext(), "Status:" + simReplacementForm.status, Toast.LENGTH_SHORT);
                                if (simReplacementForm.status.equals("INVALID_SESSION")) {
                                    ReDirectToParentActivity.callLoginActivity(activity);
                                } else {
                                    MyToast.makeMyToast(activity, "\nStatus:" + simReplacementForm.status.toString(), Toast.LENGTH_SHORT);
                                }
                            }
                        } else {
                            // ProgressDialogUtil.stopProgressDialog(progressDialog1);
                            MyToast.makeMyToast(getApplicationContext(), "EMPTY DATA", Toast.LENGTH_SHORT);
                        }
                    } else {
                        // ProgressDialogUtil.stopProgressDialog(progressDialog1);
                        MyToast.makeMyToast(getApplicationContext(), "EMPTY DATA", Toast.LENGTH_SHORT);
                    }
                }

                @Override
                public void failure(RestServiceHandler.ErrorCode error, String status) {
                    // ProgressDialogUtil.stopProgressDialog(progressDialog1);
                    BugReport.postBugReport(activity, Constants.emailId,"ERROR:"+error+"\nSTATUS:"+status,"CHECK ICCID.");
                    //MyToast.makeMyToast(getApplicationContext(), "Error:"+error, Toast.LENGTH_SHORT);
                }
            });
        }
        }

    public void getPlanGroupDetails(Long planGroupId) {
        RestServiceHandler handler = new RestServiceHandler();
        progressDialog5 = ProgressDialogUtil.startProgressDialog(activity, "please wait, Fetching Plan Details..");
        handler.getPlanGroupByID(planGroupId.toString(), new RestServiceHandler.Callback() {
            @Override
            public void success(DataModel.DataType type, List<DataModel> data) {
                planGroup = (PlanGroup) data.get(0);

                if (planGroup.status.equals("success")) {
                    postPlanGroupId = planGroup.planGroupId.toString();
                    planGroupDetailsLayout.setVisibility(View.VISIBLE);
                   // setPlanGroupDetails(planGroup);
                    toggleButtonLayout.setVisibility(View.VISIBLE);
                    if(!isSearch) {
                        selectMSISDNLayout.setVisibility(View.VISIBLE);
                        reserveAndContinue.setVisibility(View.VISIBLE);
                    } else{
                        searchMSISDINLayout.setVisibility(View.VISIBLE);
                        selectMSISDNLayout.setVisibility(View.GONE);
                        reserveAndContinue.setVisibility(View.INVISIBLE);
                    }
                    ProgressDialogUtil.stopProgressDialog(progressDialog5);
                } else {
                    planGroupDetailsLayout.setVisibility(View.GONE);
                    toggleButtonLayout.setVisibility(View.GONE);
                    MyToast.makeMyToast(activity, "PlanGroup Details Not Found. STATUS" + planGroup.status, Toast.LENGTH_SHORT);
                    ProgressDialogUtil.stopProgressDialog(progressDialog5);

                }
            }
            @Override
            public void failure(RestServiceHandler.ErrorCode error, String status) {
                ProgressDialogUtil.stopProgressDialog(progressDialog5);
                BugReport.postBugReport(activity, Constants.emailId,"STATUS"+status+"\n ERROR:-"+error,"Activity");
            }
        });


    }

    private void collectSubscriptionData() {

        SubscriptionCommand command = new SubscriptionCommand();
        List<SubscriptionCommand> subscriptionCommandList = new ArrayList<SubscriptionCommand>();

        command.active = false;
        command.isAccessRoute = false;
        command.isAutoSelectIp = false;
        command.isSingleIp = false;
        command.isFreeRecharge = false;
        command.serviceBundleSerialNumber = postServiceBundleSerialNumber;

        //command.planGroupId = ;
        if (postPlanGroupId != null) {
            command.planGroupId = Long.parseLong(postPlanGroupId);
        }

        if(isSearch){
            command.servedMSISDN = servedMSISDN;
        }else{
            command.servedMSISDN = msisdnSpinner.getSelectedItem().toString();
        }

        command.skipInventoryCheck = false;
        command.subscriptionInfo = "";

        //   SubscriptionCommand.SubscriptionPlanAddon subCommand = new SubscriptionCommand.SubscriptionPlanAddon();

        command.subPlanAddonMappings = new ArrayList<SubscriptionCommand.SubscriptionPlanAddon>();
        subscriptionCommandList.add(command);

        newOrderCommand.subscriptions = subscriptionCommandList;
        newOrderCommand.totalPlanPrice = totalPlanPrice;
        newOrderCommand.totalValue = totalPlanPrice ;
        newOrderCommand.currencyType = "";
        newOrderCommand.setupPrice = setupPrice;

        newOrderCommand.orderNo = orderNumberDetails.orderNo;
        newOrderCommand.creditLimit = orderNumberDetails.creditLimit;
        newOrderCommand.creditScore = orderNumberDetails.creditScore;
        newOrderCommand.depositValue = orderNumberDetails.depositValue;

        newOrderCommand.airtimeValue = postAirtimeValue;

        if (totalPlanPrice != null) {
            newOrderCommand.totalPlanPrice = totalPlanPrice;
        }

        //return newOrderCommand;

    }


    private void setPlanGroupDetails(SubscriptionCommand planGroup) {

        planPrice = (TextView) findViewById(R.id.plan_price);
        planGroupName = (TextView)findViewById(R.id.plan_name);
        planSetupPrice = (TextView) findViewById(R.id.setup_price);
        airtimeValue = (TextView) findViewById(R.id.airtime_value);
        bundleComponentsValue = (TextView)findViewById(R.id.bundle_list_value);

        if (planGroup.inventoryPrice != null) {
            planPrice.setText("UGX "+planGroup.inventoryPrice.toString());
            totalPlanPrice = planGroup.inventoryPrice;
        } else {
            totalPlanPrice = 0f;
            planPrice.setText("0");
        }

        if (planGroup.basePlanName != null) {
            planGroupName.setText(planGroup.basePlanName);
        } else {
            planGroupName.setText("");
        }

        /*if (planGroup.setupPrice != null) {
            planSetupPrice.setText(planGroup.currencyType.toString()+" "+planGroup.setupPrice.toString());
            setupPrice = planGroup.setupPrice;
        } else {
            planSetupPrice.setText("0");
            setupPrice = 0f;
        }*/

        airtimeValue.setText(String.valueOf(postAirtimeValue));
        if(bundleComponents.size() !=0){
            String bundleNames="";
            for(String str : bundleComponents){
                bundleNames += str+"\n";
            }
            bundleComponentsValue.setText(bundleNames.toString());
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!RegistrationData.getScanICCIDData().isEmpty()){
            iccidContainer.setVisibility(View.VISIBLE);
            scannedICCIDText.setText(RegistrationData.getScanICCIDData());
        }
        CheckNetworkConnection.cehckNetwork(OnDemandExistingPostpaidOrderAddSubscriptionActivity.this);
    }

}
