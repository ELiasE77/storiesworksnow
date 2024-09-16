package com.digitallife.journal_site;

import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@Service
public class JournalService {

    public String processEntry(String journalEntry) {
        try {
            // Call the OpenAI API
            URL url = new URL("https://api.openai.com/v1/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer YOUR_API_KEY");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // JSON body
            String inputJson = "{\"model\": \"text-davinci-003\", \"prompt\": \"" + journalEntry + "\", \"max_tokens\": 100}";

            conn.getOutputStream().write(inputJson.getBytes());
            conn.getOutputStream().flush();

            // Read the response
            Scanner sc = new Scanner(conn.getInputStream());
            StringBuilder jsonResponse = new StringBuilder();
            while (sc.hasNext()) {
                jsonResponse.append(sc.nextLine());
            }
            sc.close();

            // Handle the response (can use Gson or Jackson for parsing JSON)
            // Simplified response handling here for the sake of brevity
            return "AI response generated for: " + journalEntry;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing journal entry.";
        }
    }
}

