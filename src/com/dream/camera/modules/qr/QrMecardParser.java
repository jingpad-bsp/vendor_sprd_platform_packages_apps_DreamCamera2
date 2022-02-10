package com.dream.camera.modules.qr;

import java.util.StringTokenizer;
import com.android.camera.debug.Log;

public class QrMecardParser {
    public static final Log.Tag TAG = new Log.Tag("QrMecardParser");
    public static final String KEY_NAME = "N:";
    public static final String KEY_MECARD = "MECARD:";
    public static final String KEY_ADDRESS = "ADR:";
    public static final String KEY_TELEPHONE = "TEL:";
    public static final String KEY_EMAIL = "EMAIL:";
    public static final String KEY_URL = "URL:";
    public static final String KEY_NOTE = "NOTE:";
    public static final String KEY_TIL = "TIL:";
    public static final String KEY_TITLE = "TITLE:";
    public static final String KEY_ORG = "ORG:";

    public static String[] entryKeys = {KEY_NAME,KEY_ADDRESS,KEY_TELEPHONE,KEY_EMAIL,KEY_NOTE,KEY_TIL,KEY_ORG};
    public static QrCard parse(String meCardContent) {
        QrCard meCard = new QrCard();
        Log.d(TAG,"The mecard content is :" + meCardContent);
        if (!meCardContent.startsWith(KEY_MECARD)) {
            return null;
        }

        meCardContent = meCardContent.substring(KEY_MECARD.length(), meCardContent.length());

        StringTokenizer st = new StringTokenizer(meCardContent, ";");
        while (st.hasMoreTokens()) {
            executeParsing(meCard, st.nextToken());
        }

        return meCard;
    }

    private static void executeParsing(QrCard meCard, String tokenparsing) {

        setEntryValue(tokenparsing,meCard);
    }

    public static void setEntryValue(String tokenparsing,QrCard meCard){
        String mValue = "";
        String entryKey="";
        for (int i = 0;i < entryKeys.length;i++) {

            if (tokenparsing.startsWith(entryKeys[i])) {
                entryKey = entryKeys[i];
                mValue = tokenparsing.substring(entryKey.length(), tokenparsing.length());
            }

        }
        if (mValue.length() <= 0) {
            return;
        }

        switch (entryKey){
            case KEY_NAME:
                meCard.setName(mValue);
                break;

            case KEY_ADDRESS:
                meCard.setAddress(mValue);
                break;

            case KEY_TELEPHONE:
                meCard.addTelephone(mValue);
                break;

            case KEY_NOTE:
                meCard.setNote(mValue);
                break;

            case KEY_EMAIL:
                meCard.addEmail(mValue);
                break;

            case KEY_TIL:
                meCard.setTitle(mValue);
                break;

            case KEY_TITLE:
                meCard.setTitle(mValue);
                break;

            case KEY_ORG:
                 meCard.setCompany(mValue);
                 break;

            default:
                 break;

            }
        }
    }

