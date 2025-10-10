package com.example.explicitapp3.ToolsNLP.Handlers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

public class SVM_Weight_Handler {
    double[] coefficients;
    double intercept;

    public void loadWeights(InputStream inputStream) throws JSONException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        JSONObject tokenizerJson = new JSONObject(jsonString);
        JSONObject svm_params = tokenizerJson.getJSONObject("svm_params");
        JSONArray coefArray = svm_params.getJSONArray("coefficients").getJSONArray(0);
        coefficients = new double[coefArray.length()];
        for (int i = 0; i < coefArray.length(); i++) {
            coefficients[i] = coefArray.getDouble(i);
        }
        intercept = svm_params.getJSONArray("intercept").getDouble(0);
    }

    public double getIntercept() {
        return intercept;
    }

    public double[] getCoefficients() {
        return coefficients;
    }
}
