package com.example.explicitapp3.ToolsNLP;

import android.util.Log;

import com.example.explicitapp3.Types.ModelTypes;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class LSTM_tokenizer {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final String UNK_TOKEN = "<OOV>";


    private Map<String, Integer> vocab;
    private Map<Integer, String> idToToken;
    private final int maxLength = ModelTypes.LSTM_SEQ_LEN;
    private static final String TAG = "LSTM Tokenizer";

    public static class TokenizedResult {
        public long[] inputIds;
        public long[] attentionMask;
        public TokenizedResult(long[] inputIds, long[] attentionMask) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
        }
    }

    public LSTM_tokenizer(InputStream tokenizerJsonStream) {
        this.vocab = new HashMap<>();
        this.idToToken = new HashMap<>();
        try {
            loadTokenizer(tokenizerJsonStream);
        } catch (JSONException e) {
            Log.e(TAG, "distil_tagalog_tokenizer: ", e);
        }
    }

    private void loadTokenizer(InputStream inputStream) throws JSONException {
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String jsonString = scanner.hasNext() ? scanner.next() : "";
        scanner.close();

        JSONObject tokenizerJson = new JSONObject(jsonString);
        String wordIndexStr = tokenizerJson.getJSONObject("config").getString("word_index");
        // load vocab in that same json
        JSONObject vocabJson = new JSONObject(wordIndexStr);
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
        long[] attentionMask = new long[inputIds.length];
//        Log.i(TAG, "vocab length: " + getVocabSize());
//        Log.i(TAG, "token length: " + tokens.size());
        int tokenSize=  Math.min(tokens.size(), maxLength);
        for (int i = 0; i < inputIds.length; i++) {
            if (i < tokenSize) {
                String token = tokens.get(i);
//                Log.i(TAG, "token is: " + token + " id is: " + vocab.getOrDefault(token, vocab.get("<unk>")));
                inputIds[i] = vocab.getOrDefault(token, vocab.get(UNK_TOKEN));
                attentionMask[i] = 1;
            } else {
                // Padding
                inputIds[i] = 0;
                attentionMask[i] = 0;
            }
        }
//        Log.i(TAG, "input id: " + Arrays.toString(inputIds));
        return new TokenizedResult(inputIds, attentionMask);
    }

    private List<String> tokenizeText(String text) {
        List<String> tokens = new ArrayList<>();
        // split on whitespace and handle punctuation
        String[] words = WHITESPACE_PATTERN.split(text);
//        Log.i(TAG, "splitted word: " + Arrays.toString(words));
        for (String word : words) {
            if (word.isEmpty()) continue;
            String lw = word.toLowerCase();
            if (vocab.containsKey(lw)){
                tokens.add(lw);
            }
        }

        return tokens;
    }
    public int getVocabSize() {
        return vocab.size();
    }
}
