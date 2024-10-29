package automatastarter;

import java.awt.Color;
import java.util.Random;
import java.util.Scanner;
import java.util.ArrayList;
import java.awt.Graphics;

public class PredatorPrey {

    //don't change these constants
    private static final Scanner KEYBOARD = new Scanner(System.in);
    private static final Random RANDOM = new Random();
    private static final int[][] DIRECTIONS = new int[][]{
        {0, 1}, // right
        {1, 0}, // down
        {-1, 0}, // up
        {0, -1}, // left
        // uncomment to enable diagonal movement
        /*
        {1,1},
        {-1,1},
        {-1,-1},
        {1,-1}
        */
    };

    // SETTINGS
    // must be > 0
    private static int WIDTH = 30;  // grid width
    private static int HEIGHT = 30; // grid height

    // initial population sizes
    private static int STARTING_PREYS = 100;
    private static int STARTING_PREDATORS = 10;

    // reproduction
    private static double PREY_REPRODUCTION_CHANCE = 0.05;    // chance for prey to reproduce
    private static double PREDATOR_REPRODUCTION_CHANCE = 0.03; // chance for predators to reproduce

    private static int PREDATOR_DEATH_REQUIREMENT = 20;  // predator dies if it doesn't eat in this many steps
    private static int PREDATOR_MEAL_REQUIREMENT = 10;   // threshold for predators to reproduce

    private static final char PREDATOR = 'W';
    private static final char PREY = 'o';

    // settings
    private static boolean SMART_PREY = true; // prey avoid predators
    private static boolean TOROIDAL_GRID = false; // if true, edges wrap around

    
    // turn this off if you want to advance more than 1000 steps at a time
    private static final boolean LOGS = false;

    public static void main(String[] args) {
        // clear screen
        System.out.println("\n".repeat(20));

        String input = "";
        
        // keep running until user quits
        while (!input.equalsIgnoreCase("q")) {
            int step = 0;
            input = "";
            
            // initialize grid with predators and prey
            int[][] grid = generateGrid();

            // main simulation loop until user resets or quits
            while (!input.equalsIgnoreCase("r") & !input.equalsIgnoreCase("q")) {
                displayGrid(grid, step);
                input = KEYBOARD.nextLine();
                
                // clear screen
                System.out.println("\n".repeat(20));

                int times = 1; // default to advancing 1 step

                // matches a positive integer (for steps)
                try{
                    times = Integer.parseInt(input); // if user inputs a number, set it to number of steps
                }catch(NumberFormatException e){
                }

                for (int i = 0; i < times; i++) { // advance the simulation by the requested number of steps
                    step++;
                    grid = nextGrid(grid); // calculate next state of the grid
                }
            }

        }
    }

    // initialize the grid with the starting population of prey and predators
    
    /**
     * 
     * @return a new randomized board
     */
    public static int[][] generateGrid() {
        int[][] grid = new int[HEIGHT][WIDTH];

        // Place initial populations of prey and predators
        for (int n = 0; n < Math.min(getSTARTING_PREYS() + getSTARTING_PREDATORS(), HEIGHT * WIDTH); n++) {
            int i = RANDOM.nextInt(grid.length); // random row
            int j = RANDOM.nextInt(grid[0].length); // random column

            // make sure the tile is empty before placing an entity
            while (grid[i][j] != 0) {
                i = RANDOM.nextInt(grid.length);
                j = RANDOM.nextInt(grid[0].length);
            }

            // Place preys first, then predators
            if (n < getSTARTING_PREYS()) {
                grid[i][j] = -1; // Prey is represented by a negative number
            } else {
                grid[i][j] = getPREDATOR_DEATH_REQUIREMENT(); // Predator starts with max energy (positive number)
            }
        }
        return grid;
    }

    // calculate the next grid state based on movement and reproduction
    
