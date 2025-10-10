package com.example.explicitapp3.ToolsNLP.Handlers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

public class NaiveBayes_Weight_Handler {
    double[][] feature_log_prob;
    double[] class_log_prior;

    public double[] getClass_log_prior() {
        return class_log_prior;
    }

    public double[][] getFeature_log_prob() {
        return feature_log_prob;
    }

    public void loadWeights(InputStream inputStream) throws JSONException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        JSONObject tokenizerJson = new JSONObject(jsonString);
        JSONObject nb_params = tokenizerJson.getJSONObject("nb_params");
        JSONArray ftp = nb_params.getJSONArray("feature_log_prob");

        feature_log_prob = new double[ftp.length()][];

        for (int i = 0; i < ftp.length(); i++) {
            JSONArray ftp_inner = ftp.getJSONArray(i);
            feature_log_prob[i] = new double[ftp_inner.length()];
            for (int j = 0; j < ftp_inner.length(); j++) {
                feature_log_prob[i][j] = ftp_inner.getDouble(j);
            }
        }

        JSONArray priorArray = nb_params.getJSONArray("class_log_prior");
        class_log_prior = new double[priorArray.length()];
        for (int i = 0; i < priorArray.length(); i++) {
            class_log_prior[i] = priorArray.getDouble(i);
        }

    }

}
