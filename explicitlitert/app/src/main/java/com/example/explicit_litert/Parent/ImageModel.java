package com.example.explicit_litert.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;


import com.example.explicit_litert.Detectors.Efficientdet_Detector;
import com.example.explicit_litert.Detectors.Yolov10_Detector;
import com.example.explicit_litert.Detectors.Yolov11n_Detector;
import com.example.explicit_litert.Detectors.Yolov5_Detector;
import com.example.explicit_litert.Types.ClassifyResults;
import com.example.explicit_litert.Types.ModelTypes;


import java.io.IOException;

public class ImageModel {
    private static final String TAG = "ImageModel";
    Context mcontext;
    String selectedModel;

    Yolov11n_Detector yolov11nDetector;
    Yolov10_Detector yolov10Detector;
    Yolov5_Detector yolov5Detector;
    Efficientdet_Detector efficientdetDetector;

    public ImageModel(Context mcontext, String name) throws IOException {
        this.mcontext = mcontext;
        selectedModel = name;

        if (name.equals(ModelTypes.YOLO_V11N)) {
            yolov11nDetector = new Yolov11n_Detector(mcontext, ModelTypes.YOLO_V11N + "/" + ModelTypes.YOLO_V11N_MODEL);
            Log.i(TAG, name);
        }

        if (name.equals(ModelTypes.EFFICIENTDET)) {
            efficientdetDetector = new Efficientdet_Detector(
                    mcontext,
                    ModelTypes.EFFICIENTDET + "/" + ModelTypes.EFFICIENTDET_MODEL
            );
        }
        if (name.equals(ModelTypes.YOLO_V10_F16)){
            yolov10Detector = new Yolov10_Detector(
                    mcontext,
                    ModelTypes.YOLO_V10_F16 + "/" + ModelTypes.YOLO_V10_F16_MODEL
            );
        }
        if (name.equals(ModelTypes.YOLO_V5)){
            yolov5Detector = new Yolov5_Detector(
                    mcontext,
                    ModelTypes.YOLO_V5 + "/" + ModelTypes.YOLO_V5_MODEL
            );
        }
    }

    public ClassifyResults detect(Bitmap bitmap) {
        if (selectedModel.equals(ModelTypes.YOLO_V11N)) {
            Log.i(TAG, "detect: v11 running..");
            return yolov11nDetector.detect(bitmap);
        }
        if (selectedModel.equals(ModelTypes.EFFICIENTDET)) {
            return efficientdetDetector.detect(bitmap);
        }
        if (selectedModel.equals(ModelTypes.YOLO_V10_F16)){
            return yolov10Detector.detect(bitmap);
        }
        if (selectedModel.equals(ModelTypes.YOLO_V5)){
            return yolov5Detector.detect(bitmap);
        }
        return null;
    }

    public void cleanup() {

        if (yolov11nDetector != null) {
            yolov11nDetector.cleanup();
            yolov11nDetector = null;
        }
        if (efficientdetDetector != null) {
            efficientdetDetector.cleanup();
            efficientdetDetector = null;
        }
        if (yolov10Detector != null) {
            yolov10Detector.cleanup();
            yolov10Detector = null;
        }
        if (yolov5Detector != null) {
            yolov5Detector.cleanup();
        }
    }

}
