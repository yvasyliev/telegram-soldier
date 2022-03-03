package stop.war;

import it.tdlight.common.utils.CantLoadLibrary;
import stop.war.readers.PhonesReader;
import stop.war.telegram.TelegramClientWrapper;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final int API_ID = 19390509;
    private static final String API_HASH = "50c2cca5541478be055e84d897ed4ba1";
    private static final String PHONE_NUMBER = "+48781403253";

    public static void main(String[] args) throws CantLoadLibrary, ExecutionException, InterruptedException, IOException, TimeoutException {
        TelegramClientWrapper tg = new TelegramClientWrapper();
        tg.init(API_ID, API_HASH);

        Scanner scanner = new Scanner(System.in);
        String command;
        do {
            System.out.print("enter command: ");
            switch (command = scanner.next()) {
                case "login":
                    tg.login(PHONE_NUMBER);
                    break;

                case "reload_contacts":
                    tg.clearContacts();
                    tg.addContacts(new PhonesReader().read("phones.txt"));
                    break;

                case "notification1":
                    tg.notification1(new PhonesReader().read("phones.txt"));
                    break;
            }

        } while (!"exit".equals(command));

        tg.close();
    }
}
