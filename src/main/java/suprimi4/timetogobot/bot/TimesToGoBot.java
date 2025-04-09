package suprimi4.timetogobot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import suprimi4.timetogobot.api.GeocodeClient;
import suprimi4.timetogobot.api.RouteClient;
import suprimi4.timetogobot.dto.*;
import suprimi4.timetogobot.model.MessageState;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class TimesToGoBot extends TelegramLongPollingBot {
    private final String botUserName;

    private final GeocodeClient geocodeClient;
    private final RouteClient routeClient;
    private final Map<Long, UserData> userData = new HashMap<>();
    private final Map<Long, MessageState> messageState = new HashMap<>();

    public TimesToGoBot(@Value("${bot.username}") String botUserName,
                        @Value("${bot.token}") String botToken,
                        GeocodeClient geocodeClient,
                        RouteClient routeClient) {
        super(botToken);
        this.botUserName = botUserName;
        this.geocodeClient = geocodeClient;
        this.routeClient = routeClient;
    }

    @Override
    public String getBotUsername() {
        return botUserName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();

            if (message.equalsIgnoreCase("/start")) {
                sendMessage(chatId, "Добро пожаловать. Введите адрес проживания");
                messageState.put(chatId, MessageState.WAITING_HOME_ADDRESS);
            } else if (message.equalsIgnoreCase("/info")) {
                showUserInfo(chatId);
            } else {
                handleMessage(chatId, message);
            }
        }
    }

    private void handleMessage(Long chatId, String message) {
        MessageState currentState = messageState.get(chatId);
        UserData currentUser = userData.getOrDefault(chatId, new UserData());

        switch (currentState) {
            case WAITING_HOME_ADDRESS -> handleHomeAddress(chatId, message, currentUser);
            case WAITING_WORK_ADDRESS -> handleWorkAddress(chatId, message, currentUser);
            case WAITING_WORK_TIME -> handleWorkTime(chatId, message, currentUser);
            default -> sendMessage(chatId, "Что-то пошло не так, напиши /start, чтобы начать заново");
        }
    }

    private void handleHomeAddress(Long chatId, String message, UserData currentUser) {
        try {
            geocodeClient.resolveHomeAddress(chatId, message);

            currentUser.setHomeAddress(message);
            userData.put(chatId, currentUser);
            sendMessage(chatId, "Теперь введи адрес работы:");
            messageState.put(chatId, MessageState.WAITING_WORK_ADDRESS);
        } catch (Exception e) {
            sendMessage(chatId, "Не удалось распознать адрес. Пожалуйста, введите адрес проживания еще раз:");
        }
    }

    private void handleWorkAddress(Long chatId, String message, UserData currentUser) {
        try {
            geocodeClient.resolveWorkAddress(chatId, message);
            currentUser.setWorkAddress(message);
            userData.put(chatId, currentUser);
            sendMessage(chatId, "Введи время, когда нужно быть на работе (например, 09:00):");
            messageState.put(chatId, MessageState.WAITING_WORK_TIME);
        } catch (Exception e) {
            sendMessage(chatId, "Не удалось распознать адрес. Пожалуйста, введите адрес проживания еще раз:");
        }

    }

    private void handleWorkTime(Long chatId, String message, UserData currentUser) {
        try {
            LocalTime workTime = LocalTime.parse(message, DateTimeFormatter.ofPattern("HH:mm"));
            currentUser.setArriveTime(workTime);
            currentUser.setLastNotificationDate(null);
            userData.put(chatId, currentUser);

            UserInfoDTO userInfo = geocodeClient.getUserInfo(chatId);
            if (userInfo != null && userInfo.getTimezone() != null) {
                String infoMessage = """
                        Данные сохранены!
                        Пришлю уведомление за пол часа до оптимального времени
                        """;
                sendMessage(chatId, infoMessage);
            }

            messageState.put(chatId, MessageState.DONE);
        } catch (Exception e) {
            sendMessage(chatId, "Неверный формат времени. Введите время в формате HH:mm (например, 09:00)");
        }
    }

    @Scheduled(fixedRate = 60000)
    private void checkDepartureTimes() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        if (today == DayOfWeek.SATURDAY || today == DayOfWeek.SUNDAY) {
            return;
        }

        userData.forEach((chatId, userData) -> {
            if (userData.getArriveTime() != null) {
                UserInfoDTO userInfo = geocodeClient.getUserInfo(chatId);
                if (userInfo == null || userInfo.getTimezone() == null) return;

                ZoneId userZone = ZoneId.of(userInfo.getTimezone());
                ZonedDateTime nowInUserZone = ZonedDateTime.now(userZone);

                if (nowInUserZone.getDayOfWeek() == DayOfWeek.SATURDAY
                        || nowInUserZone.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    return;
                }

                LocalDate userDay = nowInUserZone.toLocalDate();
                if (userData.getLastNotificationDate() != null
                        && userData.getLastNotificationDate().equals(userDay)) {
                    return;
                }

                double durationSeconds = routeClient.getRouteDuration(chatId);
                LocalTime arriveTime = userData.getArriveTime();
                LocalTime departureTime = arriveTime.minusSeconds((long) durationSeconds);
                LocalTime notificationTime = departureTime.minusMinutes(30);
                LocalTime currentTime = nowInUserZone.toLocalTime();
                if (currentTime.isAfter(notificationTime) && currentTime.isBefore(departureTime)) {
                    String message = String.format("""
                                    Напоминание о выезде!
                                    Чтобы приехать к %s,
                                    вам нужно выехать в %s""",
                            arriveTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            departureTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    );
                    sendMessage(chatId, message);
                    userData.setLastNotificationDate(userDay);
                }
            }
        });
    }

    private void showUserInfo(Long chatId) {
        if (messageState.get(chatId) != MessageState.DONE) {
            sendMessage(chatId, "Сначала завершите настройку, используя команду /start");
            return;
        }

        UserInfoDTO userInfo = geocodeClient.getUserInfo(chatId);
        if (userInfo != null) {
            UserData localData = userData.get(chatId);

            String info = String.format("""
                    Ваши текущие настройки:
                    Домашний адрес: %s
                    Рабочий адрес: %s
                    Время прибытия: %s
                    Часовой пояс: %s
                    
                    Текущее время в вашем поясе: %s""",
                    userInfo.getHomeAddress(),
                    userInfo.getWorkAddress(),
                    localData.getArriveTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    userInfo.getTimezone(),
                    ZonedDateTime.now(ZoneId.of(userInfo.getTimezone())).format(DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy")));

            sendMessage(chatId, info);
        } else {
            sendMessage(chatId, "Не удалось получить информацию. Попробуйте позже.");
        }
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = SendMessage.builder().chatId(chatId).text(message).build();
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}