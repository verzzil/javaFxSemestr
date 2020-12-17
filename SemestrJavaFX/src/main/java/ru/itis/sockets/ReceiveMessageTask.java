package ru.itis.sockets;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.itis.controllers.FirstMapController;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

public class ReceiveMessageTask extends Task<Void> {
    private BufferedReader fromServer;

    private FirstMapController controller;

    public ReceiveMessageTask(BufferedReader fromServer, FirstMapController controller) {
        this.fromServer = fromServer;
        this.controller = controller;
    }

    @Override
    protected Void call() {
        while (true) {
            try {
                String messageFromServer = fromServer.readLine();
                if (messageFromServer != null) {
                    System.out.println(messageFromServer);
                    if(!controller.curFlag) {
                        controller.curFlag = true;
                        ImageView temp1 = controller.player1, temp2 = controller.player2;
                        controller.player1 = (Integer.parseInt(messageFromServer) == 1) ? temp1 : temp2;
                        controller.player2 = (Integer.parseInt(messageFromServer) == 1) ? temp2 : temp1;
                    }
                    if(messageFromServer.charAt(0) == 'm') {
                        String[] coords = messageFromServer.split(" ");
                        Platform.runLater(() -> controller.movePlayer2(Double.parseDouble(coords[1]), Double.parseDouble(coords[2])));
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}