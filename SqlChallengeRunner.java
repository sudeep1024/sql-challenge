package com.example.sql_challenge;

import com.example.sql_challenge.dto.FinalQueryRequest;
import com.example.sql_challenge.dto.GenerateWebhookRequest;
import com.example.sql_challenge.dto.GenerateWebhookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

@Component
public class SqlChallengeRunner implements CommandLineRunner {

    @Value("${student.regno}")
    private String regNo;

    @Value("${webhook.url}")
    private String webhookUrl;

    @Value("${jwt.token}")
    private String accessToken;

    @Value("classpath:solution.sql")
    private Resource solutionFile;

    private final RestTemplate http = new RestTemplate();

    @Override
    public void run(String... args) {

        // 2) Tell you which SQL problem to solve
        int lastTwo = lastTwoDigits(regNo);
        boolean isOdd = (lastTwo % 2) == 1;
        System.out.println("Your regNo last two digits = " + lastTwo +
                " => " + (isOdd ? "ODD (Question 1)" : "EVEN (Question 2)"));
        System.out.println("Open the question link from the instructions for your case, " +
                "craft the SQL, and place it in solution.sql (see below).\n");

        // 3) Load your final SQL query from a file
        String finalQuery = "SELECT 1"; // default placeholder
        try {
            if (solutionFile != null && solutionFile.exists()) {
                finalQuery = StreamUtils.copyToString(
                        solutionFile.getInputStream(), StandardCharsets.UTF_8
                ).trim();
            } else {
                System.out.println("No solution file found. Using placeholder SQL.\n");
            }
        } catch (Exception e) {
            System.err.println("Could not read solution file: " + e.getMessage());
        }

        if (finalQuery.isBlank()) {
            System.err.println("Final SQL is blank. Aborting submit.");
            return;
        }

        // 4) Submit the solution with JWT in Authorization header
        try {
            var submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            submitHeaders.setBearerAuth(accessToken); // Authorization: Bearer <token>

            var submitEntity = new HttpEntity<>(new FinalQueryRequest(finalQuery), submitHeaders);
            var submitResp = http.exchange(webhookUrl, HttpMethod.POST, submitEntity, String.class);

            System.out.println("Submission status: " + submitResp.getStatusCode());
            System.out.println("Response body : " + submitResp.getBody());
            System.out.println("\nAll done.\n");
        } catch (HttpClientErrorException e) {
            System.err.println("Submit failed: " + e.getStatusCode() + " => " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Submit failed: " + e.getMessage());
        }
    }

    private int lastTwoDigits(String regNo) {
        // Extract trailing digits; fallback to 0
        String digits = regNo.replaceAll(".*?(\\d{2})$", "$1");
        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }
}

