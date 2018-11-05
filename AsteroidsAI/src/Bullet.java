import processing.core.PVector;

/**
 * Created by Innos on 10/2/2018.
 */
public class Bullet {
    PVector pos;
    PVector vel;
    float speed = 10;
    boolean off = false;
    int lifespan = 60;
    //------------------------------------------------------------------------------------------------------------------------------------------

    Bullet(float x, float y, float r, float playerSpeed) {

        pos = new PVector(x, y);
        vel = PVector.fromAngle(r);
        vel.mult(speed + playerSpeed);//bullet speed = 10 + the speed of the player
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    //move the bullet
    void move() {
        lifespan --;
        if (lifespan<0) {//if lifespan is up then destroy the bullet
            off = true;
        } else {
            pos.add(vel);
            if (Constants.processing.isOut(pos)) {//wrap bullet
                loopy();
            }
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    //show a dot representing the bullet
    void show() {
        if (!off) {
            Constants.processing.fill(255);
            Constants.processing.ellipse(pos.x, pos.y, 3, 3);
        }
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    //if out moves it to the other side of the screen

    void loopy() {
        if (pos.y < -50) {
            pos.y = Constants.height + 50;
        } else
        if (pos.y > Constants.height + 50) {
            pos.y = -50;
        }
        if (pos.x< -50) {
            pos.x = Constants.width +50;
        } else  if (pos.x > Constants.width + 50) {
            pos.x = -50;
        }
    }
}
