package com.example.explicit_litert.Detectors;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.explicit_litert.ToolsNLP.Recognizer;
import com.example.explicit_litert.ToolsNLP.SoftmaxConverter;
import com.example.explicit_litert.ToolsNLP.Tokenizers.Distilbert_tagalog_tokenizer;
import com.example.explicit_litert.Types.DetectionResult;
import com.example.explicit_litert.Types.ModelTypes;
import com.example.explicit_litert.Types.TextResults;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// IN THIS CODE YOU CAN RUN: TINYBERT/DISTILBERT JUST REPLACE THE MODEL AND TOKENIZER FILEPATH IN THE MODELTYPES.JAVA
public class DistilBERT_tagalog_Detector {
    String[] stopwordsArray = {
            "0o", "0s", "3a", "3b", "3d", "6b", "6o", "a", "a1", "a2", "a3", "a4", "ab", "able", "about", "above", "abst",
            "ac", "accordance", "according", "accordingly", "across", "act", "actually", "ad", "added", "adj", "ae", "af",
            "affected", "affecting", "affects", "after", "afterwards", "ag", "again", "against", "ah", "ain", "ain't",
            "aj", "al", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always",
            "am", "among", "amongst", "amoungst", "amount", "an", "and", "announce", "another", "any", "anybody",
            "anyhow", "anymore", "anyone", "anything", "anyway", "anyways", "anywhere", "ao", "ap", "apart",
            "apparently", "appear", "appreciate", "appropriate", "approximately", "ar", "are", "aren", "arent",
            "aren't", "arise", "around", "as", "a's", "aside", "ask", "asking", "associated", "at", "au", "auth",
            "av", "available", "aw", "away", "awfully", "ax", "ay", "az", "b", "b1", "b2", "b3", "ba", "back",
            "bc", "bd", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand",
            "begin", "beginning", "beginnings", "begins", "behind", "being", "believe", "below", "beside",
            "besides", "best", "better", "between", "beyond", "bi", "bill", "biol", "bj", "bk", "bl", "bn", "forget",
            "both", "bottom", "bp", "br", "brief", "briefly", "bs", "bt", "bu", "but", "bx", "by", "c", "c1", "c2", "c3", "ca",
            "call", "came", "can", "cannot", "cant", "can't", "cause", "causes", "cc", "cd", "ce", "certain", "certainly",
            "cf", "cg", "ch", "changes", "ci", "cit", "cj", "cl", "clearly", "cm", "c'mon", "cn", "co", "com", "come", "comes", "con",
            "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could",
            "couldn", "couldnt", "couldn't", "course", "cp", "cq", "cr", "cry", "cs", "c's", "ct", "cu", "currently", "cv", "cx", "cy", "cz", "d", "d2", "da",
            "date", "dc", "dd", "de", "definitely", "describe", "described", "despite", "detail", "df", "di", "did", "didn", "didn't", "different", "dj", "dk",
            "dl", "do", "does", "doesn", "doesn't", "doing", "don", "done", "don't", "down", "downwards", "dp", "dr", "ds", "dt", "du", "due", "during", "dx",
            "dy", "e", "e2", "e3", "ea", "each", "ec", "ed", "edu", "ee", "ef", "effect", "eg", "ei", "eight", "eighty", "either", "ej", "el", "eleven", "else",
            "elsewhere", "em", "empty", "en", "end", "ending", "enough", "entirely", "eo", "ep", "eq", "er", "es", "especially", "est", "et", "et-al", "etc", "eu",
            "ev", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "ey", "f", "f2", "fa", "far",
            "fc", "few", "ff", "fi", "fifteen", "fifth", "fify", "fill", "find", "fire", "first", "five", "fix", "fj", "fl", "fn", "fo", "followed", "following",
            "follows", "for", "former", "formerly", "forth", "forty", "found", "four", "fr", "from", "front", "fs", "ft", "fu", "full", "further", "furthermore",
            "fy", "g", "ga", "gave", "ge", "get", "gets", "getting", "gi", "give", "given", "gives", "giving", "gj", "gl", "go", "goes", "going", "gone", "got",
            "gotten", "gr", "greetings", "gs", "gy", "h", "h2", "h3", "had", "hadn", "hadn't", "happens", "hardly", "has", "hasn", "hasnt", "hasn't", "have", "haven",
            "haven't", "having", "he", "hed", "he'd", "he'll", "hello", "help", "hence", "her", "here", "hereafter", "hereby", "herein", "heres", "here's", "hereupon",
            "hers", "herself", "hes", "he's", "hh", "hi", "hid", "him", "himself", "his", "hither", "hj", "ho", "home", "hopefully", "how", "howbeit", "however", "how's",
            "hr", "hs", "http", "hu", "hundred", "hy", "i", "i2", "i3", "i4", "i6", "i7", "i8", "ia", "ib", "ibid", "ic", "id", "i'd", "ie", "if", "ig", "ignored", "ih",
            "ii", "ij", "il", "i'll", "im", "i'm", "immediate", "immediately", "importance", "important", "in", "inasmuch", "inc", "indeed", "index", "indicate", "indicated",
            "indicates", "information", "inner", "insofar", "instead", "interest", "uto", "auto", "into", "invention", "inward", "io", "ip", "iq", "ir", "is", "isn", "isn't", "it",
            "itd", "it'd", "it'll", "its", "it's", "itself", "iv", "i've", "ix", "iy", "iz", "j", "jj", "jr", "js", "jt", "ju", "just", "k", "ke", "keep", "keeps",
            "kept", "kg", "kj", "km", "know", "known", "knows", "ko", "l", "l2", "la", "largely", "last", "lately", "later", "latter", "latterly", "lb", "lc", "le",
            "least", "les", "less", "lest", "let", "lets", "let's", "lf", "like", "liked", "likely", "line", "little", "lj", "ll", "ll", "ln", "lo", "look", "looking",
            "looks", "los", "lr", "ls", "lt", "ltd", "m", "m2", "ma", "made", "mainly", "make", "makes", "many", "may", "maybe", "me", "mean", "means", "meantime",
            "meanwhile", "merely", "mg", "might", "mightn", "mightn't", "mill", "million", "mine", "miss", "ml", "mn", "mo", "more", "moreover", "most", "mostly",
            "move", "mr", "mrs", "ms", "mt", "mu", "much", "mug", "must", "mustn", "mustn't", "my", "myself", "n", "n2", "na", "name", "namely", "nay", "nc", "nd",
            "ne", "near", "nearly", "necessarily", "necessary", "need", "needn", "needn't", "needs", "neither", "never", "nevertheless", "new", "next", "ng", "ni",
            "nine", "ninety", "nj", "nl", "nn", "no", "nobody", "non", "none", "nonetheless", "noone", "nor", "normally", "nos", "not", "noted", "nothing", "novel",
            "now", "nowhere", "nr", "ns", "nt", "ny", "o", "oa", "ob", "obtain", "obtained", "obviously", "oc", "od", "of", "off", "often", "og", "oh", "oi", "oj",
            "ok", "okay", "ol", "old", "om", "omitted", "on", "once", "one", "ones", "only", "onto", "oo", "op", "oq", "or", "ord", "os", "ot", "other", "others",
            "otherwise", "ou", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "ow", "owing", "own", "ox", "oz", "p", "p1", "p2", "p3",
            "page", "pagecount", "pages", "par", "part", "particular", "particularly", "pas", "past", "pc", "pd", "pe", "per", "perhaps", "pf", "ph", "pi", "pj",
            "pk", "pl", "placed", "please", "plus", "pm", "pn", "po", "poorly", "possible", "possibly", "potentially", "pp", "pq", "pr", "predominantly", "present",
            "presumably", "previously", "primarily", "probably", "promptly", "proud", "provides", "ps", "pt", "pu", "put", "py", "q", "qj", "qu", "que", "quickly",
            "quite", "qv", "r", "r2", "ra", "ran", "rather", "rc", "rd", "re", "readily", "really", "reasonably", "recent", "recently", "ref", "refs", "regarding",
            "regardless", "regards", "related", "relatively", "research", "research-articl", "respectively", "resulted", "resulting", "results", "rf", "rh", "ri",
            "right", "rj", "rl", "rm", "rn", "ro", "rq", "rr", "rs", "rt", "ru", "run", "rv", "ry", "s", "s2", "sa", "said", "same", "saw", "say", "saying", "says",
            "sc", "sd", "se", "sec", "second", "secondly", "section", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible",
            "sent", "serious", "seriously", "seven", "several", "sf", "shall", "shan", "shan't", "she", "shed", "she'd", "she'll", "shes", "she's", "should", "shouldn",
            "shouldn't", "should've", "show", "showed", "shown", "showns", "shows", "si", "side", "significant", "significantly", "similar", "similarly", "since",
            "sincere", "six", "sixty", "sj", "sl", "slightly", "sm", "sn", "so", "some", "somebody", "somehow", "someone", "somethan", "something", "sometime",
            "sometimes", "somewhat", "somewhere", "soon", "sorry", "sp", "specifically", "specified", "specify", "specifying", "sq", "sr", "ss", "st", "still",
            "stop", "strongly", "sub", "substantially", "successfully", "such", "sufficiently", "suggest", "sup", "sure", "sy", "system", "sz", "t", "t1", "t2",
            "t3", "take", "taken", "taking", "tb", "tc", "td", "te", "tell", "ten", "tends", "tf", "th", "than", "thank", "thanks", "thanx", "that", "that'll",
            "thats", "that's", "that've", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "thered", "therefore",
            "therein", "there'll", "thereof", "therere", "theres", "there's", "thereto", "thereupon", "there've", "these", "they", "theyd", "they'd", "they'll",
            "theyre", "they're", "they've", "thickv", "thin", "think", "third", "this", "thorough", "thoroughly", "those", "thou", "though", "thoughh", "thousand",
            "three", "throug", "through", "throughout", "thru", "thus", "ti", "til", "tip", "tj", "tl", "tm", "tn", "to", "together", "too", "took", "top", "toward",
            "towards", "tp", "tq", "tr", "tried", "tries", "truly", "try", "trying", "ts", "t's", "tt", "tv", "twelve", "twenty", "twice", "two", "tx", "u", "u201d",
            "ue", "ui", "uj", "uk", "um", "un", "under", "unfortunately", "unless", "unlike", "unlikely", "until", "unto", "uo", "up", "upon", "ups", "ur", "us", "use",
            "used", "useful", "usefully", "usefulness", "uses", "using", "usually", "ut", "v", "va", "value", "various", "vd", "ve", "ve", "very", "via", "viz", "vj",
            "vo", "vol", "vols", "volumtype", "vq", "vs", "vt", "vu", "w", "wa", "want", "wants", "was", "wasn", "wasnt", "wasn't", "way", "we", "wed", "we'd", "welcome",
            "well", "we'll", "well-b", "went", "were", "we're", "weren", "werent", "weren't", "we've", "what", "whatever", "what'll", "whats", "what's", "when", "whence",
            "whenever", "when's", "where", "whereafter", "whereas", "whereby", "wherein", "wheres", "where's", "whereupon", "wherever", "whether", "which", "while", "drama", "enter", "deposit",
            "whim", "whither", "who", "whod", "whoever", "whole", "who'll", "whom", "whomever", "whos", "who's", "whose", "why", "why's", "wi", "widely", "will", "convinced",
            "willing", "wish", "with", "within", "without", "wo", "won", "wonder", "wont", "won't", "words", "deleted", "removed", "world", "would", "wouldn", "wouldnt", "wouldn't", "www",
            "x", "x1", "x2", "x3", "xf", "xi", "xj", "xk", "xl", "xn", "xo", "xs", "xt", "xv", "xx", "y", "y2", "yes", "yet", "yj", "yl", "you", "youd", "you'd", "you'll", "like", "good",
            "your", "youre", "you're", "yours", "yourself", "yourselves", "you've", "yr", "ys", "yt", "z", "zero", "zi", "zz", "point", "points", "music", "cool", "met",
            "opo", "po", "tas", "akin", "kita", "nag", "palang", "niyo", "aking", "ako", "alin", "am", "amin", "aming", "ang", "ano", "anumang", "apat", "at", "atin", "ating", "ay", "bababa", "bago", "bakit", "bawat", "bilang", "dahil", "dalawa", "dapat",
            "din", "dito", "doon", "gagawin", "gayunman", "ginagawa", "ginawa", "ginawang", "gumawa", "gusto", "habang", "hanggang", "hindi", "huwag", "iba", "ibaba", "ibabaw", "ibig",
            "ikaw", "ilagay", "ilalim", "ilan", "inyong", "isa", "isang", "itaas", "ito", "iyo", "iyon", "iyong", "ka", "kahit", "kailangan", "kailanman", "kami", "reddit", "kanila", "kanilang",
            "kanino", "kanya", "kanyang", "kapag", "kapwa", "karamihan", "katiyakan", "katulad", "kaya", "kaysa", "ko", "kong", "kulang", "kumuha", "kung", "laban", "lahat", "lamang",
            "likod", "lima", "maaari", "maaaring", "maging", "mahusay", "makita", "marami", "marapat", "masyado", "may", "mayroon", "mga", "minsan", "mismo", "mula", "muli", "na",
            "nabanggit", "naging", "nagkaroon", "nais", "nakita", "namin", "napaka", "narito", "nasaan", "ng", "ngayon", "ni", "nila", "nilang", "nito", "niya", "niyang", "noon",
            "o", "pa", "paano", "pababa", "paggawa", "pagitan", "pagkakaroon", "pagkatapos", "palabas", "pamamagitan", "panahon", "pangalawa", "para", "paraan", "pareho",
            "pataas", "pero", "pumunta", "pumupunta", "sa", "saan", "sabi", "sabihin", "sarili", "sila", "sino", "siya", "tatlo", "tayo", "tulad", "tungkol", "una", "walang", "naman",
            "wow", "lol", "haha", "beautiful", "right", "ganda", "time", "best", "wanna", "day", "guy", "really", "sana", "great", "amazing", "little", "ill", "lucky", "god",
            "mag", "pag", "real", "ganyan", "feel", "feels", "omg", "public", "watch", "ung", "new", "better", "message", "absolutely", "actually", "try", "hair", "sobrang", "view", "sub", "work", "sakit", "nung", "helps", "routine",
            "sir", "ase"
    };
    Set<String> stopwords;
    private static final String TAG = "DISTILBERT_TAGALOG";
    private Context mcontext;
    Interpreter interpreter;
    Recognizer recognizer;
    Distilbert_tagalog_tokenizer tokenizer;
    SoftmaxConverter softmaxConverter;
    public static String[] LABELS = ModelTypes.DISTILBERT_TAGALOG_LABELARRAY;
    public static String tokenizerPath = ModelTypes.DISTILBERT_TAGALOG_TOKENIZER;
    public static String modelPath = ModelTypes.DISTILBERT_TAGALOG_MODEL;
    public static int SEQ_LEN = ModelTypes.DISTILBERT_TAGALOG_SEQ_LEN;
    int[][] ids;
    int[][] mask;
    float[][] outputs;

