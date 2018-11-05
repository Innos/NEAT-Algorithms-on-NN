import processing.core.PVector;
import processing.data.Table;
import processing.data.TableRow;

import java.util.ArrayList;

import static processing.core.PApplet.constrain;
import static processing.core.PApplet.floor;
import static processing.core.PApplet.CLOSE;

/**
 * Created by Innos on 10/2/2018.
 */
public class Player {
    public PVector position;
    public PVector velocity;
    public PVector acceleration;

    int score; // shot asteroids
    int shootCount; //limits player shots
    float rotation; // ship rotation
    float spin; // ship spin?
    float maxSpeed; //ship speed
    boolean boosting; // ship booster on/off
    ArrayList<Bullet> bullets = new ArrayList<>(); // bullets on screen
    ArrayList<Asteroid> asteroids = new ArrayList<>(); // asteroids on screen
    int asteroidSpawnTimer = 200; // time until next asteroid spawns
    int lives = 0; // player lives
    boolean isDead; // is player dead
    int immortalityTimer; // immortality duration after respawn
    int boostCount; // booster flash?

    //------------- AI

    NeuralNet brain;
    float[] vision = new float[8]; // initial values fed to network?
    float[] decision = new float[4]; //output of the NN
    boolean replay; // is this a replay

    long seedUsed; // since asteroids are spawned randomly need to have a seed for the replay
    ArrayList<Long> asteroidSeeds = new ArrayList<>(); // seeds for the asteroids
    int upToSeedNo = 0; // up to which position in the asteroid seeds we are
    float fitness; // fitness rating for the genetic algorithm

    int shotsFired = 4;
    int shotsHit = 1;
    int lifespan = 0; // for fitness function
    boolean canShoot = true;

    //constructor
    public Player() {
        replay = false;
        position = new PVector(Constants.width / 2, Constants.height / 2);
        velocity = new PVector();
        acceleration = new PVector();
        rotation = 0;
        seedUsed = floor(HelperFunctions.random(1000000000));//create and store a seed
        Constants.processing.randomSeed(seedUsed);

        //generate asteroids
        asteroids.add(new Asteroid(HelperFunctions.random(Constants.width), 0, HelperFunctions.random(-1, 1), HelperFunctions.random(-1, 1), 3));
        asteroids.add(new Asteroid(HelperFunctions.random(Constants.width), 0, HelperFunctions.random(-1, 1), HelperFunctions.random(-1, 1), 3));
        asteroids.add(new Asteroid(0, HelperFunctions.random(Constants.height), HelperFunctions.random(-1, 1), HelperFunctions.random(-1, 1), 3));
        asteroids.add(new Asteroid(
                HelperFunctions.random(Constants.width),
                HelperFunctions.random(Constants.height),
                HelperFunctions.random(-1, 1),
                HelperFunctions.random(-1, 1),
                3));
        this.maxSpeed = 10000;
        //aim the fifth one at the player
        float randX = HelperFunctions.random(Constants.width);
        float randY = -50 + floor(HelperFunctions.random(2)) * (Constants.height + 100);
        asteroids.add(new Asteroid(randX, randY, position.x - randX, position.y - randY, 3));
        brain = new NeuralNet(9, 16, 4);
    }

    // ctor for replays
    public Player(long seed) {
        replay = true;
        position = new PVector(Constants.width / 2, Constants.height / 2);
        velocity = new PVector();
        acceleration = new PVector();
        rotation = 0;

        seedUsed = seed;
        Constants.processing.randomSeed(seedUsed);

        //generate asteroids
        asteroids.add(new Asteroid(HelperFunctions.random(Constants.width), 0, HelperFunctions.random(-1, 1), HelperFunctions.random(-1, 1), 3));
        asteroids.add(new Asteroid(HelperFunctions.random(Constants.width), 0, HelperFunctions.random(-1, 1), HelperFunctions.random(-1, 1), 3));
        asteroids.add(new Asteroid(0, HelperFunctions.random(Constants.height), HelperFunctions.random(-1, 1), HelperFunctions.random(-1, 1), 3));
        asteroids.add(new Asteroid(
                HelperFunctions.random(Constants.width),
                HelperFunctions.random(Constants.height),
                HelperFunctions.random(-1, 1),
                HelperFunctions.random(-1, 1),
                3));

        this.maxSpeed = 10000;
        //aim the fifth one at the player
        float randX = HelperFunctions.random(Constants.width);
        float randY = -50 + floor(HelperFunctions.random(2)) * (Constants.height + 100);
        asteroids.add(new Asteroid(randX, randY, position.x - randX, position.y - randY, 3));
        brain = new NeuralNet(9, 16, 4);
    }

