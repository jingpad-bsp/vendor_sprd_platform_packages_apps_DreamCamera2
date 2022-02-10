package com.dream.camera.modules.qr;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;


public class QrCard {

    private String name;

    //private String formattedName;

    private String company;

    private String title;

    private List<String> telephones = new ArrayList<>();

    private List<String> emails = new ArrayList<>();

    private String address;

    private String url;

    private String note;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    /*public String getFormattedName() {
        return formattedName;
    }

    public void setFormattedName(String formattedName) {
        this.formattedName = formattedName;
    }*/

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<String> getTelephones() {
        return telephones;
    }

    public void setTelephones(List<String> telephones) {
        this.telephones = telephones;
    }

    public void setTelephone(String oldNumber, String newNumber) {
        for (int i = 0; i < this.telephones.size(); i++) {
            if (this.telephones.get(i).equals(oldNumber)) {
                this.telephones.set(i, newNumber);
            }
        }
    }

    public void addTelephone(String telephone) {
        this.telephones.add(telephone);
    }

    public void removeTelephone(String telephone) {
        for (int i = 0; i < this.telephones.size(); i++) {
            if (this.telephones.get(i).equals(telephone)) {
                this.telephones.remove(i);
                break;
            }
        }
    }

    public void removTelephone(int index) {
        this.telephones.remove(index);
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public void setEmail(String oldEmail, String newEmail) {
        for (int i = 0; i < this.emails.size(); i++) {
            if (this.emails.get(i).equals(oldEmail)) {
                this.emails.set(i, newEmail);
            }
        }
    }

    public void addEmail(String email) {
        this.emails.add(email);
    }

    public void removeEmail(String email) {
        for (int i = 0; i < this.emails.size(); i++) {
            if (this.emails.get(i).equals(email)) {
                this.emails.remove(i);
                break;
            }
        }
    }

    public void removeEmail(int index) {
        this.emails.remove(index);
    }

}