    public DistilBERT_tagalog_Detector(Context context, int etn, String modelName) {
        mcontext = context;

        try {
            stopwords = new HashSet<>(Arrays.asList(stopwordsArray));
            if (modelName.equals(ModelTypes.TINYBERT)){
                Log.i(TAG, "DistilBERT_tagalog_Detector: modelname is" + modelName);
                LABELS = ModelTypes.TINYBERT_LABELARRAY;
                modelPath = ModelTypes.TINYBERT_MODEL;
                SEQ_LEN = ModelTypes.TINYBERT_SEQ_LEN;
                tokenizerPath = ModelTypes.TINYBERT_TOKENIZER;
            }
            recognizer = new Recognizer(context, etn);
            softmaxConverter = new SoftmaxConverter();

            this.mcontext = context;
            ByteBuffer modelBuffer_base = loadModelFile(context, modelPath);
            Interpreter.Options options = new Interpreter.Options();

            Log.w(TAG, "GPU NOT SUPPORTED");
            Log.w(TAG, "available processors: " + Runtime.getRuntime().availableProcessors());
            options.setNumThreads(Runtime.getRuntime().availableProcessors());
            options.setUseXNNPACK(true);
//            }//
            interpreter = new Interpreter(modelBuffer_base, options);

            int inputCount = interpreter.getInputTensorCount();
            for (int i = 0; i < inputCount; i++) {
                int[] shape = interpreter.getInputTensor(i).shape();
                String name = interpreter.getInputTensor(i).name();
                Log.w(TAG, "Input " + i + " (" + name + ") shape: " + Arrays.toString(shape));
            }
            initBuffers();
            InputStream inputStream = mcontext.getAssets().open(tokenizerPath);
            tokenizer = new Distilbert_tagalog_tokenizer(inputStream, SEQ_LEN);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
    private static MappedByteBuffer loadModelFile(Context context, String assetPath)
            throws IOException {

        AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();

        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();

        return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public String cleanText(String t) {
        String text = t.replaceAll("\\b(kakantut(in)?|kantut(an)?|kinantot|ikakantot|kantot|inantot)\\b", " kantot ")
                .replaceAll("\\b(gagi|gagu|ginagago)\\b", " gago ")
                .replaceAll(
                        "\\b(f+u*c*k+|f\\*ck|fck|fu+ck+|fu+cck+|holy\\s*f+u*c*k+|fuck(ing|er|ers)?)\\b",
                        " fuck "
                )
                .replaceAll(
                        "\\b(c+u+m+|c+u+m+m+ing|c\\*m|cu+m+ing)\\b",
                        " cum "
                )
                .replaceAll(
                        "\\b(tamod|tamud|tamuran)\\b",
                        " cum "
                )
                .replaceAll(
                        "\\b(pussy|vagina|puke|pepe|pekpek|pek2|pek-pek|kepyas|bilat)\\b",
                        " pussy "
                )
                .replaceAll(
                        "\\b(dick|tite|titi|tete|burat|ari|oten|bayag)\\b",
                        " dick "
                )
                .replaceAll(
                        "\\b(gago|gaga|ogag|ungag|ulol|tarantado|tarantada|tanga|bobo|obob|inutil|engot|engeng|tangina|putangina|puta)\\b",
                        " tangina "
                )
                .replaceAll(
                        "\\b(sex|xxx|ass|tit|cum|fap)\\b",
                        " porn "
                )
                .replaceAll("\\b(pinag|ipag|mag|nag|pag|ipa|ma|maka|naka|pa|um|in)(?=[a-z])", "")
                .replaceAll("\\.(com|org|net|io|ph|edu|gov|app|vip)\\b", " ")
                .replaceAll("[^a-z\\s]", " ")
                .replaceAll("\\b[a-z]{1,2}\\b", " ")
                .replaceAll("\\s+", " ")
                .toLowerCase()
                .trim();


        StringBuilder result = new StringBuilder();
        for (String word : text.toLowerCase().split("\\s+")) {
            if (!stopwords.contains(word)) {
                result.append(word).append(" ");
            }
        }
        return result.toString().trim();
    }

    public List<DetectionResult> detect(Bitmap bitmap) {
        List<DetectionResult> detectionResultList = new ArrayList<>();
        long nnn = System.currentTimeMillis();
        List<TextResults> textResults = recognizer.textRecognition(bitmap);
        Log.i("recognizering", "recognizer_ms distiltiny: "+(System.currentTimeMillis()-nnn));
        long startTime = System.currentTimeMillis();
        for (TextResults t : textResults) {
            String text = cleanText(t.textContent);
            if (text.length() < 3) continue;
            Log.i("gpteam", "new distilbertortinybert");
            long now = System.currentTimeMillis();
            Distilbert_tagalog_tokenizer.TokenizedResult encoding = tokenizer.encode(text);
            Log.i("gpteam", "encode "+(System.currentTimeMillis()-now));

            long[] inputIds = encoding.inputIds;
            long[] attentionMask = encoding.attentionMask;
            if (inputIds.length < 1) continue;
//            ensureInputOrder(); // see below
            float[][] output = runInference(inputIds, attentionMask);
            now = System.currentTimeMillis();
            float[] probabilities = softmaxConverter.softmax(output[0]);
            Log.i("gpteam", "softmax "+(System.currentTimeMillis()-now));

            float max_cfs = -100f;
            String l = "";

            for (int i = 0; i < probabilities.length; i++) {
                if (probabilities[i] > max_cfs) {
//                    Log.i(TAG, LABELS[i] + "prob: " + probabilities[i]);
                    max_cfs = probabilities[i];
                    l = LABELS[i];
                }
            }
////            for (float[] o : output) Log.i(TAG, "output[]: " + Arrays.toString(o));
//            Log.i(TAG, "output length: " + output.length);
//            Log.i(TAG, "output array: " + Arrays.toString(output[0]));

//            Log.i(TAG, "\nSTART");
//
//            Log.i(TAG, "heence word: "+text + "  output[0][0]: "+output[0][0] + " | output[0][1]: "+ output[0][1] + " softmax[0]: " +probabilities[0] + " softmax[1]: "+ probabilities[1] + " label: " +l);
//            debugInput(text, inputIds, attentionMask);
//            int predClass = output[0][0] > output[0][1] ? 0 : 1;
//            if (predClass == 1) continue;
//            Log.i(TAG, "left: " + t.left + " top: " + t.top + " right: " + t.right + " bottom:" + t.bottom);


            if (l.equals("nsfw")) {
                detectionResultList.add(new DetectionResult(
                        0,
                        max_cfs,
                        t.left / bitmap.getWidth(),
                        t.top / bitmap.getHeight(),
                        t.right / bitmap.getWidth(),
                        t.bottom / bitmap.getHeight(),
                        "nsfw",
                        1
                ));
            }

            Log.i(TAG, "\n");
        }
//        long endTime = System.currentTimeMillis();
//        long duration = endTime - startTime;
//        Log.i(TAG, "this for loop took: " + duration + " ms");
        return detectionResultList;
    }

    public void initBuffers() {
        ids = new int[1][SEQ_LEN];
        mask = new int[1][SEQ_LEN];
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        outputs = new float[outputShape[0]][outputShape[1]];
    }

    public float[][] runInference(long[] inputIds, long[] attentionMask) {
        if (interpreter == null) return outputs;
        long now = System.currentTimeMillis();
        int seqLen = inputIds.length;
        for (int i = 0; i < seqLen; i++) {
            ids[0][i] = (int) inputIds[i];
            mask[0][i] = (int) attentionMask[i];
        }
        try {
            Object[] inputArray = new Object[]{mask, ids};
            Log.i("gpteam", "inputbuffer "+(System.currentTimeMillis()-now));

            now = System.currentTimeMillis();
            Map<Integer, Object> outputsMap = new HashMap<>();
            outputsMap.put(0, outputs);
            Log.i("gpteam", "outputbuffer "+(System.currentTimeMillis()-now));

            now = System.currentTimeMillis();
            interpreter.runForMultipleInputsOutputs(inputArray, outputsMap);
            Log.i("gpteam", "model.run "+(System.currentTimeMillis()-now));

        } catch (Exception e){
            Log.i(TAG, "runInference error: "+ e.getMessage());
        }

//        Log.i(TAG, "inference function took: " + duration + " ms");
        return outputs;
    }

    public void cleanup() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    private void debugInput(String raw, long[] ids, long[] mask) {
        int nonPad = 0;
        for (int i = 0; i < ids.length; i++) if (ids[i] != 0) nonPad++;
        Log.i(TAG, "RAW: '" + raw + "'");
        Log.i(TAG, "heence IDS: " + Arrays.toString(Arrays.copyOf(ids, 16)) + " ...");
        Log.i(TAG, "heence MASK: " + Arrays.toString(Arrays.copyOf(mask, 16)) + " ... nonPad=" + nonPad);
    }

}