    //Move player
    void move() {
        if (!isDead) {
            processTick();
            rotatePlayer();
            if (boosting) {//are thrusters on
                boost();
            } else {
                boostOff();
            }

            this.velocity.add(this.acceleration);//velocity += acceleration
            //this.velocity.limit(maxSpeed);
            this.velocity.mult(0.99f); //slow player down
            this.position.add(this.velocity);//position += velocity

            for (int i = 0; i < bullets.size(); i++) {//move all the bullets
                bullets.get(i).move();
            }

            for (int i = 0; i < asteroids.size(); i++) {//move all the asteroids
                asteroids.get(i).move();
            }
            if (Constants.processing.isOut(position)) {//wrap the player around the gaming area
                loopy();
            }
        }
    }

    //process tick
    void processTick() {
        lifespan +=1;
        shootCount --;
        asteroidSpawnTimer--;
        if (asteroidSpawnTimer <=0) {//spawn asteroid

            if (replay) {//if replaying use the seeds from the arrayList
                Constants.processing.randomSeed(asteroidSeeds.get(upToSeedNo));
                upToSeedNo++;
            } else {//if not generate the seeds and then save them
                long seed = floor(HelperFunctions.random(1000000));
                asteroidSeeds.add(seed);
                Constants.processing.randomSeed(seed);
            }
            //aim the asteroid at the player to encourage movement
            float randX = HelperFunctions.random(Constants.width);
            float randY = -50 +floor(HelperFunctions.random(2))* (Constants.height+100);
            asteroids.add(new Asteroid(randX, randY, position.x- randX, position.y - randY, 3));
            asteroidSpawnTimer = 200;
        }

        if (shootCount <=0) {
            canShoot = true;
        }
    }

    //booster on
    void boost() {
        this.acceleration = PVector.fromAngle(rotation);
        this.acceleration.setMag(0.1f);
    }


    //booster off
    void boostOff() {
        this.acceleration.setMag(0);
    }

    //spin that player
    void rotatePlayer() {
        rotation += spin;
    }

    //draw the player, bullets and asteroids
    void show() {
        if (!isDead) {
            for (int i = 0; i < bullets.size(); i++) {//show bullets
                bullets.get(i).show();
            }
            if (immortalityTimer >0) {//no need to decrease immortalityCounter if its already 0
                immortalityTimer--;
            }

            if (immortalityTimer >0 && floor(((float)immortalityTimer)/5)%2 ==0) {//needs to appear to be flashing so only show half of the time
            } else {

                Constants.processing.pushMatrix();
                Constants.processing.translate(this.position.x, this.position.y);
                Constants.processing.rotate(rotation);

                //actually draw the player
                Constants.processing.fill(0);
                Constants.processing.noStroke();
                Constants.processing.beginShape();
                int size = 12;

                //black triangle
                Constants.processing.vertex(-size - 2, -size);
                Constants.processing.vertex(-size - 2, size);
                Constants.processing.vertex(2 * size - 2, 0);
                Constants.processing.endShape(CLOSE);
                Constants.processing.stroke(255);

                //white out lines
                Constants.processing.line(-size - 2, -size, -size - 2, size);
                Constants.processing.line(2 * size - 2, 0, -22, 15);
                Constants.processing.line(2 * size - 2, 0, -22, -15);
                if (boosting) {//when boosting draw "flames" its just a little triangle
                    boostCount--;
                    if (floor(((float)boostCount)/3)%2 ==0) {//only show it half of the time to appear like its flashing
                        Constants.processing.line(-size - 2, 6, -size - 2 - 12, 0);
                        Constants.processing.line(-size - 2, -6, -size - 2 - 12, 0);
                    }
                }
                Constants.processing.popMatrix();
            }
        }
        for (int i = 0; i < asteroids.size(); i++) {//show asteroids
            asteroids.get(i).show();
        }
    }