    public static int[][] nextGrid(int[][] grid) {
        int[][] nextGrid = new int[HEIGHT][WIDTH]; // create a new grid to represent the next state

        // move and reproduce prey first
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] < 0) { // if current tile has prey
                    movePrey(nextGrid, grid, i, j); // handle prey movement
                }
            }
        }

        // move and reproduce predators
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] > 0) { // if current tile has a predator
                    movePredator(nextGrid, grid, i, j); // handle predator movement
                }
            }
        }

        // return the updated grid for the next step
        return nextGrid;
    }

    // move prey and handle reproduction
    private static void movePrey(int[][] nextGrid, int[][] grid, int x, int y) {
        ArrayList<int[]> possibleMoves = new ArrayList<>();

        boolean hasSafeMove = false;

        // check for adjacent empty cells
        for (int[] direction : DIRECTIONS) {
            boolean isSafe = true;
            int dx = direction[0];
            int dy = direction[1];

            if (!isTOROIDAL_GRID() && !isValid(grid, x + dx, y + dy)) {
                // skip out of bounds movement
                continue;
            }
            int newX = (HEIGHT + x + dx) % HEIGHT;
            int newY = (WIDTH + y + dy) % WIDTH;

            if (nextGrid[newX][newY] != 0) {
                continue; // skip already occupied cells
            }
            // if smart prey enabled, check for nearby predators
            if (isSMART_PREY()) {
                for (int[] direction2 : DIRECTIONS) {
                    int dx2 = direction2[0];
                    int dy2 = direction2[1];

                    if (!isTOROIDAL_GRID() && !isValid(grid, newX + dx2, newY + dy2)) {
                        continue; // skip invalid
                    }
                    int predatorX = (HEIGHT + newX + dx2) % HEIGHT; // potential predator X
                    int predatorY = (WIDTH + newY + dy2) % WIDTH;   // potential predator Y
                    if (grid[predatorX][predatorY] > 0) {
                        isSafe = false; // predator nearby
                    }
                }
            }

            // if a safe move is found, clear previous options and prioritize it
            if (isSafe) {
                if (!hasSafeMove) {
                    possibleMoves.clear(); // prioritize safety
                }
                hasSafeMove = true;
                possibleMoves.add(new int[]{newX, newY});
            } else if (!hasSafeMove) {
                possibleMoves.add(new int[]{newX, newY});
            }
        }

        // if prey can't move, it dies (not added to nextGrid)
        if (possibleMoves.isEmpty()) {
            if (LOGS) {
                System.out.println("- prey (squished)");
            }
            return;
        }

        int[] move = possibleMoves.get(RANDOM.nextInt(possibleMoves.size())); // randomly choose move
        int newX = move[0];
        int newY = move[1];

        nextGrid[newX][newY] = grid[x][y]; // move prey to new location

        // handle prey reproduction after moving
        reproduce(nextGrid, newX, newY, getPREY_REPRODUCTION_CHANCE());
    }

    // move predators and handle reproduction
    private static void movePredator(int[][] nextGrid, int[][] grid, int x, int y) {
        ArrayList<int[]> possibleMoves = new ArrayList<>(); // store possible moves
        
        boolean hasAdjacentPrey = false; // track if adjacent prey is found

        // check for adjacent prey or empty cells
        for (int[] direction : DIRECTIONS) {
            int dx = direction[0];
            int dy = direction[1];

            if (!isTOROIDAL_GRID() && !isValid(grid, x + dx, y + dy)) {
                continue; // skip invalid
            }
            int newX = (HEIGHT + x + dx) % HEIGHT;
            int newY = (WIDTH + y + dy) % WIDTH;

            if (nextGrid[newX][newY] < 0) {
                // prey found
                if (!hasAdjacentPrey) {
                    // prioritize moving towards prey
                    possibleMoves.clear();
                }
                hasAdjacentPrey = true;
                possibleMoves.add(new int[]{newX, newY});
            } else if (nextGrid[newX][newY] == 0 && !hasAdjacentPrey) { // empty cell, no adjacent prey
                // add empty cell to possible moves
                possibleMoves.add(new int[]{newX, newY});
            }
        }

        //if the predator cannot move, it stays still
        if (possibleMoves.isEmpty()) {
            nextGrid[x][y] = grid[x][y] - 1;
            if (LOGS && nextGrid[x][y] == 0) {
                System.out.println("- predator (starved)");
            }
            return;
        }

        // Choose a move, either towards prey or an empty cell
        int[] move = possibleMoves.get(RANDOM.nextInt(possibleMoves.size()));
        int newX = move[0];
        int newY = move[1];

        // If the predator moves onto a prey, it eats it
        if (grid[newX][newY] < 0) {
            nextGrid[newX][newY] = getPREDATOR_DEATH_REQUIREMENT(); // predator is set to full health
            if (LOGS) {
                System.out.println("- prey (eaten)");
            }
        } else {
            // gets more starved every step the predator doesnt eat
            nextGrid[newX][newY] = grid[x][y] - 1;
        }

        // If predator runs out of energy, it dies (not added to nextGrid)
        if (nextGrid[newX][newY] == 0) {
            if (LOGS) {
                System.out.println("- predator (starved)");
            }
            return;
        }

        // handle predator reproduction
        reproduce(nextGrid, newX, newY, getPREDATOR_REPRODUCTION_CHANCE());
    }

    private static boolean reproduce(int[][] grid, int x, int y, double reproductionChance) {
        // do not attempt to reproduce if the chance is not hit
        if (RANDOM.nextDouble() > reproductionChance) {
            return false;
        }
        ArrayList<int[]> offspringLocations = new ArrayList<>();

        // check adjacent tiles for empty spaces
        for (int[] direction : DIRECTIONS) {
            int dx = direction[0];
            int dy = direction[1];
            
            if (!isTOROIDAL_GRID() && !isValid(grid, x + dx, y + dy)) {
                continue; // skip invalid
            }
            
            int newX = (HEIGHT + x + direction[0]) % HEIGHT;
            int newY = (WIDTH + y + direction[1]) % WIDTH;
            
            

            if (grid[newX][newY] == 0) { // will only reproduce in empty adjacent cells
                offspringLocations.add(new int[]{newX, newY});
            }
        }

        // Cannot reproduce if there is no space for offspring
        if (offspringLocations.isEmpty()) {
            return false;
        }
        int[] offspringLocation = offspringLocations.get(RANDOM.nextInt(offspringLocations.size()));

        int offspringX = offspringLocation[0];
        int offspringY = offspringLocation[1];

        if (grid[x][y] > (getPREDATOR_DEATH_REQUIREMENT() - getPREDATOR_MEAL_REQUIREMENT())) {
            if (LOGS) {
                System.out.println("+ predator");
            }
            grid[offspringX][offspringY] = getPREDATOR_DEATH_REQUIREMENT();
        } else if (grid[x][y] < 0) {
            if (LOGS) {
                System.out.println("+ prey");
            }
            grid[offspringX][offspringY] = grid[x][y];
        } else {
            return false;
        }
        return true;

    }

    // Display the current state of the grid to the console
    private static void displayGrid(int[][] grid, int step) {
        int numPredators = 0;
        int numPrey = 0;

        int stepLength = String.valueOf(step).length();
        System.out.println("+" + step + "-".repeat(WIDTH >= stepLength ? WIDTH - stepLength : WIDTH) + "+");
        for (int[] row : grid) {
            System.out.print("|");
            for (int value : row) {
                if (value > 0) {
                    numPredators++;
                    //System.out.print((char) ('@' + value));
                    System.out.print(PREDATOR);
                } else if (value < 0) {
                    numPrey++;
                    System.out.print(PREY);
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println("|");
        }
        String predatorString = PREDATOR + "" + numPredators;
        String preyString = numPrey + "" + PREY;
        int strlength = (predatorString + preyString).length();

        System.out.println("+" + (strlength < WIDTH ? (predatorString + "-".repeat(WIDTH - strlength) + preyString) : "-".repeat(WIDTH)) + "+");
        //System.out.println(PREDATOR + ": " + numPredators + " | " + PREY + ": " + numPrey);
        if (numPredators == 0) {
            System.out.print("rip predators (press r to reset)");
        }
    }

    // Display the current state of the grid to graphics and returns the amount of predator and prey
    public static int[] displayGrid(int[][] grid, int step, Graphics g){
        double pixelSize = getPixelSize();
        
        int numPredators = 0;
        int numPrey = 0;
        //displayGrid(grid,step);
        
        //int width = 400 / grid.length;
        //int height = 400 / grid[0].length;
        
        for(int i = 0; i < grid.length; i++){
            for(int j = 0; j < grid[0].length; j++){
                int value = grid[i][j];
                if (value > 0) {
                    numPredators++;
                    g.setColor(new Color(0xA50000));
                    g.fillRect((int)(200+i*pixelSize),(int)(20+j*pixelSize),(int)pixelSize,(int)pixelSize);
                } else if (value < 0) {
                    numPrey++;
                    g.setColor(new Color(0x77DD77));
                    
                    g.fillRect((int)(200+i*pixelSize),(int)(20+j*pixelSize),(int)pixelSize,(int)pixelSize);
                } else {
                    //System.out.print(" ");
                }
            }
        }
        return new int[]{numPredators,numPrey};
    }
    
    public static double getPixelSize(){
        return 300/WIDTH;
    }
    
    public static void setSize(int size){//only allow square grids
        WIDTH = size;
        HEIGHT = size;
    }
    public static int getSize(){
        return WIDTH;
    }
    
    // Check if the cell is valid (within bounds)
    private static boolean isValid(int[][] grid, int x, int y) {
        return x >= 0 && x < grid.length && y >= 0 && y < grid[0].length;
    }

    /**
     * @return the STARTING_PREYS
     */
    public static int getSTARTING_PREYS() {
        return STARTING_PREYS;
    }

    /**
     * @param aSTARTING_PREYS the STARTING_PREYS to set
     */
    public static void setSTARTING_PREYS(int aSTARTING_PREYS) {
        STARTING_PREYS = aSTARTING_PREYS;
    }

    /**
     * @return the STARTING_PREDATORS
     */
    public static int getSTARTING_PREDATORS() {
        return STARTING_PREDATORS;
    }

    /**
     * @param aSTARTING_PREDATORS the STARTING_PREDATORS to set
     */
    public static void setSTARTING_PREDATORS(int aSTARTING_PREDATORS) {
        STARTING_PREDATORS = aSTARTING_PREDATORS;
    }

    /**
     * @return the PREY_REPRODUCTION_CHANCE
     */
    public static double getPREY_REPRODUCTION_CHANCE() {
        return PREY_REPRODUCTION_CHANCE;
    }

    /**
     * @param aPREY_REPRODUCTION_CHANCE the PREY_REPRODUCTION_CHANCE to set
     */
    public static void setPREY_REPRODUCTION_CHANCE(double aPREY_REPRODUCTION_CHANCE) {
        PREY_REPRODUCTION_CHANCE = aPREY_REPRODUCTION_CHANCE;
    }

    /**
     * @return the PREDATOR_REPRODUCTION_CHANCE
     */
    public static double getPREDATOR_REPRODUCTION_CHANCE() {
        return PREDATOR_REPRODUCTION_CHANCE;
    }

    /**
     * @param aPREDATOR_REPRODUCTION_CHANCE the PREDATOR_REPRODUCTION_CHANCE to set
     */
    public static void setPREDATOR_REPRODUCTION_CHANCE(double aPREDATOR_REPRODUCTION_CHANCE) {
        PREDATOR_REPRODUCTION_CHANCE = aPREDATOR_REPRODUCTION_CHANCE;
    }

    /**
     * @return the PREDATOR_DEATH_REQUIREMENT
     */
    public static int getPREDATOR_DEATH_REQUIREMENT() {
        return PREDATOR_DEATH_REQUIREMENT;
    }

    /**
     * @param aPREDATOR_DEATH_REQUIREMENT the PREDATOR_DEATH_REQUIREMENT to set
     */
    public static void setPREDATOR_DEATH_REQUIREMENT(int aPREDATOR_DEATH_REQUIREMENT) {
        PREDATOR_DEATH_REQUIREMENT = aPREDATOR_DEATH_REQUIREMENT;
    }

    /**
     * @return the PREDATOR_MEAL_REQUIREMENT
     */
    public static int getPREDATOR_MEAL_REQUIREMENT() {
        return PREDATOR_MEAL_REQUIREMENT;
    }

    /**
     * @param aPREDATOR_MEAL_REQUIREMENT the PREDATOR_MEAL_REQUIREMENT to set
     */
    public static void setPREDATOR_MEAL_REQUIREMENT(int aPREDATOR_MEAL_REQUIREMENT) {
        PREDATOR_MEAL_REQUIREMENT = aPREDATOR_MEAL_REQUIREMENT;
    }

    /**
     * @return the SMART_PREY
     */
    public static boolean isSMART_PREY() {
        return SMART_PREY;
    }

    /**
     * @param aSMART_PREY the SMART_PREY to set
     */
    public static void setSMART_PREY(boolean aSMART_PREY) {
        SMART_PREY = aSMART_PREY;
    }

    /**
     * @return the TOROIDAL_GRID
     */
    public static boolean isTOROIDAL_GRID() {
        return TOROIDAL_GRID;
    }

    /**
     * @param aTOROIDAL_GRID the TOROIDAL_GRID to set
     */
    public static void setTOROIDAL_GRID(boolean aTOROIDAL_GRID) {
        TOROIDAL_GRID = aTOROIDAL_GRID;
    }
}