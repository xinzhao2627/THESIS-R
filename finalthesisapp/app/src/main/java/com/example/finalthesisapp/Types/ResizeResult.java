package com.example.finalthesisapp.Types;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class ResizeResult {
    public ByteBuffer buffer;
    public Bitmap resizedBitmap;

    public ResizeResult(ByteBuffer buffer, Bitmap resizedBitmap) {
        this.buffer = buffer;
        this.resizedBitmap = resizedBitmap;
    }
}