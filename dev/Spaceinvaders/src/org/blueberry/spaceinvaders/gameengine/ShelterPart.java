package org.blueberry.spaceinvaders.gameengine;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.blueberry.spaceinvaders.SpaceInvaders;

import java.util.List;

/**
 * ShelterPart-Klasse
 */
public class ShelterPart extends ImageView implements ISprite {

    private int state;
    private int shelterType;
    private int width;
    private int height;
    private List<Image> imageList;

    /**
     * ShelterPart
     * @param imageList
     * @param positionX
     * @param positionY
     * @param type
     * @param rotation
     */
    ShelterPart(List<Image> imageList, int positionX, int positionY, int type, int rotation) {

        this.shelterType = type;
        this.state = type == 1 ? 3 : 2;

        this.imageList = imageList;

        this.setX(positionX);
        this.setY(positionY);
        this.setImage(this.imageList.get(0));
        this.setCache(true);
        this.setPreserveRatio(true);

        this.setFitWidth(Integer.parseInt(SpaceInvaders.getSettings("shelter.part.width")));
        this.width = (int) getFitWidth();
        this.height = (int) this.getLayoutBounds().getHeight();

        if (rotation != 0) {
            this.setRotate(rotation);
        }
    }

    /**
     * getState
     * @return
     */
    public int getState() {
        return this.state;
    }

    /**
     * move
     * @param direction
     */
    @Override
    public void move(InvaderGroup.MoveDirection direction) {

    }

    /**
     * damagedFromTop
     */
    public void damagedFromTop() {
        if (shelterType == 1) {
            switch (state) {
                case 3:
                    this.setImage(imageList.get(3));
                    break;
                case 2:
                    this.setImage(getImage() == imageList.get(3) ? imageList.get(4) : imageList.get(5));
                    break;
            }
        } else if (shelterType == 2 && state == 2) {
            this.setImage(imageList.get(1));
        }
        state--;
    }

    /**
     * damagedFromBottom
     */
    public void damagedFromBottom() {
        if (shelterType == 1) {
            switch (state) {
                case 3:
                    this.setImage(imageList.get(1));
                    break;
                case 2:
                    this.setImage(getImage() == imageList.get(1) ? imageList.get(2) : imageList.get(5));
                    break;
            }
        } else if (shelterType == 2 && state == 2) {
            this.setImage(imageList.get(2));
        }
        state--;
    }
}