package com.example.explicit_litert.ToolsNLP.Handlers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

public class LogisticRegression_Weight_Handler {
    double[] coefficients;
    double intercept;

    public void loadWeights(InputStream inputStream) throws JSONException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        JSONObject tokenizerJson = new JSONObject(jsonString);
        JSONObject lr_params = tokenizerJson.getJSONObject("lr_params");
        JSONArray coefArray = lr_params.getJSONArray("coefficients").getJSONArray(0);
        coefficients = new double[coefArray.length()];
        for (int i = 0; i < coefArray.length(); i++) {
            coefficients[i] = coefArray.getDouble(i);
        }
        intercept = lr_params.getJSONArray("intercept").getDouble(0);
    }

    public double getIntercept() {
        return intercept;
    }

    public double[] getCoefficients() {
        return coefficients;
    }
}
