import java.util.ArrayList;
import java.util.List;

public class FakeNotificationSender implements NotificationSender {
    private final List<String> sentMessages = new ArrayList<>();

    @Override
    public void send(String message) {
        sentMessages.add(message);
    }

    public List<String> getSentMessages() {
        return sentMessages;
    }
}