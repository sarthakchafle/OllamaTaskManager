package com.example.email.calendar.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskPlannerService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${email.sender.service.url}")
    private String emailSenderServiceUrl;

    @Value("${local.llm.api.url}")
    private String localLlmApiUrl;

    // This value will be injected from application.properties
    @Value("${google.calendar.service-account.key-path}")
    private String googleCalendarServiceAccountKeyPath;

    // This value will be injected from application.properties
    @Value("${google.calendar.id}")
    private String googleCalendarId;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private Calendar googleCalendarService;

    // Define the timezone for calendar operations
    private static final String CALENDAR_TIME_ZONE = "Asia/Kolkata";


    @Autowired
    public TaskPlannerService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        // The actual initialization of googleCalendarService is now in @PostConstruct
        // to ensure @Value properties are injected.
    }

    // This method runs after dependency injection is complete.
    // It's a good place to initialize components that depend on @Value properties.
    @PostConstruct
    public void init() {
        System.out.println("DEBUG: @Value for googleCalendarServiceAccountKeyPath: '" + googleCalendarServiceAccountKeyPath + "'");
        System.out.println("DEBUG: @Value for googleCalendarId: '" + googleCalendarId + "'");
        try {
            this.googleCalendarService = createGoogleCalendarService();
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Failed to initialize Google Calendar service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initializes the Google Calendar service client using a service account.
     * Requires a service account JSON key file.
     *
     * @return An initialized Google Calendar service.
     * @throws IOException If the key file cannot be read.
     * @throws GeneralSecurityException If there's a security issue during transport setup.
     */
    private Calendar createGoogleCalendarService() throws IOException, GeneralSecurityException {
        // Log the exact path being used by resourceLoader
        System.out.println("DEBUG: Attempting to load resource from classpath: 'classpath:" + googleCalendarServiceAccountKeyPath + "'");

        Resource resource = resourceLoader.getResource("classpath:" + googleCalendarServiceAccountKeyPath);
        if (!resource.exists()) {
            // This is the exact line that throws the IOException you are seeing.
            // It means resource.exists() returned false.
            throw new IOException("Google Calendar service account key file not found at: " + googleCalendarServiceAccountKeyPath);
        }

        InputStream in = resource.getInputStream();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR_EVENTS));

        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName("TaskPlannerEmailer")
                .build();
    }


    /**
     * Processes a user prompt, determining intent (send email or create event) and executing.
     *
     * @param prompt The user's natural language prompt.
     * @return A message indicating the outcome of the operation.
     */
    public String processUserPrompt(String prompt) {
        try {
            // Use LLM (Llama 3) to determine intent and extract entities
            Map<String, String> intentAndEntities = analyzePromptWithLlama3(prompt);
            String intent = intentAndEntities.get("intent");

            if ("create_event".equalsIgnoreCase(intent)) {
                String eventTitle = intentAndEntities.get("eventTitle");
                String eventDescription = intentAndEntities.get("eventDescription");
                String startTimeStr = intentAndEntities.get("startTime");
                String endTimeStr = intentAndEntities.get("endTime");

                if (eventTitle == null || startTimeStr == null) {
                    return "Error: Could not extract sufficient details to create a calendar event from your prompt. Missing title or start time.";
                }

                return createCalendarEvent(eventTitle, eventDescription, startTimeStr, endTimeStr);

            } else if ("send_email".equalsIgnoreCase(intent)) {
                String to = intentAndEntities.get("to"); // Always rely on LLM for 'to'
                String subject = intentAndEntities.get("subject"); // Always rely on LLM for 'subject'
                String body = intentAndEntities.getOrDefault("body", prompt); // Use prompt as body if no specific body extracted

                if (to == null || to.isEmpty() || subject == null || subject.isEmpty()) {
                    return "Error: Recipient email and subject are required to send an email, but LLM could not extract them from the prompt.";
                }
                return sendDirectEmail(to, subject, body);
            } else if ("delete_event".equalsIgnoreCase(intent)) { // NEW: Handle delete event intent
                String eventTitleToDelete = intentAndEntities.get("eventTitleToDelete");
                String eventDateToDelete = intentAndEntities.get("eventDateToDelete"); // Optional date for disambiguation

                if (eventTitleToDelete == null || eventTitleToDelete.isEmpty()) {
                    return "Error: Please provide the title of the event you wish to delete.";
                }
                return deleteCalendarEvent(eventTitleToDelete, eventDateToDelete);
            }
            else if ("general_chat".equalsIgnoreCase(intent)) { // Handle general chat intent
                String llmResponse = intentAndEntities.get("response");
                if (llmResponse != null && !llmResponse.isEmpty()) {
                    return "LLM Response: " + llmResponse;
                } else {
                    return "LLM responded, but no specific chat response was found.";
                }
            }
            else {
                return "Could not determine intent from your prompt. Please be more specific (e.g., 'send email to X', 'add event Y to calendar', 'delete event Z', or ask a general question).";
            }

        } catch (Exception e) {
            System.err.println("Error processing user prompt: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Calls the local LLM (Llama 3) to analyze the user's prompt for intent and entity extraction.
     *
     * @param userPrompt The prompt provided by the user.
     * @return A map containing "intent" and extracted entities like "eventTitle", "startTime", "to", "subject", "body", or "response" for general chat.
     */
    private Map<String, String> analyzePromptWithLlama3(String userPrompt) {
        // THIS IS THE UPDATED PROMPT FOR Llama 3 to handle general chat and delete event
        String prompt = "You are an AI assistant that helps manage tasks by identifying user intent. " +
                "Your output MUST be a valid JSON object. " +
                "If the user wants to 'send_email', extract 'to' (string, email address), 'subject' (string), and 'body' (string). " +
                "If the user wants to 'create_event', extract 'eventTitle' (string), 'eventDescription' (string, optional), 'startTime' (string, YYYY-MM-DDTHH:MM:SS), and 'endTime' (string, YYYY-MM-DDTHH:MM:SS, optional). " +
                "If the user wants to 'delete_event', extract 'eventTitleToDelete' (string) and 'eventDateToDelete' (string, YYYY-MM-DD, optional, for disambiguation). " +
                "If the user's request is a general conversational query and does not fit 'send_email', 'create_event', or 'delete_event', set the 'intent' to 'general_chat' and provide a 'response' field with your conversational reply to the user's prompt. " +
                "If a field is optional and not present, omit it from the JSON. " +
                "Here are some examples:\n\n" +
                "User: Schedule a team meeting for tomorrow at 10 AM for 1 hour. It's for the Q4 planning discussion.\n" +
                "Output: {\"intent\": \"create_event\", \"eventTitle\": \"Team Meeting (Q4 Planning)\", \"startTime\": \"2025-07-22T10:00:00\", \"endTime\": \"2025-07-22T11:00:00\", \"eventDescription\": \"Q4 planning discussion\"}\n\n" +
                "User: Add a reminder to call client XYZ on 2025-07-25 at 3:30 PM. It's about the new contract.\n" +
                "Output: {\"intent\": \"create_event\", \"eventTitle\": \"Call Client XYZ\", \"startTime\": \"2025-07-25T15:30:00\", \"eventDescription\": \"Discuss new contract\"}\n\n" +
                "User: Send an email to alice@example.com with the subject 'Project Update' and the body 'Hi Alice, the project is progressing well. Let\'s sync next week.'\n" +
                "Output: {\"intent\": \"send_email\", \"to\": \"alice@example.com\", \"subject\": \"Project Update\", \"body\": \"Hi Alice, the project is progressing well. Let\'s sync next week.\"}\n\n" +
                "User: Just send a quick note to bob@example.com about the meeting reminder.\n" +
                "Output: {\"intent\": \"send_email\", \"to\": \"bob@example.com\", \"subject\": \"Meeting Reminder\", \"body\": \"Just a quick note about the meeting reminder.\"}\n\n" +
                "User: Delete the 'Daily Standup' meeting.\n" +
                "Output: {\"intent\": \"delete_event\", \"eventTitleToDelete\": \"Daily Standup\"}\n\n" +
                "User: Cancel the 'Project Review' on 2025-07-25.\n" +
                "Output: {\"intent\": \"delete_event\", \"eventTitleToDelete\": \"Project Review\", \"eventDateToDelete\": \"2025-07-25\"}\n\n" +
                "User: Hi\n" +
                "Output: {\"intent\": \"general_chat\", \"response\": \"Hello! How can I assist you today?\"}\n\n" +
                "User: What is the capital of France?\n" +
                "Output: {\"intent\": \"general_chat\", \"response\": \"The capital of France is Paris.\"}\n\n" +
                "User: " + userPrompt + "\n" +
                "Output:";


        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", "llama3"); // Or the specific model name your local LLM uses
        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", "You are a helpful assistant that extracts information from user requests and outputs valid JSON."));
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", prompt));
        payload.set("messages", messages);

        // Request JSON format response from Llama 3 (if supported by your local setup)
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");
        payload.set("response_format", responseFormat);

        System.out.println("DEBUG: Calling Local LLM (Llama 3) for intent analysis at: " + localLlmApiUrl);
        System.out.println("DEBUG: LLM Request Payload: " + payload.toString()); // Log the full payload

        Map<String, String> result = new HashMap<>();
        try {
            String responseBody = webClient.post()
                    .uri(localLlmApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("DEBUG: LLM Raw Response: " + responseBody); // Log the raw response from LLM

            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").get(0).has("message")
                    && root.get("choices").get(0).get("message").has("content")) {
                String jsonText = root.get("choices").get(0).get("message").get("content").asText();
                System.out.println("DEBUG: LLM Extracted JSON Text: " + jsonText); // Log the extracted JSON string

                try {
                    JsonNode parsedJson = objectMapper.readTree(jsonText);
                    System.out.println("DEBUG: Parsed JSON: " + parsedJson.toString()); // Log the parsed JSON object

                    if (parsedJson.has("intent")) {
                        result.put("intent", parsedJson.get("intent").asText());
                    }
                    // Event fields
                    if (parsedJson.has("eventTitle")) {
                        result.put("eventTitle", parsedJson.get("eventTitle").asText());
                    }
                    if (parsedJson.has("eventDescription")) {
                        result.put("eventDescription", parsedJson.get("eventDescription").asText());
                    }
                    if (parsedJson.has("startTime")) {
                        result.put("startTime", parsedJson.get("startTime").asText());
                    }
                    if (parsedJson.has("endTime")) {
                        result.put("endTime", parsedJson.get("endTime").asText());
                    }
                    // Email fields
                    if (parsedJson.has("to")) {
                        result.put("to", parsedJson.get("to").asText());
                    }
                    if (parsedJson.has("subject")) {
                        result.put("subject", parsedJson.get("subject").asText());
                    }
                    if (parsedJson.has("body")) {
                        result.put("body", parsedJson.get("body").asText());
                    }
                    // General chat response field
                    if (parsedJson.has("response")) {
                        result.put("response", parsedJson.get("response").asText());
                    }
                    // NEW: Delete event fields
                    if (parsedJson.has("eventTitleToDelete")) {
                        result.put("eventTitleToDelete", parsedJson.get("eventTitleToDelete").asText());
                    }
                    if (parsedJson.has("eventDateToDelete")) {
                        result.put("eventDateToDelete", parsedJson.get("eventDateToDelete").asText());
                    }

                } catch (Exception jsonParseError) {
                    System.err.println("ERROR: Failed to parse LLM's JSON response: " + jsonParseError.getMessage());
                    System.err.println("LLM response that caused parsing error: " + jsonText);
                    // If parsing fails, LLM didn't return valid JSON, so intent can't be determined.
                    result.put("intent", "unknown");
                }
            } else {
                System.err.println("Unexpected Llama 3 API response structure for intent analysis: " + responseBody);
                result.put("intent", "unknown"); // Set intent to unknown if structure is unexpected
            }
        } catch (Exception e) {
            System.err.println("Error calling Local LLM (Llama 3) for intent analysis: " + e.getMessage());
            e.printStackTrace();
            result.put("intent", "unknown"); // Set intent to unknown on API call error
        }
        return result;
    }


    /**
     * Creates an event in Google Calendar.
     *
     * @param eventTitle The title of the event.
     * @param eventDescription The description of the event (can be null).
     * @param startTimeStr The start time string (e.g., "2025-07-21T10:00:00").
     * @param endTimeStr The end time string (e.g., "2025-07-21T11:00:00", can be null).
     * @return A message indicating success or failure.
     */
    private String createCalendarEvent(String eventTitle, String eventDescription, String startTimeStr, String endTimeStr) {
        if (googleCalendarService == null) {
            return "Error: Google Calendar service not initialized. Check application logs for details.";
        }

        Event event = new Event()
                .setSummary(eventTitle)
                .setDescription(eventDescription);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // For YYYY-MM-DDTHH:MM:SS

        try {
            LocalDateTime startLdt = LocalDateTime.parse(startTimeStr, formatter);
            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startLdt.toString() + ":00")) // Append :00 for seconds if not present
                    .setTimeZone(CALENDAR_TIME_ZONE); // Use defined timezone

            event.setStart(start);

            if (endTimeStr != null && !endTimeStr.isEmpty()) {
                LocalDateTime endLdt = LocalDateTime.parse(endTimeStr, formatter);
                EventDateTime end = new EventDateTime()
                        .setDateTime(new DateTime(endLdt.toString() + ":00")) // Append :00 for seconds if not present
                        .setTimeZone(CALENDAR_TIME_ZONE); // Use defined timezone
                event.setEnd(end);
            } else {
                // If no end time, set end time to 1 hour after start time
                LocalDateTime endLdt = startLdt.plusHours(1);
                EventDateTime end = new EventDateTime()
                        .setDateTime(new DateTime(endLdt.toString() + ":00"))
                        .setTimeZone(CALENDAR_TIME_ZONE);
                event.setEnd(end);
            }

            event = googleCalendarService.events().insert(googleCalendarId, event).execute();
            System.out.printf("Event created: %s\n", event.getHtmlLink());
            return "Calendar event '" + eventTitle + "' created successfully! Link: " + event.getHtmlLink();

        } catch (DateTimeParseException e) {
            System.err.println("Date/Time parsing error: " + e.getMessage());
            return "Error: Invalid date/time format. Please use YYYY-MM-DDTHH:MM:SS (e.g., 2025-07-21T10:00:00).";
        } catch (IOException e) {
            System.err.println("Error creating Google Calendar event: " + e.getMessage());
            e.printStackTrace();
            return "Error creating calendar event: " + e.getMessage();
        }
    }

    /**
     * Finds a Google Calendar event by title and an optional date.
     *
     * @param eventTitle The title of the event to search for.
     * @param eventDate The date of the event (YYYY-MM-DD), optional for disambiguation.
     * @return The ID of the found event, or null if not found.
     * @throws IOException If there's an issue with the Google Calendar API call.
     */
    private String findCalendarEvent(String eventTitle, String eventDate) throws IOException {
        if (googleCalendarService == null) {
            System.err.println("Google Calendar service not initialized. Cannot find event.");
            return null;
        }

        // Set time range for search (e.g., next 7 days or around the specified date)
        DateTime now = new DateTime(System.currentTimeMillis());
        DateTime oneWeekFromNow = new DateTime(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);

        // If a specific date is provided, narrow the search to that day
        if (eventDate != null && !eventDate.isEmpty()) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(eventDate + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                ZonedDateTime startOfDay = localDateTime.atZone(ZoneId.of(CALENDAR_TIME_ZONE));
                ZonedDateTime endOfDay = localDateTime.atZone(ZoneId.of(CALENDAR_TIME_ZONE)).plusDays(1).minusSeconds(1);

                now = new DateTime(startOfDay.toInstant().toEpochMilli());
                oneWeekFromNow = new DateTime(endOfDay.toInstant().toEpochMilli());
            } catch (DateTimeParseException e) {
                System.err.println("Warning: Could not parse eventDateToDelete: " + eventDate + ". Searching across a broader range.");
                // Fallback to broader search if date parsing fails
            }
        }


        Events events = googleCalendarService.events().list(googleCalendarId)
                .setTimeMin(now)
                .setTimeMax(oneWeekFromNow) // Search within a reasonable future window
                .setQ(eventTitle) // Search by query (summary/description)
                .setSingleEvents(true) // Expand recurring events
                .setOrderBy("startTime")
                .execute();

        List<Event> items = events.getItems();
        if (items != null && !items.isEmpty()) {
            // Find the first event that exactly matches the title (case-insensitive)
            for (Event event : items) {
                if (event.getSummary() != null && event.getSummary().equalsIgnoreCase(eventTitle)) {
                    System.out.println("Found event to delete: " + event.getSummary() + " (ID: " + event.getId() + ")");
                    return event.getId();
                }
            }
        }
        System.out.println("No event found with title: '" + eventTitle + "' and date: '" + (eventDate != null ? eventDate : "any") + "'");
        return null;
    }

    /**
     * Deletes a Google Calendar event by its title and an optional date.
     *
     * @param eventTitle The title of the event to delete.
     * @param eventDate The date of the event (YYYY-MM-DD), optional for disambiguation.
     * @return A message indicating success or failure of the deletion.
     * @throws IOException If there's an issue with the Google Calendar API call.
     */
    private String deleteCalendarEvent(String eventTitle, String eventDate) throws IOException {
        if (googleCalendarService == null) {
            return "Error: Google Calendar service not initialized. Cannot delete event.";
        }

        String eventIdToDelete = findCalendarEvent(eventTitle, eventDate);

        if (eventIdToDelete != null) {
            try {
                googleCalendarService.events().delete(googleCalendarId, eventIdToDelete).execute();
                return "Calendar event '" + eventTitle + "' deleted successfully.";
            } catch (IOException e) {
                System.err.println("Error deleting Google Calendar event (ID: " + eventIdToDelete + "): " + e.getMessage());
                e.printStackTrace();
                return "Error deleting calendar event: " + e.getMessage();
            }
        } else {
            return "Could not find a calendar event with title '" + eventTitle + "'" + (eventDate != null && !eventDate.isEmpty() ? " on " + eventDate : "") + " to delete.";
        }
    }


    /**
     * Calls the Email Sender Microservice to send a direct email.
     *
     * @param to The recipient's email address.
     * @param subject The email subject.
     * @param body The email body.
     * @return The response from the email service.
     */
    private String sendDirectEmail(String to, String subject, String body) {
        String url = emailSenderServiceUrl + "/api/email/send";
        System.out.println("Calling Email Sender Service at: " + url);

        Map<String, String> emailRequest = new HashMap<>();
        emailRequest.put("to", to);
        emailRequest.put("subject", subject);
        emailRequest.put("body", body);

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(emailRequest)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Blocking call
    }
}
