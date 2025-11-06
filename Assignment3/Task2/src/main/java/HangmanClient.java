import org.json.JSONArray;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * Hangman Game Client - Student Starter Code (Implemented Core Features)
 *
 * Your task: Implement the protocol communication for all game features.
 *
 * What's provided:
 * - Complete menu structure with different game states
 * - Name handling as a complete example
 * - Method stubs for all features
 *
 * What you need to implement:
 * - Protocol requests/responses for all game operations
 * - Proper response handling and display
 */
public class HangmanClient {
    static Socket sock;
    static ObjectOutputStream oos;
    static ObjectInputStream in;

    static Scanner scanner = new Scanner(System.in);
    static boolean gameInProgress = false;
    static boolean hasName = false;
    static String playerName = "";
    
    // Client-side cached state for display
    static String currentMask = "";
    static String currentHangmanArt = "";
    static int wrongGuesses = 0;
    static String guessedLettersDisplay = "";
    static String lastMessage = "";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            sock = new Socket(host, port);
            oos = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            System.out.println("╔════════════════════════════════════════╗");
            System.out.println("║         WELCOME TO HANGMAN GAME!         ║");
            System.out.println("╚════════════════════════════════════════╝");
            System.out.println();

            boolean running = true;
            while (running) {
                if (!hasName) {
                    running = showInitialMenu();
                } else if (!gameInProgress) {
                    running = showMainMenu();
                } else {
                    // Update and display state before showing game menu
                    getState(false);
                    running = showGameMenu();
                }
                System.out.println();
            }

