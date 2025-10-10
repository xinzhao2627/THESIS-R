package com.example.explicitapp3.ToolsNLP.Tokenizers;

import android.util.Log;

import com.example.explicitapp3.Types.ModelTypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class SVM_tokenizer {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");


    private Map<String, Integer> vocab;
    private Map<Integer, String> idToToken;
    private final int maxLength = ModelTypes.SVM_SEQ_LEN;
    private static final String TAG = "svm Tokenizer";

    public static class TokenizedResult {
        public long[] inputIds;

        public TokenizedResult(long[] inputIds) {
            this.inputIds = inputIds;
        }
    }

    public SVM_tokenizer(InputStream tokenizerJsonStream) {
        this.vocab = new HashMap<>();
        this.idToToken = new HashMap<>();
        try {
            loadTokenizer(tokenizerJsonStream);
        } catch (JSONException e) {
            Log.e(TAG, "svm tokenizer: ", e);
        }
    }

    private void loadTokenizer(InputStream inputStream) throws JSONException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        JSONObject tokenizerJson = new JSONObject(jsonString);
        // load vocab in that same json
        JSONObject vocabJson = tokenizerJson.getJSONObject("vectorizer_params").getJSONObject("vocabulary");
        for (Iterator<String> it = vocabJson.keys(); it.hasNext(); ) {
            String token = it.next();
            int id = vocabJson.getInt(token);
            // list id of corresponding token
            vocab.put(token, id);
            // list token of corresponding id
            idToToken.put(id, token);
        }
    }

    public TokenizedResult encode(String textA) {
        List<String> tokens = new ArrayList<>();
        // text tokenization
        if (textA != null) {
            tokens.addAll(tokenizeText(textA));
        }

        long[] inputIds = new long[maxLength];
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (vocab.containsKey(t)){
                Integer idx = vocab.get(t);
                if (idx != null && idx <= inputIds.length){
                    inputIds[idx] += 1;
                }
            }
        }
        return new TokenizedResult(inputIds);
    }

    private List<String> tokenizeText(String text) {
        List<String> tokens = new ArrayList<>();
        // split on whitespace and handle punctuation
        String[] words = WHITESPACE_PATTERN.split(text);
        for (String word : words) {
            if (word.isEmpty()) continue;
            String lw = word.toLowerCase();
            if (vocab.containsKey(lw)) {
                tokens.add(lw);
            }
        }

        return tokens;
    }
}
