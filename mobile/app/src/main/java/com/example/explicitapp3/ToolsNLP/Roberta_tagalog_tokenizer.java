package com.example.explicitapp3.ToolsNLP;

import android.util.Log;

import com.example.explicitapp3.Types.ModelTypes;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.util.*;

public class Roberta_tagalog_tokenizer {
    private Map<String, Integer> vocab;
    private Map<Integer, String> idToToken;
    private Set<String> specialTokens;
    private final int maxLength = ModelTypes.DISTILBERT_TAGALOG_SEQ_LEN;
    private static final String TAG = "Roberta Tokenizer";

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

    public Roberta_tagalog_tokenizer(InputStream tokenizerJsonStream) {
        this.vocab = new HashMap<>();
        this.idToToken = new HashMap<>();
        this.specialTokens = new HashSet<>();
        try {
            loadTokenizer(tokenizerJsonStream);
        } catch (JSONException e) {
            Log.e(TAG, "Roberta_tagalog_tokenizer: ", e);
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
    }

    public TokenizedResult encode(String text) {
        return encode(text, null); // Single sentence
    }

    public TokenizedResult encode(String textA, String textB) {
        List<String> tokens = new ArrayList<>();
        // <s> starting segment
        tokens.add("<s>");

        // text tokenization
        if (textA != null) {
            tokens.addAll(tokenizeText(textA));
        }

        // </s> segment between sentences
        tokens.add("</s>");

        // second text/sentence
        if (textB != null) {
            tokens.addAll(tokenizeText(textB));
            tokens.add("</s>");
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

                inputIds[i] = vocab.getOrDefault(token, vocab.get("<unk>"));
                attentionMask[i] = 1;
            } else {
                // if empty space or tokens ended, add pad
                inputIds[i] = vocab.get("<pad>");
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
            // add json space
            String cleanWord = "Ġ" + word;
            if (vocab.containsKey(cleanWord)) {
                tokens.add(cleanWord);
            }

            else if (vocab.containsKey(word.toLowerCase())){
                tokens.add(word.toLowerCase());
            }
            else {
                tokens.add("<unk>");
            }
//            else {
//                // split the word into subwords, noo
//                tokens.addAll(splitWord(word));
//            }
        }

        return tokens;
    }

    private List<String> splitWord(String word) {
        List<String> subwords = new ArrayList<>();
        int start = 0;
        for (int i = word.length(); i > 0; i--) {
            String substring = word.substring(start, i);
            if (vocab.containsKey(substring) || i == start + 1) {
                subwords.add(substring);
                start = i;
                i = word.length() + 1; // restart from end
            }
        }

        // If no subwords found, use UNK
        if (subwords.isEmpty()) {
            subwords.add("<unk>");
        }

        return subwords;
    }

    public int getVocabSize() {
        return vocab.size();
    }
}
