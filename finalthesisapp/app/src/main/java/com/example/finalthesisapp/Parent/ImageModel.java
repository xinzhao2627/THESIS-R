package com.example.finalthesisapp.Parent;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;


import com.example.finalthesisapp.Detectors.Yolov5_Detector;
import com.example.finalthesisapp.Types.ClassifyResults;
import com.example.finalthesisapp.Types.ModelTypes;

import java.io.IOException;
import java.util.ArrayList;

public class ImageModel {
    private static final String TAG = "ImageModel";
    Context mcontext;
    String selectedModel;


    Yolov5_Detector yolov5Detector;


    public ImageModel(Context mcontext, String name) throws IOException {
        this.mcontext = mcontext;
        selectedModel = name;

//        FOR YOLOV5 VARIANTS
        if (name.equals(ModelTypes.YOLO_V5)){
            yolov5Detector = new Yolov5_Detector(
                    mcontext,
                    ModelTypes.YOLO_V5 + "/" + ModelTypes.YOLO_V5_MODEL,
                    ModelTypes.YOLO_V5
            );
        }
        if (name.equals(ModelTypes.YOLO_V5N_320)){
            yolov5Detector = new Yolov5_Detector(
                    mcontext,
                    ModelTypes.YOLO_V5 + "/" + ModelTypes.YOLO_V5N_320_MODEL,
                    ModelTypes.YOLO_V5N_320
            );
        }
        if (name.equals(ModelTypes.YOLO_V5N_640)){
            yolov5Detector = new Yolov5_Detector(
                    mcontext,
                    ModelTypes.YOLO_V5 + "/" + ModelTypes.YOLO_V5N_640_MODEL,
                    ModelTypes.YOLO_V5N_640
            );
        }
        if (name.equals(ModelTypes.YOLO_V5S_320)){
            yolov5Detector = new Yolov5_Detector(
                    mcontext,
                    "yolov5s" + "/" + ModelTypes.YOLO_V5S_320_MODEL,
                    ModelTypes.YOLO_V5S_320
            );
        }

    }

    public ClassifyResults detect(Bitmap bitmap) {
        if (selectedModel.equals(ModelTypes.YOLO_V5)
                || selectedModel.equals(ModelTypes.YOLO_V5N_320)
                || selectedModel.equals(ModelTypes.YOLO_V5N_640)
                || selectedModel.equals(ModelTypes.YOLO_V5S_320)
        ){
            return yolov5Detector.detect(bitmap);
        }
        return new ClassifyResults(null, new ArrayList<>());
    }

    public void cleanup() {
        if (yolov5Detector != null) {
            yolov5Detector.cleanup();
        }
    }

}
