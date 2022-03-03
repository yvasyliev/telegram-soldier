package stop.war.telegram;

import it.tdlight.common.Init;
import it.tdlight.common.TelegramClient;
import it.tdlight.common.UpdatesHandler;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;
import it.tdlight.tdlight.ClientManager;

import java.io.IOError;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TelegramClientWrapper {
    private static final int TIMEOUT = 5;
    private static final String TEXT = "https://youtu.be/e0kqlwSwdf4\n" +
            "Привет, мы из Украины. Из свободной, демократической Украины. Страны, где слово народа имеет силу.\n" +
            " \n" +
            "Из той страны, в которую без приглашения пришли Путинские \"русский мир\", \"русская весна\". Страны, куда пришли воевать дети русских матерей за Путинские предубеждения о неонацистах.\n" +
            "И сегодня мы одни из тех небезразличных, которые стремятся донести правду народу Российской Федерации.\n" +
            "Разговор с вами мы хотим  начать с простых вопросов:\n" +
            "\n" +
            "\n" +
            "Где сейчас Ваш президент? (а наш с народом, под градами в Киеве)\n" +
            "Что вы видите вокруг? (а мы войну и ее последствия)\n" +
            "За чью идею сражаетесь вы и ваш народ? (а мы защищаем суверенность и мир. Свой и своего государства).\n" +
            "Собираетесь ли вы нести ответственность за жизнь собственную и жизнь вашего народа (или во всем, как всегда, виноват \"гниющий Запад\"?)\n" +
            "\n" +
            "Свободный народ Российской Федерации (если вы свободны)! Свобода – это не только свобода действия, но и ответственность за нее.\n" +
            "Вы зовете украинцев братьями, так где же ваше братство? Как оно проявляется?\n" +
            "Мы не звали нас спасать (да и от чего?) – вы пришли сюда танками. Мы кричим: \"Остановитесь!\" Вы вместо этого – в вакууме.\n" +
            "Российские селебрити и блогеры с многомиллионной аудиторией жалуются на напор украинского народа в соцсетях. А как иначе донести правду простому народу, когда всё ваше информационное пространство проплачено, а независимые СМИ объявлены иностранными агентами и закрыты от вашего взора?! Вы жалуетесь на то, что мы, украинцы, свирепствуем, а как не злиться на ваше бездействие и безответственность?! За вашу же жизнь! Россияне! Вас 144 млн, а вы не можете свергнуть с трона ботоксного карлика и навести порядок в своем государстве? Где же ваше стремление к свободе, если вы толпой убегаете от одного омоновца? Вас сажают? Нас убивают. Вы доверили свою жизнь гнусному человечку, который сидит где-то в бункере и чихать хотел на народ России. Так выходит Вам плевать на себя? Не плевать? Выходите на улицы и докажите это!\n" +
            "\n" +
            "Перешлите это сообщение всем, кого знаете! Пожалуйста!";
    private final TelegramClient telegramClient = ClientManager.create();

    public void init(int apiId, String apiHash) throws CantLoadLibrary, InterruptedException, ExecutionException, TimeoutException {
        Init.start();
        telegramClient.initialize(
                (UpdatesHandler) list -> {
                    list.stream()
                            .filter(o -> !(o instanceof TdApi.UpdateAuthorizationState))
                            .filter(o -> !(o instanceof TdApi.UpdateOption))
                            .filter(o -> !(o instanceof TdApi.UpdateConnectionState))
                            .peek(o -> {
                                if (o instanceof TdApi.UpdateMessageSendFailed) {
                                    System.out.println(o);
                                }
                            })
                            .map(TdApi.Object::getClass)
                            .forEach(System.out::println);

                },
                Throwable::printStackTrace,
                Throwable::printStackTrace
        );
        telegramClient.execute(new TdApi.SetLogVerbosityLevel(0));
        if (telegramClient.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        TdApi.TdlibParameters tdlibParameters = new TdApi.TdlibParameters();
        tdlibParameters.apiHash = apiHash;
        tdlibParameters.apiId = apiId;
        tdlibParameters.systemLanguageCode = "en";
        tdlibParameters.deviceModel = "Desktop";
        tdlibParameters.applicationVersion = "0.5";

        sendSynchronously(new TdApi.SetTdlibParameters(tdlibParameters));

        sendSynchronously(new TdApi.CheckDatabaseEncryptionKey());
    }

    public void close() throws InterruptedException, ExecutionException, TimeoutException {
        sendSynchronously(new TdApi.Close());
    }

    public void login(String phoneNumber) throws InterruptedException, ExecutionException, TimeoutException {
        sendAuthenticationCode(phoneNumber);
        String code = promptCode();
        checkCode(code);
        System.out.println("Login is completed.");
    }

    public void clearContacts() throws ExecutionException, InterruptedException, TimeoutException {
        sendSynchronously(new TdApi.RemoveContacts(getContacts().userIds));
    }

    public void addContacts(List<String> phones) throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("Adding contacts. Count: " + phones.size());
        TdApi.Contact[] contacts = phones.stream().map(phone -> {
            TdApi.Contact contact = new TdApi.Contact();
            contact.phoneNumber = phone;
            return contact;
        }).toArray(TdApi.Contact[]::new);
        TdApi.Object response = sendSynchronously(new TdApi.ImportContacts(contacts));
        System.out.println(response);
        System.out.println("Contacts are added.");
    }

    public void notification1(List<String> phones) throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("Sending notification1. Contacts: " + phones.size());
        TdApi.Users contacts = getContacts();
        for (long userId : contacts.userIds) {
            TdApi.Chat chat = createPrivateChat(userId);

            TdApi.SendMessage sendMessage = new TdApi.SendMessage();
            sendMessage.chatId = chat.id;

            TdApi.InputMessageText text = new TdApi.InputMessageText();
            text.text = new TdApi.FormattedText(TEXT, null);
            sendMessage.inputMessageContent = text;

            TdApi.Object response = sendSynchronously(sendMessage);
            System.out.println(response);
        }
        System.out.println("notification1 is sent.");
    }

    private long findContactId(String phone) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.Users users = (TdApi.Users) sendSynchronously(new TdApi.SearchContacts(phone, 1));
        return users == null || users.totalCount == 0 ? 0 : users.userIds[0];
    }

    private TdApi.Users getContacts() throws ExecutionException, InterruptedException, TimeoutException {
        return (TdApi.Users) sendSynchronously(new TdApi.GetContacts());
    }

    private TdApi.Chat createPrivateChat(long userId) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.Object object = sendSynchronously(new TdApi.CreatePrivateChat(userId, false));
        return (TdApi.Chat) object;
    }

    private void sendAuthenticationCode(String phoneNumber) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.SetAuthenticationPhoneNumber setAuthenticationPhoneNumber = new TdApi.SetAuthenticationPhoneNumber();
        setAuthenticationPhoneNumber.phoneNumber = phoneNumber;
        sendSynchronously(setAuthenticationPhoneNumber);
    }

    private String promptCode() {
        System.out.print("Please enter code: ");
        return new Scanner(System.in).next();
    }

    private void checkCode(String code) throws ExecutionException, InterruptedException, TimeoutException {
        sendSynchronously(new TdApi.CheckAuthenticationCode(code));
    }

    private <T extends TdApi.Object> TdApi.Object sendSynchronously(TdApi.Function<T> request) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<TdApi.Object> response = new CompletableFuture<>();
        telegramClient.send(request, response::complete, response::completeExceptionally);
        return response.get(1000000, TimeUnit.SECONDS);
    }
}
