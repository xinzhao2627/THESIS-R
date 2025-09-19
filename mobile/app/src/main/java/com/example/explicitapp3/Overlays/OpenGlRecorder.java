package com.example.explicitapp3.Overlays;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OpenGlRecorder {
    private static final String TAG = "GLSurfaceCapture";

    private final int width;
    private final int height;
    private final int densityDpi;
    private final MediaProjection mediaProjection;
    private final Context mcontext;
    private HandlerThread glThread;
    private Handler glHandler;

    // EGL and GL state
    private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;

    private int textureId = -1;
    private SurfaceTexture surfaceTexture;
    private Surface surfaceForVirtualDisplay;
    private VirtualDisplay virtualDisplay;

    // PBO ring
    private int[] pboIds;
    private int pboIndex = 0;
    private static final int PBO_COUNT = 2;

    public OpenGlRecorder(MediaProjection mp, int w, int h, int density, Context mcontext) {
        this.mediaProjection = mp;
        this.width = w;
        this.height = h;
        this.densityDpi = density;
        this.mcontext = mcontext;
    }

    public void start() {
        glThread = new HandlerThread("gl-thread");
        glThread.start();
        glHandler = new Handler(glThread.getLooper());

        glHandler.post(() -> {
            initEGL();
            createGLObjects();
            createVirtualDisplaySurface();
        });
    }

    public void stop() {
        glHandler.post(() -> {
            destroyVirtualDisplay();
            destroyGLObjects();
            releaseEGL();
        });
        glThread.quitSafely();
    }

    // ----------------------
    // EGL & GL setup
    // ----------------------
    private void initEGL() {
        // Create EGL context (EGL14 API)
        int[] attribList = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
        };
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] majorMinor = new int[2];
        EGL14.eglInitialize(eglDisplay, majorMinor, 0, majorMinor, 1);

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfig, 0);
        EGLConfig eglConfig = configs[0];

        int[] ctxAttrib = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0);

        // Create a pbuffer surface (offscreen) to make context current
        int[] pbufferAttrib = {
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
        };
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, pbufferAttrib, 0);

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
    }

    private void releaseEGL() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface);
            if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext);
            EGL14.eglTerminate(eglDisplay);
        }
        eglDisplay = EGL14.EGL_NO_DISPLAY;
        eglContext = EGL14.EGL_NO_CONTEXT;
        eglSurface = EGL14.EGL_NO_SURFACE;
    }

    // ----------------------
    // GL objects, SurfaceTexture and PBOs
    // ----------------------
    private void createGLObjects() {
        // Create external texture
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        textureId = tex[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Create SurfaceTexture bound to this texture
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setDefaultBufferSize(width, height);

        // Create surface to hand to VirtualDisplay
        surfaceForVirtualDisplay = new Surface(surfaceTexture);

        // Setup PBOs if GLES30 available
        String version = GLES20.glGetString(GLES20.GL_VERSION);
        Log.d(TAG, "GL version: " + version);
        // Try to create PBOs; if not available, catch GL errors.
        try {
            pboIds = new int[PBO_COUNT];
            GLES30.glGenBuffers(PBO_COUNT, pboIds, 0);
            int bytes = width * height * 4;
            for (int i = 0; i < PBO_COUNT; i++) {
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pboIds[i]);
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, bytes, null, GLES30.GL_STREAM_READ);
            }
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
            Log.d(TAG, "PBOs created: " + pboIds[0] + ", " + pboIds[1]);
        } catch (Throwable t) {
            Log.w(TAG, "PBOs not available or failed: " + t.getMessage());
            pboIds = null;
        }

        // Listen for frame available
        surfaceTexture.setOnFrameAvailableListener(st -> {
            // Post a task to GL handler to update texture and render
            glHandler.post(this::onFrame);
        }, glHandler);
    }

    private void destroyGLObjects() {
        if (surfaceForVirtualDisplay != null) {
            surfaceForVirtualDisplay.release();
            surfaceForVirtualDisplay = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if (textureId != -1) {
            int[] t = {textureId};
            GLES20.glDeleteTextures(1, t, 0);
            textureId = -1;
        }
        if (pboIds != null) {
            GLES30.glDeleteBuffers(pboIds.length, pboIds, 0);
            pboIds = null;
        }
    }

    // ----------------------
    // VirtualDisplay creation
    // ----------------------
    private void createVirtualDisplaySurface() {
        // Create VirtualDisplay attached to our Surface
        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaProjection stopped");
                // Clean up resources when projection stops
                glHandler.post(() -> {
                    destroyVirtualDisplay();
                    destroyGLObjects();
                });
            }
        };
        mediaProjection.registerCallback(callback, glHandler);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "GLSurfaceCaptureDisplay",
                width,
                height,
                densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surfaceForVirtualDisplay,
                null,
                null
        );
        Log.d(TAG, "VirtualDisplay created.");
    }

    private void destroyVirtualDisplay() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    // ----------------------
    // Frame handling / rendering
    // ----------------------
    private void onFrame() {
        // Make sure EGL context is current
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

        try {
            // Update SurfaceTexture -> updates the external texture with latest frame
            surfaceTexture.updateTexImage();

            // Here you would render the external texture to a framebuffer or screen; for demo we'll read pixels.
            // Option A: GPU-only flow: render + use the texture in shaders (no readback).
            // Option B: If CPU pixels needed, do async readback using PBOs:
            if (pboIds != null) {
                asyncReadbackToPBO();
            } else {
                // fallback: blocking readPixels (slow)
                blockingReadPixels();
            }

            // Swap buffers (no onscreen surface; but keep gl state)
            // If you had a real EGL surface to show, you'd call EGL14.eglSwapBuffers(eglDisplay, eglSurfaceShowing);
        } catch (Exception e) {
            Log.e(TAG, "onFrame error: " + e.getMessage(), e);
        }
    }

    // Blocking read (bad for 60fps)
    private void blockingReadPixels() {
        // Make sure we have a framebuffer containing the texture; here we assume texture is bound to FBO or default.
        // For simplicity we read from the default framebuffer; in production you should render texture to FBO then read FBO
        int bytes = width * height * 4;
        ByteBuffer buf = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        // buf now contains pixel data (bottom-up)
        // Process pixels (offload heavy ops to another thread)
        handleCpuFrame(buf);
    }

    // Asynchronous readback using PBO ring
    private void asyncReadbackToPBO() {
        int bytes = width * height * 4;

        // bind PBO for read
        int readPbo = pboIds[pboIndex % PBO_COUNT];
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, readPbo);
        // glReadPixels writes into bound PBO (offset 0)
        GLES30.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, 0);

        // unbind pack buffer
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);

        // Advance index and attempt to map previous PBO to CPU
        int mapIndex = (pboIndex + 1) % PBO_COUNT; // the PBO we wrote to in the previous frame
        pboIndex++;

        int mapPbo = pboIds[mapIndex];
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, mapPbo);
        // Map buffer - if GPU hasn't finished writing, mapping can return null or block depending on flags;
        // use glMapBufferRange with READ_BIT to attempt non-blocking mapping.
        ByteBuffer mapped = null;
        try {
            mapped = (ByteBuffer) GLES30.glMapBufferRange(
                    GLES30.GL_PIXEL_PACK_BUFFER,
                    0,
                    bytes,
                    GLES30.GL_MAP_READ_BIT
            );
        } catch (Exception e) {
            Log.w(TAG, "PBO map failed or not ready: " + e.getMessage());
        }

        if (mapped != null) {
            // Copy to a direct ByteBuffer we can pass to other threads
            ByteBuffer copy = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
            copy.put(mapped);
            copy.rewind();

            // Unmap
            GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);

            // Handle CPU frame off-GL thread
            handleCpuFrame(copy);
        } else {
            // Mapping not ready - skip this frame; that's OK: PBO gives non-blocking behavior
            Log.d(TAG, "PBO not ready for mapping; skipping CPU read this frame.");
        }

        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0);
    }

    private void handleCpuFrame(ByteBuffer pixels) {
        // IMPORTANT: This is called on the GL thread. Offload heavy processing to a worker thread.
        // Example: hand to an Executor or HandlerThread dedicated for CPU-based processing.
        // For demo: just log the buffer capacity
//        Log.d(TAG, "Received CPU frame, bytes = " + pixels.capacity());
        // TODO: process or pass to inference, encode, etc.
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(pixels);

        if (bitmap != null) {
            try {
                Log.w(TAG, "savbing to gallery...");
                // you need content resolver in android 15
                ContentResolver resolver = mcontext.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "as_screenshot_" + System.currentTimeMillis());
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                        if (outputStream == null) {
                            throw new IOException("Failed to open output stream");
                        }
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                            throw new IOException("Failed to save bitmap");
                        }
                    }
                } else {
                    Log.w(TAG, "uri is null");
                }
            } catch (IOException e) {
                Log.e(TAG, "onSurfaceTextureUpdated: IOError " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

    }
}
