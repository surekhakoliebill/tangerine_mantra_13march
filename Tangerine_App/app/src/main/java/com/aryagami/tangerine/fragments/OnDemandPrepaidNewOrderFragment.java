package com.aryagami.tangerine.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aryagami.R;
import com.aryagami.data.Account;
import com.aryagami.data.DataModel;
import com.aryagami.data.NewOrderCommand;
import com.aryagami.data.RegistrationData;
import com.aryagami.restapis.RestServiceHandler;
import com.aryagami.tangerine.activities.NavigationMainActivity;
import com.aryagami.tangerine.activities.OnDemandExistingAccountDetailsActivity;
import com.aryagami.tangerine.activities.OnDemandRegistrationActivity;
import com.aryagami.util.MyToast;
import com.aryagami.util.ProgressDialogUtil;
import com.aryagami.util.ReDirectToParentActivity;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OnDemandPrepaidNewOrderFragment extends Fragment {

    NewOrderCommand newOrderCommandData;
    RadioGroup radioGroup;
    RadioButton createNewAccountRadioButton, existingAccountRadioButton;
    SearchableSpinner accountSpinner;
    Account accounts[];
    List<String> accountList;
    String accountList1[];
    LinearLayout accountSetupLayout;
    Button cancel,saveAndContinue;
    ProgressDialog progressDialog;
    CheckBox mobileMoneyRegCheckbox;
    // AccountDetails
    LinearLayout accountDetailsLayout;
    TextView firstName, surName, identity;
    CheckBox verifyUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ondemand_fragment_prepaid_new_order, container, false);
        RegistrationData.setIsPassportScan(false);
        RegistrationData.setUserThumbImageDrawable(null);
        RegistrationData.setUserIndexImageDrawable(null);


        if(NewOrderCommand.getOnDemandNewOrderCommand() != null){
            newOrderCommandData = NewOrderCommand.getOnDemandNewOrderCommand();
        }else{
            newOrderCommandData = new NewOrderCommand();
        }


        newOrderCommandData.isPostpaid = false;
        accountSetupLayout = (LinearLayout)view.findViewById(R.id.existing_account_container1);
        radioGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
        createNewAccountRadioButton = (RadioButton)view.findViewById(R.id.create_new_acc_radio_btn);
        existingAccountRadioButton = (RadioButton)view.findViewById(R.id.existing_acc_radio_btn);
        accountSpinner = (SearchableSpinner)view.findViewById(R.id.item_one_spinner);
        accountSpinner.setTitle("Select Account");

        cancel = (Button)view.findViewById(R.id.cancel_btn);
        saveAndContinue = (Button)view.findViewById(R.id.save_and_continue);

        // to show Account details
        firstName = (TextView)view.findViewById(R.id.acc_first_name);
        surName = (TextView)view.findViewById(R.id.acc_last_name);
        identity = (TextView)view.findViewById(R.id.acc_user_identity);
        verifyUserId = (CheckBox) view.findViewById(R.id.acc_verified_user_id);
        accountDetailsLayout = (LinearLayout)view.findViewById(R.id.account_details);


        mobileMoneyRegCheckbox = (CheckBox)view.findViewById(R.id.enableMobileMoneyReg);

        if(createNewAccountRadioButton.isChecked()){
            saveAndContinue.setText(getString(R.string.continue_btn));
            accountSetupLayout.setVisibility(View.GONE);
            saveAndContinue.setVisibility(View.VISIBLE);
            accountDetailsLayout.setVisibility(View.GONE);
            newOrderCommandData.isNewAccount = true;
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.create_new_acc_radio_btn:
                        saveAndContinue.setText(getString(R.string.continue_btn));
                        accountSetupLayout.setVisibility(View.GONE);
                        saveAndContinue.setVisibility(View.VISIBLE);
                        accountDetailsLayout.setVisibility(View.GONE);
                        newOrderCommandData.isNewAccount = true;
                        break;
                    case R.id.existing_acc_radio_btn:
                        saveAndContinue.setText(getString(R.string.save_and_continue));
                        saveAndContinue.setVisibility(View.INVISIBLE);
                        accountSetupLayout.setVisibility(View.VISIBLE);

                        if(RegistrationData.getEnableMobileMoneyReg() != null) {
                            if (RegistrationData.getEnableMobileMoneyReg()) {
                                mobileMoneyRegCheckbox.setVisibility(View.VISIBLE);
                                mobileMoneyRegCheckbox.setChecked(false);
                                setAccountDetails();
                                // setMobileMoneyAccountDetails();
                            } else {
                                mobileMoneyRegCheckbox.setVisibility(View.GONE);
                                mobileMoneyRegCheckbox.setChecked(false);
                                setAccountDetails();
                            }
                        }else {
                            mobileMoneyRegCheckbox.setVisibility(View.GONE);
                            mobileMoneyRegCheckbox.setChecked(false);
                            setAccountDetails();
                        }
                        newOrderCommandData.isNewAccount = false;

                        break;
                }

            }
        });

        mobileMoneyRegCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if(checked){
                    accountDetailsLayout.setVisibility(View.GONE);
                    saveAndContinue.setVisibility(View.INVISIBLE);
                    verifyUserId.setChecked(false);
                    setMobileMoneyAccountDetails();
                }else{
                    accountDetailsLayout.setVisibility(View.GONE);
                    saveAndContinue.setVisibility(View.INVISIBLE);
                    verifyUserId.setChecked(false);
                    setAccountDetails();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
                Intent intent = new Intent(getActivity(), NavigationMainActivity.class);
                startActivity(intent);
            }
        });

        saveAndContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                newOrderCommandData.contract = new NewOrderCommand.ContractCommand();

                if(existingAccountRadioButton.isChecked()){
                    newOrderCommandData.isNewAccount= false;
                    NewOrderCommand.setOnDemandNewOrderCommand(newOrderCommandData);

                    Intent intent = new Intent(getActivity(), OnDemandExistingAccountDetailsActivity.class);
                    intent.putExtra("position", accountSpinner.getSelectedItemPosition());
                    startActivity(intent);
                }
                if(createNewAccountRadioButton.isChecked()){
                    newOrderCommandData.isNewAccount= true;
                    newOrderCommandData.isPostpaid = false;
                    NewOrderCommand.setOnDemandNewOrderCommand(newOrderCommandData);
                    RegistrationData.setRefugeeThumbEncodedData(null);

                    Intent intent = new Intent(getActivity(), OnDemandRegistrationActivity.class);
                    startActivity(intent);
                }
            }
        });


       /* verifyUserId.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    saveAndContinue.setVisibility(View.VISIBLE);
                }else{
                    saveAndContinue.setVisibility(View.INVISIBLE);
                }
            }
        });
*/
       /* accountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                if(accounts != null){


                    if(accounts.length > position){

                        Account accountDetails = new Account();
                        accountDetails = accounts[position];

                        if(accountDetails.userInfo != null)
                        if(accountDetails.userInfo.registrationType != null) {
                            accountDetailsLayout.setVisibility(View.VISIBLE);
                            if (accountDetails.userInfo.registrationType.equals("personal")) {

                                if (accountDetails.userInfo.nationalIdentity != null) {

                                    if (accountDetails.userInfo.nationalIdentity.equals("Passport No")) {

                                        if (accountDetails.userInfo.identityNumber != null) {
                                            identity.setText("Passport No : " + accountDetails.userInfo.identityNumber);
                                        } else {
                                            identity.setVisibility(View.GONE);
                                        }

                                    } else if (accountDetails.userInfo.nationalIdentity.equals("Refugee")) {

                                        if (accountDetails.userInfo.identityNumber != null) {
                                            identity.setText("Refugee ID: " + accountDetails.userInfo.identityNumber);
                                        } else {
                                            identity.setVisibility(View.GONE);
                                        }

                                    } else if (accountDetails.userInfo.nationalIdentity.equals("Ugandan NationalID")) {

                                        if (accountDetails.userInfo.identityNumber != null) {
                                            identity.setText("Ugandan National ID : " + accountDetails.userInfo.identityNumber);
                                        } else {
                                            identity.setVisibility(View.GONE);
                                        }
                                    }

                                    if (accountDetails.userInfo.fullName != null) {
                                        firstName.setText("FirstName : " + accountDetails.userInfo.fullName);
                                    } else {
                                        firstName.setVisibility(View.GONE);
                                    }
                                    if (accountDetails.userInfo.surname != null) {
                                        surName.setVisibility(View.VISIBLE);
                                        surName.setText("Surname : " + accountDetails.userInfo.surname);
                                    } else {
                                        surName.setVisibility(View.GONE);
                                    }


                                }

                            } else if (accountDetails.userInfo.registrationType.equals("company")) {

                                if(accountDetails.userInfo.userGroup != null)
                                if(accountDetails.userInfo.userGroup.equals("Mobile Money Agent")){

                                    if (accountDetails.userInfo.company != null) {
                                        firstName.setText("Company Name : " + accountDetails.userInfo.company);
                                    } else {
                                        firstName.setVisibility(View.GONE);
                                    }

                                    if (accountDetails.userInfo.mobileMoneyUserType != null) {
                                        surName.setText("UserType : " + accountDetails.userInfo.mobileMoneyUserType);
                                    } else {
                                        surName.setText("UserType : Not Found");
                                    }

                                    if (accountDetails.userInfo.tinNumber != null) {
                                        identity.setText("TIN Number : " + accountDetails.userInfo.tinNumber);
                                    } else {
                                        if(accountDetails.userInfo.coiNumber != null){
                                            identity.setText("COI Number : " + accountDetails.userInfo.coiNumber);
                                        }else{
                                            identity.setText("TIN Number : Not Found"  );
                                        }
                                    }

                                }else{
                                    surName.setVisibility(View.GONE);
                                    if (accountDetails.userInfo.company != null) {
                                        firstName.setText("Company Name : " + accountDetails.userInfo.company);
                                    } else {
                                        firstName.setVisibility(View.GONE);
                                    }

                                    if (accountDetails.userInfo.tinNumber != null) {
                                        identity.setText("TIN Number : " + accountDetails.userInfo.tinNumber);
                                    } else {
                                        if(accountDetails.userInfo.coiNumber != null){
                                            identity.setText("COI Number : " + accountDetails.userInfo.coiNumber);
                                        }else{
                                            identity.setText("TIN Number : Identity Not Found"  );
                                        }
                                    }
                                }

                            }
                        }else{
                            accountDetailsLayout.setVisibility(View.GONE);
                        }
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
       */
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        //CheckNetworkConnection.cehckNetwork(getActivity());
    }
    private void setAccountDetails() {
        /*if(NewOrderCommand.getAccount() != null){

            accountList = new ArrayList<String>();
            for (Account acc : NewOrderCommand.getAccount()) {
                accountList.add(acc.username + "-" + acc.accountId);
            }

            accountList1 = new String[accountList.size()];
            accountList.toArray(accountList1);

            if (accountList1 != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, accountList1);
                adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                accountSpinner.setAdapter(adapter);
                saveAndContinue.setVisibility(View.VISIBLE);
            } else {
                saveAndContinue.setVisibility(View.INVISIBLE);
                Toast.makeText(getActivity(), "EMPTY DATA", Toast.LENGTH_SHORT).show();
            }
        }else {*/
        RestServiceHandler serviceHandler = new RestServiceHandler();
        try {
            progressDialog = ProgressDialogUtil.startProgressDialog(getActivity(),"Please wait, Fetching Active Accounts...");

            serviceHandler.getAccountDetails(new RestServiceHandler.Callback() {
                @Override
                public void success(DataModel.DataType type, List<DataModel> data) {
                    if (data != null) {
                            /*accounts = new Account[data.size()];
                            data.toArray(accounts);

                            NewOrderCommand.setAccount(accounts);

                            accountList = new ArrayList<String>();
                            for (Account acc : accounts) {
                                accountList.add(acc.username + "-" + acc.accountId);
                            }

                            accountList1 = new String[accountList.size()];
                            accountList.toArray(accountList1);

                            if (accountList1 != null) {
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, accountList1);
                                adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                                accountSpinner.setAdapter(adapter);
                                saveAndContinue.setVisibility(View.VISIBLE);
                                ProgressDialogUtil.stopProgressDialog(progressDialog);
                            } else {
                                saveAndContinue.setVisibility(View.INVISIBLE);
                                Toast.makeText(getActivity(), "Empty Data!", Toast.LENGTH_SHORT).show();
                            }*/

                        Account account = (Account)data.get(0);
                        if(account != null){
                            if(account.status != null)
                                if(account.status.equals("success")){

                                    accounts = new Account[account.accountList.size()];
                                    account.accountList.toArray(accounts);

                                    NewOrderCommand.setAccount(accounts);

                                    accountList = new ArrayList<String>();
                                    for(Account acc : accounts){
                                        accountList.add(acc.username+"-"+acc.accountId);
                                    }

                                    accountList1 = new String[accountList.size()];
                                    accountList.toArray(accountList1);

                                    if(accountList1 != null) {

                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, accountList1);
                                        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                                        accountSpinner.setAdapter(adapter);
                                        saveAndContinue.setVisibility(View.VISIBLE);
                                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                                    }else{
                                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                                        saveAndContinue.setVisibility(View.INVISIBLE);
                                        Toast.makeText(getActivity(),"Empty Data",Toast.LENGTH_SHORT).show();
                                    }
                                }else if(account.status.equals("INVALID_SESSION")){
                                    ReDirectToParentActivity.callLoginActivity(getActivity());
                                }else{
                                    MyToast.makeMyToast(getActivity(),"Status:"+account.status,Toast.LENGTH_SHORT);
                                }


                        }
                    }
                }

                @Override
                public void failure(RestServiceHandler.ErrorCode error, String status) {
                    ProgressDialogUtil.stopProgressDialog(progressDialog);
                    saveAndContinue.setVisibility(View.INVISIBLE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void setMobileMoneyAccountDetails() {
        /*if(NewOrderCommand.getAccount() != null){

            accountList = new ArrayList<String>();
            for (Account acc : NewOrderCommand.getAccount()) {
                accountList.add(acc.username + "-" + acc.accountId);
            }

            accountList1 = new String[accountList.size()];
            accountList.toArray(accountList1);

            if (accountList1 != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, accountList1);
                adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                accountSpinner.setAdapter(adapter);
                saveAndContinue.setVisibility(View.VISIBLE);
            } else {
                saveAndContinue.setVisibility(View.INVISIBLE);
                Toast.makeText(getActivity(), "EMPTY DATA", Toast.LENGTH_SHORT).show();
            }
        }else {*/
        RestServiceHandler serviceHandler = new RestServiceHandler();
        try {
            progressDialog = ProgressDialogUtil.startProgressDialog(getActivity(),"Please wait, fetching Mobile Money Accounts");

            serviceHandler.getMobileMoneyAccountDetails(new RestServiceHandler.Callback() {
                @Override
                public void success(DataModel.DataType type, List<DataModel> data) {
                    if (data != null) {
                            /*accounts = new Account[data.size()];
                            data.toArray(accounts);

                            NewOrderCommand.setAccount(accounts);

                            accountList = new ArrayList<String>();
                            for (Account acc : accounts) {
                                accountList.add(acc.username + "-" + acc.accountId);
                            }

                            accountList1 = new String[accountList.size()];
                            accountList.toArray(accountList1);

                            if (accountList1 != null) {
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, accountList1);
                                adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                                accountSpinner.setAdapter(adapter);
                                saveAndContinue.setVisibility(View.VISIBLE);
                                ProgressDialogUtil.stopProgressDialog(progressDialog);
                            } else {
                                saveAndContinue.setVisibility(View.INVISIBLE);
                                Toast.makeText(getActivity(), "Empty Data!", Toast.LENGTH_SHORT).show();
                            }*/

                        Account account = (Account)data.get(0);
                        if(account != null){
                            if(account.status != null)
                                if(account.status.equals("success")){

                                    accounts = new Account[account.accountList.size()];
                                    account.accountList.toArray(accounts);

                                    NewOrderCommand.setAccount(accounts);

                                    accountList = new ArrayList<String>();
                                    for(Account acc : accounts){
                                        accountList.add(acc.username+"-"+acc.accountId);
                                    }

                                    accountList1 = new String[accountList.size()];
                                    accountList.toArray(accountList1);

                                    if(accountList1 != null) {

                                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, accountList1);
                                        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
                                        accountSpinner.setAdapter(adapter);
                                        saveAndContinue.setVisibility(View.VISIBLE);
                                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                                    }else{
                                        ProgressDialogUtil.stopProgressDialog(progressDialog);
                                        saveAndContinue.setVisibility(View.INVISIBLE);
                                        Toast.makeText(getActivity(),"Empty Data",Toast.LENGTH_SHORT).show();
                                    }
                                }else if(account.status.equals("INVALID_SESSION")){
                                    ReDirectToParentActivity.callLoginActivity(getActivity());
                                }else{
                                    MyToast.makeMyToast(getActivity(),"Status:"+account.status,Toast.LENGTH_SHORT);
                                }


                        }
                    }
                }

                @Override
                public void failure(RestServiceHandler.ErrorCode error, String status) {
                    ProgressDialogUtil.stopProgressDialog(progressDialog);
                    saveAndContinue.setVisibility(View.INVISIBLE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public  void onTrimMemory(int level) {
        System.gc();
    }
}