    //shoot a bullet
    void shoot() {
        if (shootCount <=0) {//if can shoot
            bullets.add(new Bullet(position.x, position.y, rotation, velocity.mag()));//create bullet
            shootCount = 30;//reset shoot count
            canShoot = false;
            shotsFired ++;
        }
    }

    //in charge or moving everything and also checking if anything has been shot or hit
    void update() {
        for (int i = 0; i < bullets.size(); i++) {//if any bullets expires remove it
            if (bullets.get(i).off) {
                bullets.remove(i);
                i--;
            }
        }
        move();//move everything
        checkPositions();//check if anything has been shot or hit
    }

    //check if anything has been shot or hit
    void checkPositions() {
        //check if any bullets have hit any asteroids
        for (int i = 0; i < bullets.size(); i++) {
            for (int j = 0; j < asteroids.size(); j++) {
                if (asteroids.get(j).checkIfHit(bullets.get(i).pos)) {
                    shotsHit++;
                    bullets.remove(i);//remove bullet
                    score +=1;
                    break;
                }
            }
        }
        //check if player has been hit
        if (immortalityTimer <=0) {
            for (int j = 0; j < asteroids.size(); j++) {
                if (asteroids.get(j).checkIfHitPlayer(position)) {
                    playerHit();
                }
            }
        }
    }

    //called when player is hit by an asteroid
    void playerHit() {
        if (lives == 0) {//if no lives left
            isDead = true;
        } else {//remove a life and reset positions
            lives -=1;
            immortalityTimer = 100;
            resetPositions();
        }
    }

    //returns player to center
    void resetPositions() {
        position = new PVector(Constants.width/2, Constants.height/2);
        velocity = new PVector();
        acceleration = new PVector();
        bullets = new ArrayList<Bullet>();
        rotation = 0;
    }

    //wraps the player around the playing area
    void loopy() {
        if (position.y < -50) {
            position.y = Constants.height + 50;
        } else
        if (position.y > Constants.height + 50) {
            position.y = -50;
        }
        if (position.x< -50) {
            position.x = Constants.width +50;
        } else  if (position.x > Constants.width + 50) {
            position.x = -50;
        }
    }

    //for genetic algorithm
    void calculateFitness() {
        float hitRate = (float)shotsHit/(float)shotsFired;
        fitness = (score+1)*10;
        fitness *= lifespan;
        fitness *= hitRate;//includes hitrate to encourage aiming
        //Constants.processing.println(fitness);
    }

    void mutate() {
        brain.mutate(Constants.processing.globalMutationRate);
    }

    //returns a clone of this player with the same brian
    Player getClone() {
        Player clone = new Player();
        clone.brain = brain.getClone();
        return clone;
    }

    //returns a clone of this player with the same brian and same random seeds used so all of the asteroids will be in  the same positions
    void saveForReplay(String playerIdentifier) {
        Player clone = new Player(seedUsed);
        clone.brain = brain.getClone();
        clone.asteroidSeeds = (ArrayList)asteroidSeeds.clone();

        //create table
        Table t = new Table();

        //convert the matrices to an array
        float[] whiArr = brain.whi.toArray();
        float[] whhArr = brain.whh.toArray();
        float[] wohArr = brain.woh.toArray();

        //create the amount of columns needed to fit the weights in the biggest layer to layer matrix
        int columnsNeeded = Constants.processing.max(whiArr.length, whhArr.length, wohArr.length);
        for (int i = 0; i< columnsNeeded; i++) {
            t.addColumn();
        }

        t.addColumn("Base seed");

        //create column for base seed and asteroid seeds
        for (int i = 0; i< asteroidSeeds.size(); i++) {
            t.addColumn("Asteroid Seed " + i);
        }

        t.addColumn("Top Score");

        //set the first row as whi
        TableRow tr = t.addRow();

        for (int i = 0; i< whiArr.length; i++) {
            tr.setFloat(i, whiArr[i]);
        }

        // add base seed
        tr.setLong(columnsNeeded, seedUsed);

        for (int i = 0; i < asteroidSeeds.size();i++){
            int col = columnsNeeded + 1 + i;
            tr.setLong(col,asteroidSeeds.get(i));
        }

        int scoreOffset = columnsNeeded + 1 + asteroidSeeds.size();
        tr.setLong(scoreOffset,this.score);

        //set the second row as whh
        tr = t.addRow();

        for (int i = 0; i< whhArr.length; i++) {
            tr.setFloat(i, whhArr[i]);
        }

        //set the third row as woh
        tr = t.addRow();

        for (int i = 0; i< wohArr.length; i++) {
            tr.setFloat(i, wohArr[i]);
        }

        Constants.processing.saveTable(t, "data/replay" + playerIdentifier + ".csv");
    }

