package com.example.explicitapp3.Types;

public class ModelTypes {
    public static final String YOLO_V10_F16 = "yolov10_16";
    public static final String YOLO_V10_F16_MODEL = "yolov10n_float16.tflite";

    public static final String YOLO_V10_F32= "yolov10_32";
    public static final String YOLO_V10_F32_MODEL = "yolov10n_float32.tflite";

    public static final String ROBERTA_TAGALOG = "roberta_tagalog";
    public static final String ROBERTA_TAGALOG_MODEL = "roberta_tagalog_nsfw_model.tflite";
    public static final String[] ROBERTA_TAGALOG_LABELARRAY = {"safe", "nsfw"};
    public static final int ROBERTA_TAGALOG_SEQ_LEN = 256;
    public static final String ROBERTA_TAGALOG_TOKENIZER = "roberta_tagalog/tokenizer.json";


    public static final String DISTILBERT_TAGALOG = "distilbert_tagalog";
    public static final String DISTILBERT_TAGALOG_MODEL = "distilbert_tagalog/distilbert_tagalog_classification_model.tflite";
    public static final int DISTILBERT_TAGALOG_SEQ_LEN = 256;
    public static final String DISTILBERT_TAGALOG_TOKENIZER = "distilbert_tagalog/tokenizer.json";
    public static final String[] DISTILBERT_TAGALOG_LABELARRAY = {"good", "neutral", "bad"};




}
