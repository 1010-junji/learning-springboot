public class EquipmentService {
    private final NotificationSender notificationSender;

    public EquipmentService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void lendEquipment(String userName, String equipmentName) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationSender.send(message);
    }
}