package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClient) {
        this.webClient = WebClient.builder().build();
    }


    public String generateEmailReply(EmailRequest emailRequest){
        //Build prompt
        String prompt = buildPrompt(emailRequest);
        //craft a request

        Map<String, Object> request = Map.of(
                "contents" , new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );
        //do a request and get a response

        String response = webClient.post()
                .uri(geminiApiUrl = geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        //Extract Reponse and Return Response
        return extractrResponseContent(response);
    }

    private String extractrResponseContent(String response){
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        }catch(Exception e){
            return "Error processing request : " + e.getMessage();

        }

    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. please dont generate a subject Line ");
        if(emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()){
            prompt.append("use a").append(emailRequest.getTone()).append(" tone. ");
        }
        prompt.append("\nOriginal mail: \n").append(emailRequest.getEmailContent());

        return prompt.toString();
    }
}
