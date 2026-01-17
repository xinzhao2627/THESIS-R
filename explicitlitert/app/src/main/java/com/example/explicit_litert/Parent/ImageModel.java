package com.example.explicit_litert.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;


import com.example.explicit_litert.Detectors.Yolov11n_Detector;
import com.example.explicit_litert.Types.ClassifyResults;
import com.example.explicit_litert.Types.ModelTypes;


import java.io.IOException;

public class ImageModel {
    private static final String TAG = "ImageModel";
    Yolov11n_Detector yolov11nDetector;
    Context mcontext;
    String selectedModel;

    public ImageModel(Context mcontext, String name) throws IOException {
        this.mcontext = mcontext;
        selectedModel = name;

        if (name.equals(ModelTypes.YOLO_V11N)) {
            yolov11nDetector = new Yolov11n_Detector(mcontext, ModelTypes.YOLO_V11N + "/" + ModelTypes.YOLO_V11N_MODEL,
                    ModelTypes.YOLO_V11N + "/" + "labels.txt");
            Log.i(TAG, name);
        }
    }

    public ClassifyResults detect(Bitmap bitmap) {
        if (selectedModel.equals(ModelTypes.YOLO_V11N)) {
            Log.i(TAG, "detect: v11 running..");
            return yolov11nDetector.detect(bitmap);
        }
        return null;
    }

    public void cleanup() {

        if (yolov11nDetector != null) {
            yolov11nDetector.cleanup();
            yolov11nDetector = null;
        }
    }

}
