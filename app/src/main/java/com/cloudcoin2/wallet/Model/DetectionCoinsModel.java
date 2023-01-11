package com.cloudcoin2.wallet.Model;

import android.net.Uri;

import java.net.URI;

public class DetectionCoinsModel {

    private int status;
    private Uri Uri;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Uri getUri() {
        return Uri;
    }

    public void setUri(android.net.Uri uri) {
        Uri = uri;
    }
}
