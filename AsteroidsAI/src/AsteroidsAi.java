import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;

import java.io.File;


public class AsteroidsAi extends PApplet {

    Player humanPlayer;//the player which the user (you) controls
    Population pop;
    int speed = 100;
    float globalMutationRate = 0.1f;

    PFont font;
    boolean showBest = true;
    boolean runBest = false;
    boolean humanPlaying = false;

    public void settings(){
        Constants.initiate(this);
        size(Constants.width, Constants.height, P2D);

        humanPlayer = new Player();
        pop = new Population(200);// create new population of size 200
        //frameRate(speed);
        font = loadFont("AgencyFB-Reg-48.vlw");
//        size(width, height);
//        pop = new Population(200);// create new population of size 200
//        frameRate(speed);
//        font = loadFont("AgencyFB-Reg-48.vlw");
    }

    public void setup(){
        frameRate(speed);
    }

    public void draw() {
        background(0); //deep space background
        if (humanPlaying) {//if the user is controlling the ship[
            if (!humanPlayer.isDead) {//if the player isn't dead then move and show the player based on input
                humanPlayer.update();
                humanPlayer.show();
            } else {//once done return to ai
                humanPlaying = false;
            }
        } else
        if (runBest) {// if replaying the best ever game
            if (!pop.bestPlayer.isDead) {//if best player is not dead
                pop.bestPlayer.look();
                pop.bestPlayer.think();
                pop.bestPlayer.update();
                pop.bestPlayer.show();
            } else {//once dead
                runBest = false;//stop replaying it
                //pop.bestPlayer = pop.bestPlayer.cloneForReplay();//reset the best player so it can play again
            }
        } else {//if just evolving normally
            if (!pop.done()) {//if any players are alive then update them
                pop.updateAlive();
            } else {//all dead
                //genetic algorithm
                pop.calculateFitness();
                pop.naturalSelection();
            }
        }
        showScore();//display the score
    }

    @Override
    public void keyPressed() {
        switch(key) {
            case ' ':
                if (humanPlaying) {//if the user is controlling a ship shoot
                    humanPlayer.shoot();
                } else {//if not toggle showBest
                    showBest = !showBest;
                }
                break;
            case 'p'://play
                humanPlaying = !humanPlaying;
                humanPlayer = new Player();
                break;
            case '+'://speed up frame rate
                speed += 10;
                frameRate(speed);
                println(speed);

                break;
            case '-'://slow down frame rate
                if (speed > 10) {
                    speed -= 10;
                    frameRate(speed);
                    println(speed);
                }
                break;
            case 'h'://halve the mutation rate
                globalMutationRate /=2;
                println(globalMutationRate);
                break;
            case 'd'://double the mutation rate
                globalMutationRate *= 2;
                println(globalMutationRate);
                break;
            case 'b'://run the best
                Player replayPlayer = pop.players[0].loadPlayerFromReplay("");
                if(replayPlayer != null){
                    pop.bestPlayer = replayPlayer;
                    runBest = true;
                }
                break;
        }

        //player controls
        if (key == CODED) {
            if (keyCode == UP) {
                humanPlayer.boosting = true;
            }
            if (keyCode == LEFT) {
                humanPlayer.spin = -0.08f;
            } else if (keyCode == RIGHT) {
                humanPlayer.spin = 0.08f;
            }
        }
    }

    @Override
    public void keyReleased() {
        //once key released
        if (key == CODED) {
            if (keyCode == UP) {//stop boosting
                humanPlayer.boosting = false;
            }
            if (keyCode == LEFT) {// stop turning
                humanPlayer.spin = 0;
            } else if (keyCode == RIGHT) {
                humanPlayer.spin = 0;
            }
        }
    }

    //function which returns whether a vector is out of the play area
    boolean isOut(PVector pos) {
        if (pos.x < -50 || pos.y < -50 || pos.x > Constants.width + 50 || pos.y > 50 + Constants.height) {
            return true;
        }
        return false;
    }

    //shows the score and the generation on the screen
    void showScore() {
        if (humanPlaying) {
            Constants.processing.textFont(font);
            Constants.processing.fill(255);
            Constants.processing.textAlign(LEFT);
            Constants.processing.text("Score: " + humanPlayer.score, 80, 60);
        } else if (runBest) {
            Constants.processing.textFont(font);
            Constants.processing.fill(255);
            Constants.processing.textAlign(LEFT);
            Constants.processing.text("Score: " + pop.bestPlayer.score, 80, 60);
            Constants.processing.text("Gen: " + pop.gen, width - 200, 60);
        } else {
            if (showBest) {
                Constants.processing.textFont(font);
                Constants.processing.fill(255);
                Constants.processing.textAlign(LEFT);
                Constants.processing.text("Score: " + pop.players[0].score, 80, 60);
                Constants.processing.text("Gen: " + pop.gen, width - 200, 60);
//                if(!pop.players[0].isDead)
//                {
//                    Constants.processing.println(pop.players[0].vision);
//                    Constants.processing.println(pop.players[0].position, pop.players[0].velocity,pop.players[0].acceleration, pop.players[0].boosting);
//                }
            }
        }
    }

    public static void main(String... args){
        PApplet.main("AsteroidsAi");
    }
}
