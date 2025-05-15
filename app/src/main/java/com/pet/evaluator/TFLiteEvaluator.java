package com.pet.evaluator;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * A TensorFlow Lite wrapper for on-device inference
 */
public class TFLiteEvaluator {
    private static final String TAG = "TFLiteEvaluator";

    // TFLite Interpreter used for running the model
    private Interpreter tflite;
    private boolean isModelLoaded = false;

    // Model configuration
    private String modelName;
    private int[] inputShape;
    private int[] outputShape;
    private String[] inputNames;
    private String[] outputNames;
    private static JSONObject modelConfig = null;

    /**
     * Constructor that loads the TensorFlow Lite model
     *
     * @param context   Android context for accessing assets
     * @param modelPath Path to the model file in assets
     */
    public TFLiteEvaluator(Context context, String modelPath) {
        try {
            // Extract model name from path (with null check)
            if (modelPath == null) {
                throw new IllegalArgumentException("Model path cannot be null");
            }

            String modelFullName = modelPath;
            if (modelPath.contains("/")) {
                modelFullName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
            }
            modelName = modelFullName.replace(".tflite", "");

            // Load model config if not already loaded
            if (modelConfig == null) {
                loadModelConfig(context);
            }

            // Load model metadata from config
            loadModelMetadata();

            // Load the actual model
            tflite = new Interpreter(loadModelFile(context, modelPath));
            isModelLoaded = true;
            Log.i(TAG, "TFLite model " + modelPath + " loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model: " + e.getMessage(), e);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing model metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Load model configuration from JSON file
     *
     * @param context Android context for accessing assets
     * @throws IOException   If config file cannot be read
     * @throws JSONException If config file has invalid JSON
     */
    private void loadModelConfig(Context context) throws IOException, JSONException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("model_config.json")))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            modelConfig = new JSONObject(json.toString());
            Log.i(TAG, "Model configuration loaded successfully");
        }
    }

    /**
     * Load model-specific metadata from the config
     *
     * @throws JSONException If metadata for the model is not found
     */
    private void loadModelMetadata() throws JSONException {
        if (modelConfig == null || !modelConfig.has("models")) {
            throw new JSONException("Model configuration is missing or invalid");
        }

        JSONObject models = modelConfig.getJSONObject("models");
        if (!models.has(modelName)) {
            throw new JSONException("Metadata for model " + modelName + " not found");
        }

        JSONObject metadata = models.getJSONObject(modelName);

        try {
            // Parse input and output shapes with error handling
            int[] inputs = metadata.has("input_shape") ?
                    parseJsonIntArray(metadata.getJSONArray("input_shape")) : new int[]{1};
            int[] outputs = metadata.has("output_shape") ?
                    parseJsonIntArray(metadata.getJSONArray("output_shape")) : new int[]{1};

            inputShape = inputs;
            outputShape = outputs;

            // Parse input and output names with error handling
            String[] inNames = metadata.has("input_names") ?
                    parseJsonStringArray(metadata.getJSONArray("input_names")) : new String[]{"input"};
            String[] outNames = metadata.has("output_names") ?
                    parseJsonStringArray(metadata.getJSONArray("output_names")) : new String[]{"output"};

            inputNames = inNames;
            outputNames = outNames;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing model metadata: " + e.getMessage());
            // Set default values
            inputShape = new int[]{1};
            outputShape = new int[]{1};
            inputNames = new String[]{"input"};
            outputNames = new String[]{"output"};
        }

        Log.i(TAG, "Model " + modelName + " metadata loaded: " +
                "input shape: " + inputShape[0] + ", output shape: " + outputShape[0]);
    }

    /**
     * Helper method to parse JSON int arrays
     */
    private int[] parseJsonIntArray(org.json.JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) {
            return new int[0]; // Return empty array if input is null
        }

        int[] result = new int[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            result[i] = jsonArray.getInt(i);
        }
        return result;
    }

    /**
     * Helper method to parse JSON string arrays
     */
    private String[] parseJsonStringArray(org.json.JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) {
            return new String[0]; // Return empty array if input is null
        }

        String[] result = new String[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            String value = jsonArray.optString(i, ""); // Use optString to avoid null
            result[i] = value;
        }
        return result;
    }

    /**
     * Load a TensorFlow Lite model file
     *
     * @param context   Android context for accessing assets
     * @param modelPath Path to the model file in assets
     * @return MappedByteBuffer containing the model
     * @throws IOException If model file cannot be loaded
     */
    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {

            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    /**
     * Predict score for a candidate based on resume features
     *
     * @param resumeFeatures Array of resume features
     * @return Predicted score
     */
    public float predictResumeScore(float[] resumeFeatures) {
        if (!isModelLoaded) {
            Log.e(TAG, "TFLite model not loaded");
            return 0.0f;
        }

        // Validate input shape if we have metadata
        if (inputShape != null && resumeFeatures.length != inputShape[0]) {
            Log.w(TAG, "Input features length " + resumeFeatures.length +
                    " doesn't match expected input shape " + inputShape[0]);
        }

        // Output tensor will contain the predicted score
        float[][] output = new float[1][1];

        try {
            // Run inference
            tflite.run(new float[][]{resumeFeatures}, output);
            Log.d(TAG, "Resume score prediction successful: " + output[0][0]);
        } catch (Exception e) {
            Log.e(TAG, "Error during inference: " + e.getMessage(), e);
            return 0.0f;
        }

        return output[0][0];
    }

    /**
     * Predict English fluency and vocabulary scores from video features
     *
     * @param videoFeatures Array of video features
     * @return Map containing fluency and vocabulary scores
     */
    public Map<String, Float> predictVideoScores(float[] videoFeatures) {
        if (!isModelLoaded) {
            Log.e(TAG, "TFLite model not loaded");
            return new HashMap<>();
        }

        // Validate input shape if we have metadata
        if (inputShape != null && videoFeatures.length != inputShape[0]) {
            Log.w(TAG, "Input features length " + videoFeatures.length +
                    " doesn't match expected input shape " + inputShape[0]);
        }

        // Output tensors will contain fluency and vocabulary scores
        float[][] outputFluency = new float[1][1];
        float[][] outputVocabulary = new float[1][1];

        // Run inference
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, outputFluency);
        outputs.put(1, outputVocabulary);

        // We'd normally use runForMultipleInputsOutputs
        // But for this demo, we'll just simulate with two separate runs
        tflite.run(new float[][]{videoFeatures}, outputFluency);
        tflite.run(new float[][]{videoFeatures}, outputVocabulary);

        Map<String, Float> results = new HashMap<>();
        results.put("fluency", outputFluency[0][0]);
        results.put("vocabulary", outputVocabulary[0][0]);

        return results;
    }

    /**
     * Predict overall score for a candidate
     *
     * @param resumeScore     Resume score
     * @param cgpa            Normalized CGPA (0-1)
     * @param fluencyScore    English fluency score
     * @param vocabularyScore Vocabulary score
     * @return Predicted overall score
     */
    public float predictOverallScore(float resumeScore, float cgpa, float fluencyScore, float vocabularyScore) {
        if (!isModelLoaded) {
            Log.e(TAG, "TFLite model not loaded");
            return 0.0f;
        }

        // Combine features into a single array
        float[] features = {resumeScore, cgpa, fluencyScore, vocabularyScore};

        // Output tensor will contain the predicted score
        float[][] output = new float[1][1];

        // Run inference
        tflite.run(new float[][]{features}, output);

        return output[0][0];
    }

    /**
     * Close the interpreter when done
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            isModelLoaded = false;
        }
    }
}
