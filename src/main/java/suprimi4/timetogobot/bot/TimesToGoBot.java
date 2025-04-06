package suprimi4.timetogobot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import suprimi4.timetogobot.dto.UserData;
import suprimi4.timetogobot.model.MessageState;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class TimesToGoBot extends TelegramLongPollingBot {
    private final String botUserName;
    private final Map<Long, UserData> userData = new HashMap<>();

    //TODO
    private final Map<Long, MessageState> messageState = new HashMap<>();

    public TimesToGoBot(@Value("${bot.username}") String botUserName,
                        @Value("${bot.token}") String botToken) {
        super(botToken);
        this.botUserName = botUserName;
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }


    @Override
    public void onUpdateReceived(Update update) {
        //Вытягивать user_id
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();
            if (message.equalsIgnoreCase("/start")) {
                sendMessage(chatId, "Добро пожаловать. Введите адрес проживания");
                messageState.put(chatId, MessageState.WAITING_HOME_ADDRESS);
            } else {
                handleMessage(chatId, message);
            }

        }
    }

    private void handleMessage(Long chatId, String message) {
        MessageState currentState = messageState.get(chatId);
        UserData currentUser = userData.getOrDefault(chatId, new UserData());
        //TODO каждая ручка должна возвращать user info
        switch (currentState) {

            case WAITING_HOME_ADDRESS -> {
                currentUser.setHomeAddress(message);
                userData.put(chatId, currentUser);
                sendMessage(chatId, "Теперь введи адрес работы:");
                //TODO dadata request + validate if result null. (Домашняя ручка)
                messageState.put(chatId, MessageState.WAITING_WORK_ADDRESS);
            }
            case WAITING_WORK_ADDRESS -> {
                currentUser.setWorkAddress(message);
                sendMessage(chatId, "Введи время, когда нужно быть на работе (например, 09:00):");
                //TODO dadata request + validate if result null. (Рабочая ручка)
                //TODO валидация расстояния рабочего адреса и домашнего
                messageState.put(chatId, MessageState.WAITING_WORK_TIME);
            }
            case WAITING_WORK_TIME -> {
                LocalTime workTime = LocalTime.parse(message, DateTimeFormatter.ofPattern("HH:mm"));
                //TODO set user worktime
                currentUser.setArriveTime(workTime);
                sendMessage(chatId, "Данные сохранены! Я напомню тебе, когда выезжать." + '\n'
                        + userData.get(chatId).toString()
                );

                messageState.put(chatId, MessageState.DONE);
            }
            //TODO спросить у пользователя верны ли данные (новый state)
            default -> sendMessage(chatId, "Что-то пошло не так, напиши /start, чтобы начать заново");
        }

    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage
                .builder()
                .chatId(chatId)
                .text(message)
                .build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }


}
