package suprimi4.timetogobot.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class UserData {
    private String chatId;
    private String homeAddress;
    private String workAddress;
    private LocalTime arriveTime;
    private LocalDate lastNotificationDate;

    @Override
    public String toString() {
        return "Введенные вами данные:" + '\n' +
                "Домашний адрес: " + homeAddress + '\n' +
                "Рабочий адрес: " + workAddress + '\n' +
                "Время прибытия на работу " + arriveTime ;
    }
}
