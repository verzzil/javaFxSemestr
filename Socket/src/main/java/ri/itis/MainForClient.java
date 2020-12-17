package ri.itis;

import java.util.Random;
import java.util.Scanner;

public class MainForClient {
    public static void main(String[] args) {
        SocketClient client = new SocketClient("localhost", 7477);

        Scanner scan = new Scanner(System.in);

        while(true) {
            String message = scan.nextLine();

            client.sendMessage(message);
        }
    }
}
