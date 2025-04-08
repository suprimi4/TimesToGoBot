package suprimi4.timetogobot.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import suprimi4.timetogobot.dto.GeocodeResponse;
import suprimi4.timetogobot.dto.TelegramAddressRequest;
import suprimi4.timetogobot.dto.TelegramChatIdRequest;
import suprimi4.timetogobot.dto.UserInfoDTO;


@Component
public class GeocodeClient {
    private final RestTemplate restTemplate;
    private final String apiUrl;

    public GeocodeClient(RestTemplate restTemplate,
                         @Value("${api.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    public void resolveHomeAddress(Long chatId, String address) {
        TelegramAddressRequest request = new TelegramAddressRequest(chatId, address);
        ResponseEntity<GeocodeResponse> response = restTemplate.postForEntity(
                apiUrl + "/geocode/api/home",
                request,
                GeocodeResponse.class
        );

        if (response.getStatusCode().is4xxClientError() || response.getBody() == null) {
            throw new RuntimeException("Неудалось распознать адрес");
        }

        response.getBody();
    }

    public void resolveWorkAddress(Long chatId, String address) {
        TelegramAddressRequest request = new TelegramAddressRequest(chatId, address);
        ResponseEntity<GeocodeResponse> response = restTemplate.postForEntity(
                apiUrl + "/geocode/api/work",
                request,
                GeocodeResponse.class
        );

        if (response.getStatusCode().is4xxClientError() || response.getBody() == null) {
            throw new RuntimeException("Неудалось распознать адрес");
        }
        response.getBody();
    }

    public UserInfoDTO getUserInfo(Long chatId) {
        ResponseEntity<UserInfoDTO> response = restTemplate.postForEntity(
                apiUrl + "/geocode/api/userInfo",
                new TelegramChatIdRequest(chatId),
                UserInfoDTO.class
        );
        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
