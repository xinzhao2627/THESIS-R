package com.example.explicitapp3.Types;

public class ModelTypes {
    public static final String YOLO_V10_F16 = "yolov10_16";
    public static final String YOLO_V10_F16_MODEL = "yolov10n_float16.tflite";

    public static final String YOLO_V10_F32= "yolov10_32";
    public static final String YOLO_V10_F32_MODEL = "yolov10n_float32V3.tflite";

    public static final String MOBILENET_SSD = "mobilenet_ssd";
    public static final String MOBILENET_SSD_MODEL = "mobilenet_ssd.tflite";

    public static final String EFFICIENTDET = "efficientdet";
    public static final String EFFICIENTDET_MODEL = "efficient.tflite";


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


    public static final String LSTM = "lstm";
    public static final String LSTM_MODEL = "lstm/lstm_model.tflite";
    public static final String LSTM_TOKENIZER = "lstm/tokenizer.json";
    public static final int LSTM_SEQ_LEN = 128;
    public static final String[] LSTM_LABELARRAY = {"good", "nsfw"};

    public static final String LogisticRegression = "logistic_regression";
    public static final String LogisticRegression_TOKENIZER = "logistic_regression/tokenizer.json";
    public static final int LogisticRegression_SEQ_LEN = 1000;
    public static final String[] LogisticRegression_LABELARRAY = {"safe", "nsfw"};

    public static final String NaiveBayes = "naive_bayes";
    public static final String NaiveBayes_TOKENIZER = "naive_bayes/tokenizer.json";
    public static final int NaiveBayes_SEQ_LEN = 1000;
    public static final String[] NaiveBayes_LABELARRAY = {"safe", "nsfw"};

    public static final String SVM = "svm";
    public static final String SVM_TOKENIZER = "svm/tokenizer.json";
    public static final int SVM_SEQ_LEN = 1000;
    public static final String[] SVM_LABELARRAY = {"safe", "nsfw"};

}
