public class ChangeRequestExample {
    public void lendEquipment(String userName, String equipmentName, boolean production) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";

        if (production) {
            new EmailNotificationService().send(message);
        } else {
            new ConsoleNotificationService().send(message);
        }
    }
}