    Player loadPlayerFromReplay(String playerIdentifier){
        Table t = Constants.processing.loadTable("data/player" + playerIdentifier + ".csv");

        // load base seed
        int seedIndex = t.getColumnIndex("Base Seed");
        TableRow firstRow = t.getRow(0);
        Player load = new Player(firstRow.getLong(seedIndex));

        // load asteroid seeds
        ArrayList<Long> loadedAsteroidSeeds = new ArrayList<>();
        int topScoreIndex = t.getColumnIndex("Top Score");
        for (int i = seedIndex + 1; i < topScoreIndex; i++){
            loadedAsteroidSeeds.add(firstRow.getLong(i));
        }
        load.asteroidSeeds = loadedAsteroidSeeds;

        //load NN
        load.brain.TableToNet(t);
        return load;
    }

    Player crossover(Player parent2) {
        Player child = new Player();
        child.brain = brain.crossover(parent2.brain);
        return child;
    }

    //looks in 8 directions to find asteroids
    void look() {
        vision = new float[9];
        //look left
        PVector direction;
        for (int i = 0; i< vision.length; i++) {
            direction = PVector.fromAngle(rotation + i*(Constants.processing.PI/4));
            direction.mult(10);
            vision[i] = lookInDirection(direction);
        }

        if (canShoot && vision[0] !=0) {
            vision[8] = 1;
        } else {
            vision[8] =0;
        }
    }

    float lookInDirection(PVector direction) {
        //set up a temp array to hold the values that are going to be passed to the main vision array

        PVector currentPosition = new PVector(position.x, position.y);//the position where we are currently looking for food or tail or wall
        float distance = 0;
        //move once in the desired direction before starting
        currentPosition.add(direction);
        distance +=1;

        //look in the direction until you reach a wall
        while (distance <= 60) {
            for (Asteroid a : asteroids) {
                if (a.lookForHit(currentPosition) ) {
                    //return  1/distance;
                    return (60 - distance) / 60;
                }
            }

            //look further in the direction
            currentPosition.add(direction);

            //loop it
            if (currentPosition.y < -50) {
                currentPosition.y += Constants.height + 100;
            } else
            if (currentPosition.y > Constants.height + 50) {
                currentPosition.y -= Constants.height - 100;
            }
            if (currentPosition.x < -50) {
                currentPosition.x += Constants.width + 100;
            } else  if (currentPosition.x > Constants.width + 50) {
                currentPosition.x -= Constants.width + 100;
            }

            distance +=1;
        }
        return 0;
    }

    //saves the player to a file by converting it to a table
    void savePlayer(String playerIdentifier, int score, int popID) {
        //save the players top score and its population id
        Table playerStats = new Table();
        playerStats.addColumn("Top Score");
        playerStats.addColumn("PopulationID");
        TableRow tr = playerStats.addRow();
        tr.setFloat(0, score);
        tr.setInt(1, popID);

        Constants.processing.saveTable(playerStats, "data/playerStats" + playerIdentifier + ".csv");

        //save players brain
        Constants.processing.saveTable(brain.NetToTable(), "data/player" + playerIdentifier + ".csv");
    }

    //return the player saved in the parameter position
    Player loadPlayer(String playerIdentifier) {

        Player load = new Player();
        Table t = Constants.processing.loadTable("data/player" + playerIdentifier + ".csv");
        load.brain.TableToNet(t);
        return load;
    }

    //convert the output of the neural network to actions
    void think() {
        //get the output of the neural network
        decision = brain.output(vision);

        if (decision[0] > 0.8) {//output 0 is boosting
            boosting = true;
        } else {
            boosting = false;
        }
        if (decision[1] > 0.8) {//output 1 is turn left
            spin = -0.08f;
        } else {//cant turn right and left at the same time
            if (decision[2] > 0.8) {//output 2 is turn right
                spin = 0.08f;
            } else {//if neither then dont turn
                spin = 0;
            }
        }
        //shooting
        if (decision[3] > 0.8) {//output 3 is shooting
            shoot();
        }
    }
}
