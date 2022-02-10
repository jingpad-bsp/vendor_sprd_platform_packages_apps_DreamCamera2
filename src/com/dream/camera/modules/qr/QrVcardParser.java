package com.dream.camera.modules.qr;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.android.camera.debug.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QrVcardParser {
    public static final Log.Tag TAG = new Log.Tag("QrVcardParser");
    public static QrCard parser(String ini) {
        QrCard qrCard = new QrCard();
        try {
        String in = ini.replaceAll("BEGIN:","");
        Pattern p = Pattern.compile("VCARD(\\r\\n)([\\s\\S\\r\\n\\.]*?)END:VCARD");
        //Pattern p = Pattern.compile("VCARD(\\r\\n)([\\s\\S\\r\\n\\.]*?)END:VCARD");

            Matcher m = p.matcher(in);
            while (m.find()) {
                String str = m.group(0);
                Pattern pn = Pattern.compile("N:([\\s*\\S*\\w+\\W]*?)([\\r\\n])");
                Matcher mn = pn.matcher(m.group(0));
                while (mn.find()) {
                    String name = "";
                    name = mn.group(0);
                    Log.d(TAG,"name n:" + name);
                    boolean isVersion2 = name.substring("N:".length()).replaceAll("\r|\n", "").equals("2.1");
                    boolean isVersion3 = name.substring("N:".length()).replaceAll("\r|\n", "").equals("3.0");
                    boolean isVersion4 = name.substring("N:".length()).replaceAll("\r|\n", "").equals("4.0");
                    if(!(isVersion3) && !(isVersion2) && !(isVersion4)){
                        String nameValue = name.substring("N:".length()).replaceAll("\r|\n", "");
                        if (nameValue.length() > 0) {
                            Log.d(TAG,"name value:" + name);
                            qrCard.setName(nameValue);
                        }
                    }
                }

                String cell = "";
                Pattern p1 = Pattern.compile("TEL:\\d*([\\s*\\d*\\s*\\d*\\s*\\d*\\S*]*?)([\\r\\n])");
                Matcher m1 = p1.matcher(str);
                while (m1.find()) {
                    String tel = m1.group(0);
                    cell = tel.substring("TEL:".length());
                    cell.replaceAll("\r|\n", "");
                }
                if (cell.length()> 0) {
                    Log.d(TAG,"cell:"+cell);
                    qrCard.addTelephone(cell);
                }

                String work = "";
                Pattern p2 = Pattern.compile("TEL;TYPE=WORK\\d*([\\s*\\d*\\s*\\d*\\s*\\d*\\S*]*?)([\\r\\n])");
                Matcher m2 = p2.matcher(str);
                while (m2.find()) {
                    if (m2.group(0).contains("VOICE")) {
                        work = m2.group(0).substring(m2.group(0).indexOf("TEL;TYPE=WORK,VOICE:") + "TEL;TYPE=WORK,VOICE:".length());
                    }else if (!(m2.group(0).contains("FAX"))){
                        work = m2.group(0).substring(m2.group(0).indexOf("TEL;TYPE=WORK:") + "TEL;TYPE=WORK:".length());
                    }

                }
                if (work.length() > 0) {
                    qrCard.addTelephone(work);
                }

                String home = "";
                Pattern p3 = Pattern.compile("TEL;TYPE=HOME\\d*([\\s*\\d*\\s*\\d*\\s*\\d*\\S*]*?)([\\r\\n])");
                Matcher m3 = p3.matcher(str);
                while (m3.find()) {
                    if (m3.group(0).contains("VOICE")) {
                        home = m3.group(0).substring(m3.group(0).indexOf("TEL;TYPE=HOME,VOICE:") + "TEL;TYPE=HOME,VOICE:".length());
                    }else if (!m3.group(0).contains("FAX")) {
                        home = m3.group(0).substring(m3.group(0).indexOf("TEL;TYPE=HOME:") + "TEL;TYPE=HOME:".length());
                    }
                }
                if (home.length() > 0) {
                Log.d(TAG,"home:"+home);
                    qrCard.addTelephone(home);
                }

                String title = "";
                Pattern p4 = Pattern.compile("TITLE([\\s*\\S*\\w\\W\\.]*?)([\\r\\n])");
                Matcher m4 = p4.matcher(str);
                while (m4.find()) {
                    if (m4.group(0).contains("CHARSET=UTF-8")) {
                        title = m4.group(0).substring(("TITLE;CHARSET=UTF-8:".length()));
                    }else {
                        title = m4.group(0).substring(("TITLE:".length()));
                    }
                }
                Log.d(TAG,"title:"+title);
                if (title.length() > 0) {
                    qrCard.setTitle(title);
                }

                String addresshome = "";
                Pattern p5 = Pattern.compile("ADR;TYPE=HOME:([\\s\\S\\r\\n\\.]*?)([\\r\\n])");
                Matcher m5 = p5.matcher(str);
                while (m5.find()) {
                    String addresshome1 = m5.group(0);
                    Log.d(TAG,"addresshome1:"+addresshome1);
                    addresshome = addresshome1.substring(("ADR;TYPE=HOME:".length()));
                }
                if (addresshome.length() > 0){
                    qrCard.setAddress(addresshome);
                }

                String addr = "";
                Pattern p11 = Pattern.compile("ADR:([\\s\\S\\r\\n\\.]*?)([\\r\\n])");
                Matcher m11 = p11.matcher(str);
                while (m11.find()) {
                    String addre = m11.group(0);
                    Log.d(TAG,"addr:"+addre);
                    addr = addre.substring(("ADR:;;".length()));
                }
                if (addr.length() > 0){
                    qrCard.setAddress(addr);
                }

                String addresswork = "";
                Pattern p6 = Pattern.compile("ADR;TYPE=WORK([\\s\\S\\r\\n\\.]*?)([\\r\\n])");
                Matcher m6 = p6.matcher(str);
                while (m6.find()) {
                    if (m6.group(0).contains("CHARSET=UTF-8")) {
                        addresswork = m6.group(0).substring(("ADR;TYPE=WORK;CHARSET=UTF-8:;;".length()));
                    } else {
                        addresswork = m6.group(0).substring(("ADR;TYPE=WORK:".length()));
                    }
                }
                if (addresswork.length() > 0){
                    Log.d(TAG,"addresswork:"+addresswork);
                    qrCard.setAddress(addresswork);
                }

                String note = "";
                Pattern p7 = Pattern.compile("NOTE([\\s\\S\\r\\n\\.]*?)([\\r\\n])");
                Matcher m7 = p7.matcher(str);
                while (m7.find()) {
                     if (m7.group(0).contains("CHARSET=UTF-8")){
                         note = m7.group(0).substring(("NOTE;CHARSET=UTF-8:".length()));
                     }else {
                         note = m7.group(0).substring(("NOTE:".length()));
                     }
                }
                if (note.length() > 0){
                    qrCard.setNote(note);
                }

                String email = "";
                Pattern p8 = Pattern.compile("\\w+(\\.\\w+)*@\\w+(\\.\\w+)+");
                Matcher m8 = p8.matcher(str);
                while (m8.find()) {
                    email = m8.group(0);
                    Log.d(TAG,"email:"+email);
                    if (email.length() > 0) {
                        qrCard.addEmail(email);
                    }
                }


                String org = "";
                Pattern p9 = Pattern.compile("ORG([\\s\\S\\r\\n\\.]*?)([\\r\\n])");
                Matcher m9 = p9.matcher(str);
                while (m9.find()) {
                    if (m9.group(0).contains("CHARSET=UTF-8")) {
                        org = m9.group(0).substring(("ORG;CHARSET=UTF-8:".length()));
                    }else {
                        org = m9.group(0).substring(("ORG:".length()));
                    }
                    Log.d(TAG,"org:"+org);
                }
                if (org.length()> 0){
                    qrCard.setCompany(org);
                }
                String cell2 = "";
                Pattern p10 = Pattern.compile("TEL;TYPE=CELL:\\d*([\\s*\\d*\\s*\\d*\\s*\\d*\\S*]*?)([\\r\\n])");
                Matcher m10 = p10.matcher(str);
                while (m10.find()) {
                    String cellNoVoice = m10.group(0);
                    Log.d(TAG,"cell type:"+cellNoVoice);
                    cell2 = cellNoVoice.substring(("TEL;TYPE=CELL:".length()));
                }
                if (cell2.length() > 0){
                    qrCard.addTelephone(cell2);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return qrCard;
    }
}