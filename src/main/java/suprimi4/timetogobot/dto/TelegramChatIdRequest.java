package suprimi4.timetogobot.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelegramChatIdRequest {
    private Long chatId;
}
