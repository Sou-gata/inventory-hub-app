package com.ubx.usdk.profile.aidl;

interface IPrinterService {
    void printText(String text, String encoding);
    void printBitmap(in android.graphics.Bitmap bitmap);
}