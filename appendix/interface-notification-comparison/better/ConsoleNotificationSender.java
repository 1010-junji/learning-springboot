public class ConsoleNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[Console] " + message);
    }
}