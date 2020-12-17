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
                    if (!controller.curFlag) {
                        controller.curFlag = true;
                        ImageView temp1 = controller.player1, temp2 = controller.player2;
                        controller.player1 = (Integer.parseInt(messageFromServer) == 1) ? temp1 : temp2;
                        controller.player2 = (Integer.parseInt(messageFromServer) == 1) ? temp2 : temp1;
                    }
                    if (messageFromServer.charAt(0) == 'm') {
                        String[] data = messageFromServer.split(" ");
                        Platform.runLater(() ->
                                controller.movePlayer2(
                                        Double.parseDouble(data[1]), Double.parseDouble(data[2]), Integer.parseInt(data[3])
                                )
                        );
                    } else if (messageFromServer.charAt(0) == 'b') {
                        String[] data = messageFromServer.split(" ");
                        Platform.runLater(() ->
                                controller.shotPlayer2(
                                        Double.parseDouble(data[1]), Double.parseDouble(data[2]), Integer.parseInt(data[3])
                                )
                        );
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}