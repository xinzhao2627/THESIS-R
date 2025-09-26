package com.example.explicitapp3.ToolsNLP;

import android.util.Log;

import com.example.explicitapp3.Types.ModelTypes;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.util.*;

public class Distilbert_tagalog_tokenizer {
    private Map<String, Integer> vocab;
    private Map<Integer, String> idToToken;
    private Set<String> specialTokens;
    private final int maxLength = ModelTypes.DISTILBERT_TAGALOG_SEQ_LEN;
    private static final String TAG = "DistilBert Tokenizer";

    public static class TokenizedResult {
        public long[] inputIds;
        public long[] attentionMask;
        public long[] tokenTypeIds;

        public TokenizedResult(long[] inputIds, long[] attentionMask, long[] tokenTypeIds) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
            this.tokenTypeIds = tokenTypeIds;
        }
    }

    public Distilbert_tagalog_tokenizer(InputStream tokenizerJsonStream) {
        this.vocab = new HashMap<>();
        this.idToToken = new HashMap<>();
        this.specialTokens = new HashSet<>();
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

        // Load vocabulary
        JSONObject vocabJson = tokenizerJson.getJSONObject("model").getJSONObject("vocab");
        for (Iterator<String> it = vocabJson.keys(); it.hasNext(); ) {
            String token = it.next();
            int id = vocabJson.getInt(token);
            // list id of corresponding token
            vocab.put(token, id);
            // list token of corresponding id
            idToToken.put(id, token);
        }

        // Load special tokens
        if (tokenizerJson.has("added_tokens")) {
            JSONArray addedTokensArray = tokenizerJson.getJSONArray("added_tokens");
            for (int i = 0; i < addedTokensArray.length(); i++) {
                JSONObject tokenInfo = addedTokensArray.getJSONObject(i);
                if (tokenInfo.has("content")) {
                    // e.g <s>, <p>, <unk>
                    specialTokens.add(tokenInfo.getString("content"));
                }
            }
        }
        Log.i(TAG, "unk is at: " + vocab.get("[UNK]"));
    }

    public TokenizedResult encode(String text) {
        return encode(text, null); // Single sentence
    }

    public TokenizedResult encode(String textA, String textB) {
        List<String> tokens = new ArrayList<>();
        // [CLS]
        tokens.add("[CLS]");

        // text tokenization
        if (textA != null) {
            tokens.addAll(tokenizeText(textA));
        }

        // [SEP] between sentences
        tokens.add("[SEP]");

        // second text/sentence
        if (textB != null) {
            tokens.addAll(tokenizeText(textB));
            tokens.add("[SEP]");
        }

        //                maxLength = seq_len = 1
        long[] inputIds = new long[maxLength];
        long[] attentionMask = new long[inputIds.length];
        long[] tokenTypeIds = new long[inputIds.length];
//        Log.i(TAG, "vocab length: " + getVocabSize());
//        Log.i(TAG, "token length: " + tokens.size());
        for (int i = 0; i < inputIds.length; i++) {
            if (i < tokens.size()) {
                String token = tokens.get(i);
//                Log.i(TAG, "token is: " + token + " id is: " + vocab.getOrDefault(token, vocab.get("<unk>")));
                inputIds[i] = vocab.getOrDefault(token, vocab.get("[UNK]"));
                attentionMask[i] = 1;
            } else {
                // Padding
                inputIds[i] = vocab.get("[PAD]");
                attentionMask[i] = 0;
            }
            tokenTypeIds[i] = 0;
        }
//        Log.i(TAG, "input id: " + Arrays.toString(inputIds));
        return new TokenizedResult(inputIds, attentionMask, tokenTypeIds);
    }

    private List<String> tokenizeText(String text) {
        List<String> tokens = new ArrayList<>();
        // split on whitespace and handle punctuation
        String[] words = text.split("\\s+");
//        Log.i(TAG, "splitted word: " + Arrays.toString(words));
        for (String word : words) {

            if (vocab.containsKey(word)){
                tokens.add(word.toLowerCase());
            } else if (vocab.containsKey(word.toLowerCase())){
                tokens.add(word.toLowerCase());
            }
            else {
                // find all subwords
                tokens.addAll(findSubWords(word));
            }
        }

        return tokens;
    }
    private List<String> findSubWords(String word) {
        List<String> tokens = new ArrayList<>();
        int start = 0;

        while (start < word.length()) {
            String longestMatch = null;
            int longestEnd = start;

            // Try all possible substrings starting from 'start'
            for (int end = word.length(); end > start; end--) {
                String candidate = word.substring(start, end);

                // Add ## prefix if this isn't the start of the word
                if (start > 0) {
                    candidate = "##" + candidate;
                }

                if (vocab.containsKey(candidate)) {
                    longestMatch = candidate;
                    longestEnd = end;
                    break;
                } else if (vocab.containsKey(candidate.toLowerCase())){
                    longestMatch = candidate;
                    longestEnd = end;
                    break;
                }
            }

            if (longestMatch != null) {
                tokens.add(longestMatch);
                start = longestEnd;
            } else {
                // No match found - use [UNK] and move forward
                tokens.add("[UNK]");
                start++;
            }
        }

        return tokens;
    }

    public int getVocabSize() {
        return vocab.size();
    }
}