            overandout();
        } catch (Exception e) {
            System.out.println("\n--- Connection Error ---");
            System.out.println("Could not connect to server or connection lost: " + e.getMessage());
            //e.printStackTrace();
        }
    }

    static boolean showInitialMenu() {
        System.out.println("────────────────────────────────────────");
        System.out.println("  1. Set Your Name");
        System.out.println("  2. Quit");
        System.out.println("────────────────────────────────────────");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                setName();
                return true;
            case "2":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    static boolean showMainMenu() {
        System.out.println("────────────────────────────────────────");
        System.out.println("MAIN MENU (Player: " + playerName + "):");
        System.out.println("  1. Start New Game");
        System.out.println("  2. View Leaderboard");
        System.out.println("  3. Quit");
        System.out.println("────────────────────────────────────────");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                startGame();
                return true;
            case "2":
                getLeaderboard();
                return true;
            case "3":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    static boolean showGameMenu() {
        System.out.println("\n────────────────────────────────────────");
        System.out.println(">> " + lastMessage);
        System.out.println("────────────────────────────────────────");
        System.out.println(currentHangmanArt);
        System.out.println("Word: " + currentMask);
        System.out.println("Wrong Guesses: " + wrongGuesses + "/9");
        System.out.println("Guessed Letters: " + guessedLettersDisplay);
        System.out.println("────────────────────────────────────────");
        System.out.println("Type a letter or word to guess");
        System.out.println("Or choose:");
        System.out.println("  1 - Show game state");
        System.out.println("  2 - See guessed letters");
        System.out.println("  3 - Give up (return to main menu)");
        System.out.println("  0 - Quit game");
        System.out.println("────────────────────────────────────────");
        System.out.print("Your input: ");
        String input = scanner.nextLine().trim();

        // Handle special commands
        if (input.equals("1")) {
            getState(true);
            return true;
        } else if (input.equals("2")) {
            getLetters();
            return true;
        } else if (input.equals("3")) {
            giveUp();
            return true;
        } else if (input.equals("0")) {
            quit();
            return false;
        }

        if (input.isEmpty()) {
            System.out.println("Please enter a letter, word, or command.");
            return true;
        }

        // Single character = letter guess, multiple = word guess
        if (input.length() == 1) {
            guessLetter(input);
        } else {
            guessWord(input);
        }

        return true;
    }

    // --- GAME FEATURE IMPLEMENTATIONS ---

    static void giveUp() {
        System.out.print("\nAre you sure you want to give up? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            
            JSONObject request = new JSONObject();
            request.put("type", "give_up");
            
            JSONObject response = sendRequest(request);
            if (response != null && response.getBoolean("ok")) {
                lastMessage = response.getString("message");
                String finalWord = response.getString("final_word");
                System.out.println("The word was: **" + finalWord + "**");
                gameInProgress = false;
                System.out.println("\n" + lastMessage + " Returning to main menu...\n");
            } else if (response != null) {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        } else {
            System.out.println("\nContinuing game...");
        }
    }

    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     */
    static void setName() {
        System.out.print("\nEnter your name: ");
        String name = scanner.nextLine().trim();

        // Create request according to protocol design.
        JSONObject request = new JSONObject();
        request.put("type", "name");
        request.put("name", name);

        // Send request and get response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                hasName = true;
                playerName = name;
                System.out.println("\n" + response.getString("message"));
                System.out.println();
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Start game implementation
     **/
    static void startGame() {
        System.out.println("\nSelect difficulty:");
        System.out.println("  1. Easy (long words - more clues)");
        System.out.println("  2. Medium (moderate length words)");
        System.out.println("  3. Hard (short words - fewer clues)");
        System.out.print("Enter choice (1-3): ");
        String diffChoice = scanner.nextLine().trim();

        String difficulty = "easy";
        switch (diffChoice) {
            case "1":
                difficulty = "easy";
                break;
            case "2":
                difficulty = "medium";
                break;
            case "3":
                difficulty = "hard";
                break;
            default:
                System.out.println("Invalid choice, defaulting to easy");
        }

        // Create request.
        JSONObject request = new JSONObject();
        request.put("type", "start");
        request.put("difficulty", difficulty);

        // Send request, handle response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                gameInProgress = true;
                updateClientState(response);
                System.out.println("✔ Game started on **" + difficulty.toUpperCase() + "** difficulty!");
            } else {
                System.out.println("✗ Error starting game: " + response.getString("message"));
            }
        }
    }

    /**
     * Implement letter guess
     */
    static void guessLetter(String letter) {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }
        
        // Create request.
        JSONObject request = new JSONObject();
        request.put("type", "guess_letter");
        request.put("letter", letter.trim().toLowerCase());

        // Send request, handle response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                updateClientState(response);
                checkGameOver(response);
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Implement word guess
     */
    static void guessWord(String word) {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }

        // Create request.
        JSONObject request = new JSONObject();
        request.put("type", "guess_word");
        request.put("word", word.trim().toLowerCase());

        // Send request, handle response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                updateClientState(response);
                checkGameOver(response);
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Implement get game state
     * @param verbose If true, print a confirmation message.
     */
    static void getState(boolean verbose) {
        if (!gameInProgress) {
            if (verbose) System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }

        // Create request.
        JSONObject request = new JSONObject();
        request.put("type", "get_state");

        // Send request, handle response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                updateClientState(response);
                if (verbose) System.out.println("✔ Game state refreshed.");
            } else if (verbose) {
                System.out.println("✗ Error getting state: " + response.getString("message"));
            }
        }
    }

    /**
     * Implement get guessed letters
     */
    static void getLetters() {
        if (!gameInProgress) {
            System.out.println("✗ No game in progress. Start a new game first.");
            return;
        }

        // Create request.
        JSONObject request = new JSONObject();
        request.put("type", "get_letters");

        // Send request, handle response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                JSONArray letters = response.getJSONArray("guessed_letters");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < letters.length(); i++) {
                    sb.append(letters.getString(i).toUpperCase());
                    if (i < letters.length() - 1) sb.append(", ");
                }
                System.out.println("\nGuessed Letters: [" + sb.toString() + "]");
            } else {
                System.out.println("✗ Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Implement get leaderboard
     */
    static void getLeaderboard() {
        // Create request.
        JSONObject request = new JSONObject();
        request.put("type", "leaderboard");

        // Send request, handle response.
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                displayLeaderboard(response.getJSONArray("leaderboard"));
            } else {
                System.out.println("✗ Error retrieving leaderboard: " + response.getString("message"));
            }
        }
    }
    
    // Game state and display helpers.
    /**
     * Updates the client's cached game state variables from a server response.
     */
    static void updateClientState(JSONObject response) {
        if (response.has("mask")) {
            currentMask = response.getString("mask");
        }
        if (response.has("hangman_art")) {
            currentHangmanArt = response.getString("hangman_art");
        }
        if (response.has("wrong_guesses")) {
            wrongGuesses = response.getInt("wrong_guesses");
        }
        if (response.has("message")) {
            lastMessage = response.getString("message");
        }
        if (response.has("guessed_letters")) {
            JSONArray letters = response.getJSONArray("guessed_letters");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < letters.length(); i++) {
                sb.append(letters.getString(i).toUpperCase());
                if (i < letters.length() - 1) sb.append(" ");
            }
            guessedLettersDisplay = sb.toString();
        }
    }
    
    /**
     * Checks the response for game end conditions (win/loss).
     */
    static void checkGameOver(JSONObject response) {
        if (response.has("game_status")) {
            String status = response.getString("game_status");
            if (status.equals("win") || status.equals("loss")) {
                gameInProgress = false;
                String finalWord = response.getString("final_word");
                int finalScore = response.has("score") ? response.getInt("score") : 0;
                
                System.out.println("\n****************************************");
                System.out.println(currentHangmanArt);
                
                if (status.equals("win")) {
                    System.out.println("CONGRATULATIONS! YOU WIN!");
                    System.out.println("The word was: **" + finalWord + "**");
                    System.out.println("Final Score: **" + finalScore + "**");
                } else {
                    System.out.println("GAME OVER! YOU LOSE!");
                    System.out.println("The word was: **" + finalWord + "**");
                    System.out.println("Final Score: **" + finalScore + "** (0 points)");
                }
                System.out.println("****************************************\n");
            }
        }
    }

    /**
     * Formats and displays the leaderboard.
     */
    static void displayLeaderboard(JSONArray leaderboard) {
        System.out.println("\n╔═════════════════════════════════════╗");
        System.out.println("║            HANGMAN LEADERBOARD            ║");
        System.out.println("╠═════════════════════════════════════╣");
        System.out.printf("║ %-3s | %-15s | %-8s | %-5s ║\n", "RANK", "NAME", "DIFFICULTY", "SCORE");
        System.out.println("╠═════════════════════════════════════╣");

        if (leaderboard.length() == 0) {
            System.out.printf("║ %-35s     ║\n", "No scores recorded yet.");
        } else {
            for (int i = 0; i < leaderboard.length(); i++) {
                JSONObject entry = leaderboard.getJSONObject(i);
                System.out.printf("║ %-4s| %-15s | %-8s | %-5d ║\n",
                                i + 1 + ".",
                                entry.getString("name"),
                                entry.getString("difficulty"),
                                entry.getInt("score"));
            }
        }
        System.out.println("╚═════════════════════════════════════╝\n");
    }

    /**
     * Quit game
     */
    static boolean quit() {
        JSONObject request = new JSONObject();
        request.put("type", "quit");

        JSONObject response = sendRequest(request);
        if (response != null && response.getBoolean("ok")) {
            System.out.println("\n" + response.getString("message"));
            System.out.println("Thanks for playing!");
        } else if (response == null) {
            System.out.println("\nConnection to server was lost. Exiting.");
        }
        // stops main loop.
        return false;
    }

    /**
     * Helper: Send request and receive response.
     */
    static JSONObject sendRequest(JSONObject request) {
        try {
            String req = request.toString();
            oos.writeObject(req);
            oos.flush();

            String res = (String) in.readObject();
            return new JSONObject(res);
        } catch (SocketException | EOFException e) {
            System.out.println("\n--- Communication Error ---");
            System.out.println("Server connection closed or unexpected end of stream.");
            return null;
        } catch (Exception e) {
            System.out.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close connection.
     */
    static void overandout() {
        try {
            if (oos != null) oos.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
        }
    }
}
