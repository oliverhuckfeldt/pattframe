package org.blueberry.spaceinvaders.gameengine;

import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.AudioClip;
import org.blueberry.spaceinvaders.SpaceInvaders;

import java.util.*;

import static org.blueberry.spaceinvaders.gameengine.Game.GameStatus.*;


/**
 * Created by KK on 09.12.2016.
 */
public class Game {

    private static Game ourInstance;

    public static Game getInstance() {
        if(ourInstance == null){
           ourInstance = new Game();
        }
        return ourInstance;
    }



    private Map<String, Image> imageAssets = new HashMap<String, Image>();
    private Map<String, AudioClip> audioAssets = new HashMap<String, AudioClip>();
    private List<Timeline> allActiveTimeLines = new ArrayList<>();
    private InvaderGroup invaderGroup;
    private AnchorPane display;
    private Ship ship;
    private boolean shipSelfMove = Boolean.parseBoolean(SpaceInvaders.getSettings("ship.move.self"));

    private Player player;
    private int currentInvaderBulletsCount = 0;
    private int maxInvaderBulletsCount = Integer.parseInt(SpaceInvaders.getSettings("invader.shoots.parallel"));
    private ObjectProperty<GameStatus> gameStatus = new SimpleObjectProperty<>(PLAY);

    private int invaderMoveDuration = Integer.parseInt(SpaceInvaders.getSettings("invader.move.speed.1"));

    private long invaderShootDelayMin = Long.parseLong(SpaceInvaders.getSettings("invader.shoots.delay.random.min"));
    private long invaderShootDelayMax = Long.parseLong(SpaceInvaders.getSettings("invader.shoots.delay.random.max"));

    private GameAnimationTimer gameAnimationTimer = new GameAnimationTimer();

    private Label gameStatusLabel = new Label(); //TODO: wieder entfernen nur temporär



    public void loadAssets(String theme){

        imageAssets.put("invader1a", new Image(theme + "/graphics/invader1a.png"));
        imageAssets.put("invader1b", new Image(theme + "/graphics/invader1b.png"));
        imageAssets.put("invader2a", new Image(theme + "/graphics/invader2a.png"));
        imageAssets.put("invader2b", new Image(theme + "/graphics/invader2b.png"));
        imageAssets.put("invader3a", new Image(theme + "/graphics/invader3a.png"));
        imageAssets.put("invader3b", new Image(theme + "/graphics/invader3b.png"));

        imageAssets.put("invaderBullet", new Image(theme + "/graphics/invader_bullet.png"));

        imageAssets.put("shipBullet", new Image(theme + "/graphics/ship_bullet.png"));
        imageAssets.put("ship", new Image(theme + "/graphics/ship.png"));

        imageAssets.put("bunker1a", new Image(theme + "/graphics/bunker/8x8/1a.png"));
        imageAssets.put("bunker2a", new Image(theme + "/graphics/bunker/8x8/2a.png"));


        audioAssets.put("shipShoot", new AudioClip(getClass().getResource("/" + theme + "/sounds/ship_shoot.wav").toExternalForm()));
        audioAssets.put("shipExplosion", new AudioClip(getClass().getResource("/" + theme + "/sounds/ship_explosion.wav").toExternalForm()));
        audioAssets.put("invaderKilled", new AudioClip(getClass().getResource("/" + theme + "/sounds/invader_killed.wav").toExternalForm()));


    }

    public void addInvadersToPane(AnchorPane anchorPane, List<Invader> invaderList){

        for (Invader invader: invaderList){
            anchorPane.getChildren().add(invader);
        }
    }

    public Image getImageAsset(String key){
        return imageAssets.get(key);
    }

    public AudioClip getAudioAsset(String key){
        return audioAssets.get(key);
    }

    private Game(){
        loadAssets(SpaceInvaders.getSettings("game.standardtheme"));
        player = new Player();


    }

