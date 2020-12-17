package ru.itis.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ru.itis.consts.Direction;
import ru.itis.consts.MapSchemas;
import ru.itis.sockets.ReceiveMessageTask;
import ru.itis.sockets.SocketClient;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstMapController implements Initializable {

    public boolean curFlag = false;
    public ImageView player1;
    public ImageView player2;
    public AnchorPane pane;
    public ImageView bullet;
    public ImageView opponentBullet;
    private List<ImageView> bricks = new ArrayList<>();
    private List<ImageView> concretes = new ArrayList<>();
    private List<ImageView> water = new ArrayList<>();
    List<ImageView> obstacles = new ArrayList<>();

    private int currentDirection = Direction.TOP;
    private int currentBulletDirection;
    private boolean isBulletActive = false;
    private boolean isOpponentBulletActive = false;


    private Timeline bulletTimeLine;
    private Timeline opponentBulletTimeLine;
    private SocketClient client;


    public EventHandler<KeyEvent> keyEventEventHandler = event -> {
        boolean playersIntersect = player1.getBoundsInParent().intersects(player2.getBoundsInParent());
        boolean obstaclesIntersect = isObstaclesIntersect(player1);
        boolean isWentAboard = isWentAbroad(player1);



        if (event.getCode() == KeyCode.UP) {
            player1.setRotate(0);
            currentDirection = Direction.TOP;
            removeIntersect(playersIntersect, obstaclesIntersect, isWentAboard, "y");
        } else if (event.getCode() == KeyCode.DOWN) {
            player1.setRotate(180);
            currentDirection = Direction.BOTTOM;
            removeIntersect(playersIntersect, obstaclesIntersect, isWentAboard, "y");
        } else if (event.getCode() == KeyCode.LEFT) {
            player1.setRotate(270);
            currentDirection = Direction.LEFT;
            removeIntersect(playersIntersect, obstaclesIntersect, isWentAboard, "x");
        } else if (event.getCode() == KeyCode.RIGHT) {
            player1.setRotate(90);
            currentDirection = Direction.RIGHT;
            removeIntersect(playersIntersect, obstaclesIntersect, isWentAboard, "x");
        } else if (event.getCode() == KeyCode.CONTROL) {
            if (!isBulletActive) {
                InputStream inputStream = getClass().getResourceAsStream("/images/bullet.png");
                Image image = new Image(inputStream);
                bullet = new ImageView(image);

                bullet.setFitHeight(10);
                bullet.setFitWidth(10);
                bullet.setLayoutX(player1.getLayoutX() + bullet.getFitWidth());
                bullet.setLayoutY(player1.getLayoutY() + bullet.getFitHeight());
                pane.getChildren().add(bullet);

                currentBulletDirection = currentDirection;

                client.sendMessage("b " + player1.getLayoutX() + " " + player1.getLayoutY() + " " + currentBulletDirection);

                bulletTimeLine = new Timeline(
                        new KeyFrame(
                                Duration.seconds(0.003),
                                animation -> {
                                    if (
                                            opponentBullet != null &&
                                                    opponentBullet.getBoundsInParent().intersects(bullet.getBoundsInParent())
                                    ) {
                                        removeOpponentBullet();
                                        removeBullet();
                                        bullet = null;
                                        opponentBullet = null;
                                        return;
                                    }
                                    switch (currentBulletDirection) {
                                        case Direction.TOP:
                                            bullet.setRotate(0);
                                            bullet.setLayoutY(bullet.getLayoutY() - 2);
                                            break;
                                        case Direction.BOTTOM:
                                            bullet.setRotate(180);
                                            bullet.setLayoutY(bullet.getLayoutY() + 2);
                                            break;
                                        case Direction.LEFT:
                                            bullet.setRotate(270);
                                            bullet.setLayoutX(bullet.getLayoutX() - 2);
                                            break;
                                        case Direction.RIGHT:
                                            bullet.setRotate(90);
                                            bullet.setLayoutX(bullet.getLayoutX() + 2);
                                            break;
                                        default:
                                            break;
                                    }
                                    if (
                                            bullet.getLayoutY() >= pane.getHeight() ||
                                                    bullet.getLayoutY() <= 0 ||
                                                    bullet.getLayoutX() >= pane.getWidth() ||
                                                    bullet.getLayoutX() <= 0
                                    )
                                        removeBullet();
                                    for (ImageView brick : bricks) {
                                        if (bullet.getBoundsInParent().intersects(brick.getBoundsInParent())) {
                                            pane.getChildren().remove(brick);
                                            bricks.remove(brick);
                                            obstacles.remove(brick);
                                            removeBullet();
                                            break;
                                        }
                                    }
                                    for (ImageView concrete : concretes) {
                                        if (bullet.getBoundsInParent().intersects(concrete.getBoundsInParent())) {
                                            removeBullet();
                                            break;
                                        }
                                    }
                                    if (bullet.getBoundsInParent().intersects(player2.getBoundsInParent())) {
                                        removeBullet();
                                        pane.getChildren().remove(player2);
                                        gameOver();
                                    }
                                }
                        )
                );

                bulletTimeLine.setCycleCount(1000);
                bulletTimeLine.setOnFinished(event1 -> {
                    pane.getChildren().remove(bullet);
                    isBulletActive = false;
                });
                bulletTimeLine.play();
                isBulletActive = true;
            }
        }

        if(
                event.getCode() == KeyCode.UP ||
                        event.getCode() == KeyCode.DOWN ||
                        event.getCode() == KeyCode.LEFT ||
                        event.getCode() == KeyCode.RIGHT
        )
            client.sendMessage("m " + player1.getLayoutX() + " " + player1.getLayoutY() + " " + currentDirection);

    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        client = new SocketClient("localhost", 7477);
        ReceiveMessageTask receiveMessageTask = new ReceiveMessageTask(client.getFromServer(), this);
        ExecutorService service = Executors.newFixedThreadPool(1);
        service.execute(receiveMessageTask);

        pane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, new Insets(0, 0, 0, 0))));
        String scheme = MapSchemas.SECOND_MAP;
        renderMap(scheme);

        obstacles = new ArrayList<>(bricks);
        obstacles.addAll(new ArrayList<>(concretes));
        obstacles.addAll(new ArrayList<>(water));

    }

    private void renderMap(String scheme) {
        int currentX = 0, currentY = -40;
        for (int i = 0; i < scheme.length(); i++) {
            if (i % 23 == 0) {
                currentY += 40;
                currentX = 0;
            }

            if (scheme.charAt(i) == 'b') {
                InputStream inputStream = this.getClass().getResourceAsStream("/images/brick.png");
                Image image = new Image(inputStream);
                ImageView brick = new ImageView(image);

                brick.setLayoutX(currentX);
                brick.setLayoutY(currentY);
                pane.getChildren().add(brick);

                bricks.add(brick);
            } else if (scheme.charAt(i) == 'c') {
                InputStream inputStream = this.getClass().getResourceAsStream("/images/concrete.png");
                Image image = new Image(inputStream);
                ImageView concrete = new ImageView(image);

                concrete.setLayoutX(currentX);
                concrete.setLayoutY(currentY);
                pane.getChildren().add(concrete);

                concretes.add(concrete);
            } else if (scheme.charAt(i) == 'g') {
                InputStream inputStream = this.getClass().getResourceAsStream("/images/greens.png");
                Image image = new Image(inputStream);
                ImageView greens = new ImageView(image);

                greens.setLayoutX(currentX);
                greens.setLayoutY(currentY);
                pane.getChildren().add(greens);

            } else if (scheme.charAt(i) == 'w') {
                InputStream inputStream = this.getClass().getResourceAsStream("/images/water.png");
                Image image = new Image(inputStream);
                ImageView water = new ImageView(image);

                water.setLayoutX(currentX);
                water.setLayoutY(currentY);
                pane.getChildren().add(water);

                this.water.add(water);
            }
            currentX += 40;
        }
    }

    private void removeIntersect(boolean playersIntersect, boolean obstaclesIntersect, boolean isWentAboard, String axis) {
        if (axis.equals("y")) {
            if (!playersIntersect && !obstaclesIntersect && !isWentAboard)
                if (currentDirection == Direction.TOP)
                    player1.setLayoutY(player1.getLayoutY() - 5);
                else
                    player1.setLayoutY(player1.getLayoutY() + 5);

            playersIntersect = player1.getBoundsInParent().intersects(player2.getBoundsInLocal());
            obstaclesIntersect = isObstaclesIntersect(player1);
            isWentAboard = isWentAbroad(player1);

            while (playersIntersect || obstaclesIntersect || isWentAboard) {
                if (currentDirection == Direction.TOP)
                    player1.setLayoutY(player1.getLayoutY() + 1);
                else
                    player1.setLayoutY(player1.getLayoutY() - 1);
                playersIntersect = player1.getBoundsInParent().intersects(player2.getBoundsInParent());
                obstaclesIntersect = isObstaclesIntersect(player1);
                isWentAboard = isWentAbroad(player1);
            }
        } else if (axis.equals("x")) {
            if (!playersIntersect && !obstaclesIntersect && !isWentAboard)
                if (currentDirection == Direction.RIGHT)
                    player1.setLayoutX(player1.getLayoutX() + 5);
                else
                    player1.setLayoutX(player1.getLayoutX() - 5);

            playersIntersect = player1.getBoundsInParent().intersects(player2.getBoundsInParent());
            obstaclesIntersect = isObstaclesIntersect(player1);
            isWentAboard = isWentAbroad(player1);

            while (playersIntersect || obstaclesIntersect || isWentAboard) {
                if (currentDirection == Direction.RIGHT)
                    player1.setLayoutX(player1.getLayoutX() - 1);
                else
                    player1.setLayoutX(player1.getLayoutX() + 1);
                playersIntersect = player1.getBoundsInParent().intersects(player2.getBoundsInParent());
                obstaclesIntersect = isObstaclesIntersect(player1);
                isWentAboard = isWentAbroad(player1);
            }
        }
    }

    private void removeBullet() {
        bulletTimeLine.stop();
        isBulletActive = false;
        pane.getChildren().remove(bullet);
    }
    private void removeOpponentBullet() {
        opponentBulletTimeLine.stop();
        isOpponentBulletActive = false;
        pane.getChildren().remove(opponentBullet);
    }

    private void bulletRotate(int currentBulletDirection) {
        switch (currentBulletDirection) {
            case Direction.RIGHT:
                bullet.setRotate(90);
                break;
            case Direction.BOTTOM:
                bullet.setRotate(180);
                break;
            case Direction.LEFT:
                bullet.setRotate(270);
                break;
            default:
                break;
        }
    }

    private boolean isObstaclesIntersect(ImageView player) {
        for (ImageView obstacle : obstacles) {
            if (player.getBoundsInParent().intersects(obstacle.getBoundsInParent()))
                return true;
        }
        return false;
    }

    private boolean isWentAbroad(ImageView player) {
        return (currentDirection == Direction.TOP ? player.getLayoutY() : player.getLayoutY() + 30) >= pane.getHeight() ||
                player.getLayoutY() <= 0 ||
                (currentDirection == Direction.LEFT ? player.getLayoutX() : player.getLayoutX() + 30) >= pane.getWidth() ||
                player.getLayoutX() <= 0;
    }

    public int getCurrentDirection() {
        return currentDirection;
    }

    public int getCurrentBulletDirection() {
        return currentBulletDirection;
    }

    public boolean isBulletActive() {
        return isBulletActive;
    }

    public void setBulletActive(boolean state) {
        isBulletActive = state;
    }

    public Timeline getBulletTimeLine() {
        return bulletTimeLine;
    }

    public void movePlayer2(double x, double y, Integer currentDirection) {
        player2.setLayoutX(x);
        player2.setLayoutY(y);
        switch (currentDirection) {
            case Direction.TOP:
                player2.setRotate(0);
                break;
            case Direction.BOTTOM:
                player2.setRotate(180);
                break;
            case Direction.LEFT:
                player2.setRotate(270);
                break;
            case Direction.RIGHT:
                player2.setRotate(90);
                break;
            default:
                break;
        }
    }

    public void shotPlayer2(double x, double y, int currentDirection) {
        if (!isOpponentBulletActive) {

            InputStream inputStream = getClass().getResourceAsStream("/images/bullet.png");
            Image image = new Image(inputStream);
            opponentBullet = new ImageView(image);

            opponentBullet.setFitHeight(10);
            opponentBullet.setFitWidth(10);
            opponentBullet.setLayoutX(x + opponentBullet.getFitWidth());
            opponentBullet.setLayoutY(y + opponentBullet.getFitHeight());
            pane.getChildren().add(opponentBullet);

            opponentBulletTimeLine = new Timeline(
                    new KeyFrame(
                            Duration.seconds(0.003),
                            animation -> {
                                if (
                                        bullet != null &&
                                                opponentBullet.getBoundsInParent().intersects(bullet.getBoundsInParent())
                                ) {
                                    removeOpponentBullet();
                                    removeBullet();
                                    bullet = null;
                                    opponentBullet = null;
                                    return;
                                }

                                switch (currentDirection) {
                                    case Direction.TOP:
                                        opponentBullet.setRotate(0);
                                        opponentBullet.setLayoutY(opponentBullet.getLayoutY() - 2);
                                        break;
                                    case Direction.BOTTOM:
                                        opponentBullet.setRotate(180);
                                        opponentBullet.setLayoutY(opponentBullet.getLayoutY() + 2);
                                        break;
                                    case Direction.LEFT:
                                        opponentBullet.setRotate(270);
                                        opponentBullet.setLayoutX(opponentBullet.getLayoutX() - 2);
                                        break;
                                    case Direction.RIGHT:
                                        opponentBullet.setRotate(90);
                                        opponentBullet.setLayoutX(opponentBullet.getLayoutX() + 2);
                                        break;
                                    default:
                                        break;
                                }
                                if (
                                        opponentBullet.getLayoutY() >= pane.getHeight() ||
                                                opponentBullet.getLayoutY() <= 0 ||
                                                opponentBullet.getLayoutX() >= pane.getWidth() ||
                                                opponentBullet.getLayoutX() <= 0
                                )
                                    removeOpponentBullet();
                                for (ImageView brick : bricks) {
                                    if (opponentBullet.getBoundsInParent().intersects(brick.getBoundsInParent())) {
                                        pane.getChildren().remove(brick);
                                        bricks.remove(brick);
                                        obstacles.remove(brick);
                                        removeOpponentBullet();
                                        break;
                                    }
                                }
                                for (ImageView concrete : concretes) {
                                    if (opponentBullet.getBoundsInParent().intersects(concrete.getBoundsInParent())) {
                                        removeOpponentBullet();
                                        break;
                                    }
                                }
                                if (opponentBullet.getBoundsInParent().intersects(player1.getBoundsInParent())) {
                                    removeOpponentBullet();
                                    pane.getChildren().remove(player1);
                                    gameOver();
                                }
                            }
                    )
            );

            opponentBulletTimeLine.setCycleCount(1000);
            opponentBulletTimeLine.setOnFinished(event1 -> {
                pane.getChildren().remove(opponentBullet);
                isOpponentBulletActive = false;
            });
            opponentBulletTimeLine.play();
            isOpponentBulletActive = true;
        }
    }

    private void gameOver() {

    }

}