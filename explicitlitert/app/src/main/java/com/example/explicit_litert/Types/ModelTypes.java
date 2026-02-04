package com.example.explicit_litert.Types;

public class ModelTypes {
    public static final String YOLO_V10_F16 = "yolov10n";
    public static final String YOLO_V10_F16_MODEL = "best_640_float16.tflite";

    // for v12 and v11
    public static final String YOLO_V11N = "yolov11n";
    public static final String YOLO_V11N_MODEL = "best_320_float16.tflite";
//    public static final String YOLO_V11N_MODEL = "best_float16_640.tflite";


    public static final String YOLO_V5 = "yolov5n";
    public static final String YOLO_V5_MODEL = "best-fp16_640.tflite";

    public static final String YOLO_V5N_640 = "yolov5n640";
    public static final String YOLO_V5N_640_MODEL = "best-fp16_640.tflite";

    public static final String YOLO_V5N_320 = "yolov5n320";
    public static final String YOLO_V5N_320_MODEL = "best-fp16_320.tflite";

    public static final String YOLO_V5S_320 = "yolov5s320";
    public static final String YOLO_V5S_320_MODEL = "best-fp16_320.tflite";

//  FOR ROBERTA & DOST ROBERTA
//    public static final String ROBERTA_TAGALOG = "roberta_tagalog";
//    public static final String ROBERTA_TAGALOG_MODEL = "roberta_tagalog/roberta_tagalog_nsfw_model.tflite";
//    public static final String ROBERTA_TAGALOG_TOKENIZER = "roberta_tagalog/tokenizer.json";
    public static final String ROBERTA_TAGALOG = "dostroberta";
    public static final String ROBERTA_TAGALOG_MODEL = "dostroberta/nsfw_model_fp16.tflite";
    public static final String ROBERTA_TAGALOG_TOKENIZER = "dostroberta/tokenizer.json";

    public static final String[] ROBERTA_TAGALOG_LABELARRAY = {"safe", "nsfw"};
    public static final int ROBERTA_TAGALOG_SEQ_LEN = 32;


    //    FOR MOBILEBERT
    public static final String MOBILEBERT = "mobilebert";
    public static final String MOBILEBERT_MODEL = "mobilebert/mobilebert_nsfw.tflite";
    public static final int MOBILEBERT_SEQ_LEN = 128;
    public static final String MOBILEBERT_TOKENIZER = "mobilebert/vocab.txt";
    public static final String[] MOBILEBERT_LABELARRAY = {"safe", "nsfw"};

    //    FOR DISTILBERT
    public static final String DISTILBERT_TAGALOG = "distilbert_tagalog";
    public static final String DISTILBERT_TAGALOG_MODEL = "distilbert_tagalog/distilbert_tagalog_classification_model.tflite";
    public static final int DISTILBERT_TAGALOG_SEQ_LEN = 256;
    public static final String DISTILBERT_TAGALOG_TOKENIZER = "distilbert_tagalog/tokenizer.json";
    public static final String[] DISTILBERT_TAGALOG_LABELARRAY = {"safe", "nsfw"};

//    public static final String[] DISTILBERT_TAGALOG_LABELARRAY = {"nsfw", "safe"};

//    FOR TINYBERT
//    public static final String DISTILBERT_TAGALOG = "tinybert";
//    public static final String DISTILBERT_TAGALOG_MODEL = "tinybert/tinybert_nsfw.tflite";
//    public static final int DISTILBERT_TAGALOG_SEQ_LEN = 128;
//    public static final String DISTILBERT_TAGALOG_TOKENIZER = "tinybert/tokenizer.json";
//    public static final String[] DISTILBERT_TAGALOG_LABELARRAY = {"safe", "nsfw"};

    public static final String TINYBERT = "tinybert";
    public static final String TINYBERT_MODEL = "tinybert/tinybert_nsfw.tflite";
    public static final int TINYBERT_SEQ_LEN = 128;
    public static final String TINYBERT_TOKENIZER = "tinybert/tokenizer.json";
    public static final String[] TINYBERT_LABELARRAY = {"safe", "nsfw"};

    //    FOR LSTM
    public static final String LSTM = "lstm";
    public static final String LSTM_MODEL = "lstm/lstm_model_old.tflite";
    public static final String LSTM_TOKENIZER = "lstm/tokenizer_old.json";
    public static final String[] LSTM_LABELARRAY = {"nsfw", "safe"};
    public static final int LSTM_SEQ_LEN = 100;


//    public static final int LSTM_SEQ_LEN = 128;

//    FOR BILSTM
    public static final String BILSTM = "bilstm";
    public static final String BILSTM_MODEL = "bilstm/model.tflite";
    public static final String BILSTM_TOKENIZER = "bilstm/tokenizer.json";
    public static final String[] BILSTM_LABELARRAY = {"safe", "nsfw"};
    public static final int BILSTM_SEQ_LEN = 100;







    //    public static final String ROBERTA_TAGALOG = "minilm";
//    public static final String ROBERTA_TAGALOG_MODEL = "minilm/model.tflite";
//    public static final String[] ROBERTA_TAGALOG_LABELARRAY = {"safe", "nsfw"};
//    public static final int ROBERTA_TAGALOG_SEQ_LEN = 128;
//    public static final String ROBERTA_TAGALOG_TOKENIZER = "minilm/tokenizer.json";
    public static final String MINILM = "minilm";
    public static final String MINILM_MODEL = "minilm/model.tflite";
    public static final String[] MINILM_LABELARRAY = {"safe", "nsfw"};
    public static final int MINILM_SEQ_LEN = 128;
    public static final String MINILM_TOKENIZER = "minilm/tokenizer.json";




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
    public static final String MOBILENET_SSD = "mobilenet_ssd";
    //    public static final String MOBILENET_SSD_MODEL = "mobilenet_ssd1.tflite";
    public static final String MOBILENET_SSD_MODEL = "mobilenet_ssd2.tflite";

    public static final String EFFICIENTDET = "efficientdet";
    public static final String EFFICIENTDET_MODEL = "model.tflite";

}