    public void constructGame(AnchorPane pane){
        this.display = pane;
        createInvaderGroup();
        addInvadersToPane(display, invaderGroup.getInvaderList());
        ship = new Ship(getImageAsset("ship"));
        display.getChildren().add(ship);


        gameStatusLabel.textProperty().bind(gameStatus.asString()); //TODO: raus damit
        display.getChildren().add(gameStatusLabel); //TODO: raus damit

        display.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                switch (event.getCode()) {
                    case LEFT:
                        ship.setMoveDirection(InvaderGroup.MoveDirection.LEFT);
                        break;
                    case RIGHT:
                        ship.setMoveDirection(InvaderGroup.MoveDirection.RIGHT);
                        break;
                    case X:
                        tryShipShoot();
                        break;
                    case SPACE:
                        tryShipShoot();
                        break;
                    case P:
                        gameStatus.set(gameStatus.get() == PLAY ? PAUSE : PLAY);
                        break;
                }

            }
        });

        display.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (shipSelfMove){return;}
                if(event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT){
                    ship.setMoveDirection(InvaderGroup.MoveDirection.NONE);
                }
            }
        });

        gameStatus.addListener((observable, oldValue, newValue) -> {
            switch (newValue){
                case PLAY:
                    playActiveTimeLines(allActiveTimeLines);
                    break;
                case PAUSE:
                    pauseActiveTimeLines(allActiveTimeLines);
                    break;
                case GAMEOVER:
                    stop();
                    SpaceInvaders.setScreen("HighscoreView"); //TODO: Übergang so mit GAMEOVER und dann in den Screen
                    break;
                case WON:
                    Label finshLabel2 = new Label("Du hast dieses Spiel gewonnen");
                    finshLabel2.setLayoutX(100);
                    finshLabel2.setLayoutY(500);
                    display.getChildren().add(finshLabel2);
                    break;
            }
        });

        player.livesProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Player Lives Changed: " + newValue);
            if (newValue.intValue() == 0){
                gameStatus.set(GAMEOVER);
            }
        });
    }

    private void pauseActiveTimeLines(List<Timeline> timeLines){
        timeLines.forEach(Timeline::pause);
        System.out.println("Anzahl aktiver TimeLines: " + timeLines.size());
    }

    private void playActiveTimeLines(List<Timeline> timeLines){
        timeLines.forEach(Timeline::play);
    }

    public List<Timeline> getAllActiveTimeLines(){
        return this.allActiveTimeLines;
    }

    private void removeBullet(IGunSprite sprite){
        System.out.println("RemoveBulletAnfang Anzahl aktiver TimeLines: " + allActiveTimeLines.size());
        sprite.getBullet().getTimeLine().stop();
        allActiveTimeLines.remove(sprite.getBullet().getTimeLine());
        display.getChildren().remove(sprite.getBullet());
        sprite.removeBullet();
        System.out.println("RemoveBulletEndeAnzahl aktiver TimeLines: " + allActiveTimeLines.size());
    }

    private void removeInvader(Invader invader){
        display.getChildren().remove(invader);
        invaderGroup.removeInvader(invader);
    }

    private void tryShipShoot(){

        if (ship.getBullet() == null && gameStatus.get() == PLAY){
            ship.newBullet();
            ship.getBullet().getTimeLine().setOnFinished(event -> {
                removeBullet(ship);
                System.out.println("Schussanimation fertig");
            });

            display.getChildren().add(ship.getBullet());
            ship.shoot();
        }
    }

    private void tryInvaderShoot(){

        if (currentInvaderBulletsCount < maxInvaderBulletsCount && currentInvaderBulletsCount < invaderGroup.getInvaderList().size()){

            Random random = new Random();
            int randomInt = random.nextInt(invaderGroup.getInvaderList().size());

            System.out.println("RandomInt: " + randomInt);

            // einen Invader bekommen, der momentan nicht schießt
            Invader tempInvader = invaderGroup.getInvaderList().get(randomInt);
            while (tempInvader.getBullet() != null){
                randomInt = random.nextInt(invaderGroup.getInvaderList().size());
                tempInvader = invaderGroup.getInvaderList().get(randomInt);
            }

            currentInvaderBulletsCount++;
            final Invader shootInvader = tempInvader;

            shootInvader.newBullet();
            shootInvader.getBullet().getTimeLine().setOnFinished(event -> {
                removeBullet(shootInvader);
                currentInvaderBulletsCount--;
                System.out.println("Invader Schussanimation fertig");
            });

            display.getChildren().add(shootInvader.getBullet());
            shootInvader.shoot();
        }

    }


    public void createInvaderGroup(){
        invaderGroup = InvaderGroup.getInstance();
        invaderGroup.createGroup(Integer.parseInt(SpaceInvaders.getSettings("invadergroup.position.x")), Integer.parseInt(SpaceInvaders.getSettings("invadergroup.position.y")));
    }


    public InvaderGroup getInvaderGroup(){
        return invaderGroup;
    }

    public void setTheme(String theme){
        loadAssets(theme);
        invaderGroup.createGroup(Integer.parseInt(SpaceInvaders.getSettings("invadergroup.position.x")), Integer.parseInt(SpaceInvaders.getSettings("invadergroup.position.y")));
    }

    public void play(){

//        GameAnimationTimer gameAnimationTimer = new GameAnimationTimer();
        gameAnimationTimer.start();

    }

    private Invader detectCollisionedInvader(Bullet bullet, List<Invader> invaders){
        for(Invader invader : invaders){
            if(bullet.intersects(invader.getLayoutBounds())){
                return invader;
            }
        }
        return null;
    }


    public class GameAnimationTimer extends AnimationTimer {

        long invaderMoveLastTime = System.nanoTime();
        long invaderShootLastTime = System.nanoTime();
        Random random = new Random();


        @Override
        public void handle(long now) {

            if (gameStatus.get() == PLAY) {

                // ship bewegen
                ship.move(ship.getMoveDirection());

                //InvaderGroup bewegen (Zeitinterval application.properties: invader.move.speed.1)
                if (now > invaderMoveLastTime + invaderMoveDuration * 1000000) {
                    invaderMoveLastTime = now;
                    invaderGroup.move();
                }


                //Invaderschuss absetzen
                if (now > invaderShootLastTime + ((long) (random.nextDouble()*invaderShootDelayMax) + invaderShootDelayMin) * 1000000L) {
                    invaderShootLastTime = now;
                    tryInvaderShoot();
                }

                // hat die schiffskanone einen invader getroffen
                if(ship.getBullet() != null){
                    Invader collisionedInvader = detectCollisionedInvader(ship.getBullet(), invaderGroup.getInvaderList());
                    if(collisionedInvader != null){
                        System.out.println("Invader getroffen");
                        getAudioAsset("invaderKilled").play();
                        removeBullet(ship);

                        player.setScore(player.getScore() + collisionedInvader.getValue());
                        removeInvader(collisionedInvader);
                    }
                    if(invaderGroup.getInvaderList().size() == 0){
                        gameStatus.set(WON);
                    }
                }


                // hat ein Invader das ship getroffen
                for (Invader invader: invaderGroup.getInvaderList()){
                    if (invader.getBullet() != null){
                        if (ship.intersects(invader.getBullet().getLayoutBounds())){
                            System.out.println("Ship getroffen");
                            getAudioAsset("shipExplosion").play();
                            player.setlives(player.getlives() - 1);
                            removeBullet(invader);
                            currentInvaderBulletsCount--;
                            break;
                        }
                    }
                }










            }
        }
    }


    public void stop(){
        allActiveTimeLines.forEach(Timeline::stop);
        this.gameAnimationTimer.stop();
    }

    public static void reset(){
        ourInstance = null;
    }


    public final GameStatus getGameStatus() {
        return gameStatus.get();
    }

    public final void setGameStatus(GameStatus status) {
        gameStatusProperty().set(status);
    }

    public final ObjectProperty<GameStatus> gameStatusProperty() {
        return gameStatus;
    }

    public Player getPlayer(){
        return player;
    }


    public enum GameStatus{
        PLAY,
        PAUSE,
        WON,
        GAMEOVER;
    }


}