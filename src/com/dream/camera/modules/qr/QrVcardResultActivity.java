package com.dream.camera.modules.qr;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaFile;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SimpleAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.camera.debug.Log;
import com.android.camera2.R;
import com.dream.camera.util.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class QrVcardResultActivity extends Activity {

    public static final Log.Tag TAG = new Log.Tag("QrVcardResultActivity");
    public static final String KEY_MECARD = "MECARD";
    public static final String KEY_NAME = "N";
    public static final String KEY_ADDRESS = "ADR";
    public static final String KEY_TELEPHONE = "TEL";
    public static final String KEY_EMAIL = "EMAIL";
    public static final String KEY_URL = "URL";
    public static final String KEY_NOTE = "NOTE";
    //public static final String KEY_DAY = "DAY";
    public static final String KEY_ORG = "ORG";
    public static final String KEY_TIL = "TIL";

    private String mNameValue = null;
    private String mAddressValue = null;
    private String mTilValue = null;
    private String mOrgValue = null;
    private String mNoteValue = null;
    private List<String> mTelValue = null;
    private List<String> mEmailValue = null;
    private String actionBarColor = "#2e2e2e";

    private ImageView mImageView;
    private Intent mIntent;
    private ListView mInfoList;
    private SimpleAdapter mResultAdapter;
    //The field ---display map
    private Map<String, String> mItemTitleDisplayMap = new HashMap<String, String>();
    private List<Map<String,Object>> mDataLists = new ArrayList<Map<String,Object>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vcard_result);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        int color = Color.parseColor(actionBarColor);
        ColorDrawable drawable = new ColorDrawable(color);
        actionBar.setBackgroundDrawable(drawable);
        actionBar.setTitle(R.string.scan_result);

        /* create vcard key <--> title map @{*/
        String[] fieldKeyItems = getResources().getStringArray(R.array.pref_vcard_field_entries);
        String[] displayTitles = getResources().getStringArray(R.array.pref_vcard_entries);
        String fieldKeyItem = null;
        String displayTitle = null;
        for (int i = 0; i < displayTitles.length; i++) {
            fieldKeyItem= fieldKeyItems[i];
            displayTitle = displayTitles[i];
            mItemTitleDisplayMap.put(fieldKeyItem,displayTitle);
        } /* @} */

        mIntent = getIntent();
        Bundle bundle = mIntent.getExtras();
        String resultString = bundle.getString("result");
        Log.d(TAG,"resultString:"+resultString);
        if (resultString.contains("MECARD")) {
            QrCard qrmeCard = QrMecardParser.parse(resultString);
            getCardInfo(qrmeCard);
        }else if (resultString.contains("VCARD")){

            QrCard qrVcard = QrVcardParser.parser(resultString);
            getCardInfo(qrVcard);
        }

        mInfoList = (ListView)findViewById(R.id.vcard_base_item);

        mResultAdapter = new SimpleAdapter(this,mDataLists,R.layout.vcard_base_item
                ,new String[]{"title","result"}
                ,new int[]{R.id.vcard_item_title,R.id.vcard_item_value});
        mInfoList.setAdapter(mResultAdapter);

        Bitmap bitmapSafe = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.ic_scan_result_vcard);
        mImageView = (ImageView) findViewById(R.id.qrvcard_bitmap);
        mImageView.setImageBitmap(bitmapSafe);

        Button mAddNewContactButton = (Button) findViewById(R.id.vcard_add_button);
        mAddNewContactButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (mNameValue != null) {
                    intent.putExtra(ContactsContract.Intents.Insert.NAME, mNameValue);
                }

                if (mAddressValue != null) {
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL, mAddressValue);
                }

                if (mTilValue != null) {
                    intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, mTilValue);
                }

                if (mOrgValue != null) {
                    intent.putExtra(ContactsContract.Intents.Insert.COMPANY, mOrgValue);
                }

                if (mNoteValue != null) {
                    intent.putExtra(ContactsContract.Intents.Insert.NOTES, mNoteValue);
                }

                if (mEmailValue != null) {
                    String[] emails = (String[])mEmailValue.toArray(new String[mEmailValue.size()]);
                    for (int i = 0;i<emails.length;i++){
                        if (i == 0){
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, emails[i]);
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, 4);
                        }else if(i == 1){
                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, emails[i]);
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, 4);
                        }else if (i == 2){
                            intent.putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL , emails[i]);
                            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, 4);
                        }
                    }
                }

                if (mTelValue != null){
                    String[] tels = (String[])mTelValue.toArray(new String[mTelValue.size()]);
                    for (int i = 0;i<tels.length;i++){
                        if (i == 0){
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE, tels[i]);
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, 2);
                        }else if(i == 1){
                            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, tels[i]);
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, 2);
                        }else if (i == 2){
                            intent.putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, tels[i]);
                            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, 2);
                        }
                    }
                }

                startActivity(intent);
            }
        });

        Button mScanButton = (Button) findViewById(R.id.cancel_button);
        mScanButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                QrVcardResultActivity.this.finish();
            }
        });
    }

    public void getCardInfo(QrCard qrCard){

        if (qrCard != null){
            mNameValue= qrCard.getName();
            mAddressValue = qrCard.getAddress();
            mTilValue = qrCard.getTitle();
            mOrgValue = qrCard.getCompany();
            mNoteValue = qrCard.getNote();
            mTelValue = qrCard.getTelephones();
            mEmailValue = qrCard.getEmails();
        }

            if (mNameValue != null){
                Map<String,Object> tempMap =new HashMap<String, Object>();
                tempMap.put("title",mItemTitleDisplayMap.get(KEY_NAME));
                tempMap.put("result",mNameValue);
                mDataLists.add(tempMap);
            }

            if (mTelValue != null){
                String[] tels = (String[])mTelValue.toArray(new String[mTelValue.size()]);
                for (int i = 0;i<tels.length;i++){
                     Map<String,Object> tempMap =new HashMap<String, Object>();
                     tempMap.put("title",mItemTitleDisplayMap.get(KEY_TELEPHONE));
                     tempMap.put("result",tels[i]);
                     Log.d(TAG,"KEY_TEL:"+ mItemTitleDisplayMap.get(KEY_TELEPHONE)+ " ,mTelValue:" + tels[i]);
                     mDataLists.add(tempMap);
                }
            }

            if (mAddressValue != null){
                Map<String,Object> tempMap =new HashMap<String, Object>();
                tempMap.put("title",mItemTitleDisplayMap.get(KEY_ADDRESS));
                tempMap.put("result",mAddressValue);
                mDataLists.add(tempMap);
            }

            if (mOrgValue != null){
                Map<String,Object> tempMap =new HashMap<String, Object>();
                tempMap.put("title",mItemTitleDisplayMap.get(KEY_ORG));
                tempMap.put("result",mOrgValue);
                Log.d(TAG,"KEY_ORG:"+ mItemTitleDisplayMap.get(KEY_ADDRESS)+ " ,orgValue:" + mOrgValue);
                mDataLists.add(tempMap);
            }

            if (mTilValue != null){
                Map<String,Object> tempMap =new HashMap<String, Object>();
                tempMap.put("title",mItemTitleDisplayMap.get(KEY_TIL));
                tempMap.put("result",mTilValue);
                mDataLists.add(tempMap);
            }

            if (mEmailValue != null){
                String[] emails = (String[])mEmailValue.toArray(new String[mEmailValue.size()]);
                int len = emails.length;
                for (int i = 0;(i<3 && i <len);i++){
                    Map<String,Object> tempMap =new HashMap<String, Object>();
                    tempMap.put("title",mItemTitleDisplayMap.get(KEY_EMAIL));
                    tempMap.put("result",emails[i]);
                    Log.d(TAG,"KEY_EMAIL:"+ mItemTitleDisplayMap.get(KEY_EMAIL)+ " ,emailValue:" + emails[i]);
                    mDataLists.add(tempMap);
               }
            }

            if (mNoteValue != null){
                Map<String,Object> tempMap =new HashMap<String, Object>();
                tempMap.put("title",mItemTitleDisplayMap.get(KEY_NOTE));
                tempMap.put("result",mNoteValue);
                mDataLists.add(tempMap);
            }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mItemTitleDisplayMap != null){
            mItemTitleDisplayMap.clear();
        }
        if(mDataLists != null){
            mDataLists.clear();
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return true;
    }

}
