package com.digitallife.journal_site.ChatGptIntegration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatGPTController {

    private final String NEW_OPENAI_API_KEY = "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";
    private static final String FINE_TUNED_MODEL_ID = "ft:gpt-4o-mini-2024-07-18:personal:stories:AIydAQCN";

    /**
     * gets feedback about the journal entry text from a fine-tuned model of chat gpt 4o mini. This model was trained on our own data.
     *
     * @param journalEntry The journal Entry that needs to be assessed.
     * @return A string which contains the response of the fine-tuned model
     * @throws JSONException when JSON rules are not adhered when creating a JSONArray
     */
    @PostMapping("/get-feedback")
    public ResponseEntity<String> getFeedback(@RequestBody String journalEntry) throws JSONException {
        String url = "https://api.openai.com/v1/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        // Constructing the message list with 'user' input
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "You are an assistant that gives constructive feedback on journal entries."));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "Give feedback on the following journal entry:\n\n" + journalEntry));

        // Building the request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", FINE_TUNED_MODEL_ID);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 150);
        requestBody.put("temperature", 0.5);  // adjust creativity, closer to 1 means answers will be more creative

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + NEW_OPENAI_API_KEY);
        headers.set("Content-Type", "application/json");


        // Create HTTP request
        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

        // Make the API call and return the response
        return restTemplate.postForEntity(url, request, String.class);
    }

    /**
     * generate an image using dalle 3 api. The image is based on the journal entry and the chosen style.
     * TODO instead of storing the image as a base64 store it on a cloud like google cloud (saves data of database)
     *
     * @param request is a JSON file with the journalText and the type of style chosen by the user
     * @return the image in base64 form to store in a database
     * @throws JSONException when JSON rules are not adhered
     */
    @PostMapping("/generate-image")
    public ResponseEntity<String> generateImage(@RequestBody Map<String, String> request) throws JSONException {
        String journalText = request.get("journalText");
        String style = request.get("style");

        //create a prompt to dall e 3 api
        String prompt = "Create a " + style + " image of one of the positive moments of the following journal:\n\n" + journalText;


        String url = "https://api.openai.com/v1/images/generations";
        RestTemplate restTemplate = new RestTemplate();

        // Prepare the request body for DALL·E
        JSONObject body = new JSONObject();
        body.put("model", "dall-e-3"); // Specify DALL·E 3 model
        body.put("prompt", prompt);
        body.put("n", 1); // Generate 1 image
        body.put("size", "1024x1024"); // Image size
        body.put("quality", "standard");

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + NEW_OPENAI_API_KEY);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // Parse the image URL from the response
        String imageUrl = ""; // Extract the image URL from the response
        if (response.getStatusCode().is2xxSuccessful()) {
            JSONObject jsonResponse = new JSONObject(response.getBody());
            imageUrl = jsonResponse.getJSONArray("data").getJSONObject(0).getString("url");
        } else {
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        }

        try {
            // Fetch the image from the URL
            URL urlObj = new URL(imageUrl);
            InputStream inputStream = urlObj.openStream();
            byte[] imageBytes = inputStream.readAllBytes(); // Read image into a byte array

            // Convert byte array to Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Return the Base64 image as a JSON response
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body("{\"base64Image\":\"" + base64Image + "\"}");

        } catch (Exception e) {
            // Handle any errors that occur while fetching or processing the image
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to fetch and encode image: " + e.getMessage() + "\"}");
        }
    }


}