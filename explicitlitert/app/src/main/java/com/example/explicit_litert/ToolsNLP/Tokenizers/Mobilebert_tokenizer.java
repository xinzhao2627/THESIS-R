package com.example.explicit_litert.ToolsNLP.Tokenizers;

import android.util.Log;

import com.example.explicit_litert.Types.ModelTypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

public class Mobilebert_tokenizer {
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final String UNK_TOKEN = "[UNK]";
    private static final String PAD_TOKEN = "[PAD]";
    private static final String CLS_TOKEN = "[CLS]";
    private static final String SEP_TOKEN = "[SEP]";
    private static final String MASK_TOKEN = "[MASK]";
    private int unkTokenId;
    private int padTokenId;
    private int clsTokenId;
    private int sepTokenId;
    private int maskTokenId;


    private Map<String, Integer> vocab;
    private Map<Integer, String> idToToken;
    private final int maxLength = ModelTypes.MOBILEBERT_SEQ_LEN;
    private static final String TAG = "Mobilebert Tokenizer";

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

    public Mobilebert_tokenizer(InputStream tokenizerJsonStream) {
        this.vocab = new HashMap<>();
        this.idToToken = new HashMap<>();
        try {
            loadTokenizer(tokenizerJsonStream);
        } catch (IOException e) {
            Log.e(TAG, "mobilebert_tokenizer: ", e);
        }
    }

    private void loadTokenizer(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        int id = 0;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            vocab.put(line, id);
            idToToken.put(id, line);
            id++;
        }

        reader.close();

        // required special tokens
        this.unkTokenId = vocab.get(UNK_TOKEN);
        this.padTokenId = vocab.get(PAD_TOKEN);
        this.clsTokenId = vocab.get(CLS_TOKEN);
        this.sepTokenId = vocab.get(SEP_TOKEN);
        this.maskTokenId= vocab.get(MASK_TOKEN);
    }

    public Mobilebert_tokenizer.TokenizedResult encode(String text) {
        return encode(text, null); // Single sentence
    }

    public Mobilebert_tokenizer.TokenizedResult encode(String textA, String textB) {
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
        Log.i(TAG, "behold tokens: " + tokens);
//        Log.i(TAG, "input id: " + Arrays.toString(inputIds));
        return new Mobilebert_tokenizer.TokenizedResult(inputIds, attentionMask, tokenTypeIds);
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
//                DONT ADD SUBWORDS THIS WILL RUIN THE MODEL INPUT, PRODUCING INCONSISTENT RESULT

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

            // try all possible substrings starting from 'start'
            for (int end = word.length(); end > start; end--) {
                String candidate = word.substring(start, end);

                // add ## prefix if this isn't the start of the word
                if (start > 0) {
                    candidate = "##" + candidate;
                }

                if (vocab.containsKey(candidate)) {
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
