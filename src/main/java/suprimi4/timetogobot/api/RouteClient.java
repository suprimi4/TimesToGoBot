package suprimi4.timetogobot.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import suprimi4.timetogobot.dto.TelegramChatIdRequest;

@Component
public class RouteClient {
    private final RestTemplate restTemplate;


    private final String apiUrl;

    public RouteClient(RestTemplate restTemplate,
                       @Value("${api.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }


    public double getRouteDuration(Long chatId) {
        ResponseEntity<Double> response = restTemplate.postForEntity(apiUrl + "/routing/api/duration", new TelegramChatIdRequest(chatId), Double.class);
        return response.getBody() != null ? response.getBody() : 0;
    }
}
