package com.example.explicitapp3.ToolsNLP;

import android.util.Log;

import com.example.explicitapp3.Types.ModelTypes;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class Distilbert_tagalog_tokenizer {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final String UNK_TOKEN = "[UNK]";
    private static final String PAD_TOKEN = "[PAD]";
    private static final String CLS_TOKEN = "[CLS]";
    private static final String SEP_TOKEN = "[SEP]";
    private int unkTokenId;
    private int padTokenId;
    private int clsTokenId;
    private int sepTokenId;


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

        // load vocab in that same json
        JSONObject vocabJson = tokenizerJson.getJSONObject("model").getJSONObject("vocab");
        for (Iterator<String> it = vocabJson.keys(); it.hasNext(); ) {
            String token = it.next();
            int id = vocabJson.getInt(token);
            // list id of corresponding token
            vocab.put(token, id);
            // list token of corresponding id
            idToToken.put(id, token);
        }
        this.unkTokenId = vocab.get(UNK_TOKEN);
        this.padTokenId = vocab.get(PAD_TOKEN);
        this.clsTokenId = vocab.get(CLS_TOKEN);
        this.sepTokenId = vocab.get(SEP_TOKEN);

        // load special tokens like sep, unk, pad, etc..
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
        // [CLS] for distil
        tokens.add(CLS_TOKEN);

        // text tokenization
        if (textA != null) {
            tokens.addAll(tokenizeText(textA));
        }

        // [SEP] between sentences separator
        tokens.add(SEP_TOKEN);

        // second text/sentence
        if (textB != null) {
            tokens.addAll(tokenizeText(textB));
            tokens.add("[SEP]");
        }

        long[] inputIds = new long[maxLength];
        long[] attentionMask = new long[inputIds.length];
        long[] tokenTypeIds = new long[inputIds.length];
//        Log.i(TAG, "vocab length: " + getVocabSize());
//        Log.i(TAG, "token length: " + tokens.size());
        int tokenSize=  Math.min(tokens.size(), maxLength);
        for (int i = 0; i < inputIds.length; i++) {
            if (i < tokenSize) {
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
        String[] words = WHITESPACE_PATTERN.split(text);
//        Log.i(TAG, "splitted word: " + Arrays.toString(words));
        for (String word : words) {
            if (word.isEmpty()) continue;
            String lw = word.toLowerCase();
            if (vocab.containsKey(lw)){
                tokens.add(lw);
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
                // [UNK] if theres nothing
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
