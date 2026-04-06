public class EquipmentService {
    private final ConsoleNotificationService notificationService = new ConsoleNotificationService();

    public void lendEquipment(String userName, String equipmentName) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationService.send(message);
    }
}