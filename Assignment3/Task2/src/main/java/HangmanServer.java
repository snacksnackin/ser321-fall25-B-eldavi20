import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Hangman Game Server - Student Starter Code (Implemented Core Features)
 *
 * Implemented:
 * - Start New Game (with basic word selection)
 * - Guess Letter (validation, state update)
 * - Get Game State
 * - Win/Lose Detection
 */
public class HangmanServer {
    static Socket sock;
    static ObjectOutputStream os;
    static ObjectInputStream in;
    static int port = 8888;

    // Game state for current player
    static String playerName = null;
    static String currentWord = null;
    static String difficulty = null;
    static Set<Character> guessedLetters = new TreeSet<>(); // Use TreeSet for sorted display
    static int wrongGuesses = 0;
    static int score = 0;
    static boolean gameActive = false;

    // Leaderboard - list of game results
    static List<Map<String, Object>> leaderboard = new ArrayList<>();

    // Hangman ASCII art - 10 stages (0-9 wrong guesses)
    // Loaded from resources/hangman_stages.txt
    static String[] HANGMAN_STAGES = new String[10];

    // Word lists by difficulty - loaded from resource files
    static String[] EASY_WORDS;
    static String[] MEDIUM_WORDS;
    static String[] HARD_WORDS;
    
    // Max wrong guesses (based on 10 hangman stages, 0 is initial)
    static final int MAX_WRONG_GUESSES = HANGMAN_STAGES.length - 1;

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Expected arguments: <port(int)>");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("Port must be an integer");
            System.exit(2);
        }

        // Load game resources
        loadHangmanStages();
        loadWords();

        try {
            ServerSocket serv = new ServerSocket(port);
            System.out.println("Hangman Server ready for connections on port " + port);

            while (true) {
                System.out.println("Server waiting for a connection");
                sock = serv.accept();
                System.out.println("Client connected");

                // Setup streams.
                in = new ObjectInputStream(sock.getInputStream());
                OutputStream out = sock.getOutputStream();
                os = new ObjectOutputStream(out);

                // Reset game state for new connection.
                resetGame();

                boolean connected = true;
                while (connected) {
                    String s = "";
                    try {
                        s = (String) in.readObject();
                    } catch (Exception e) {
                        System.out.println("Client disconnect");
                        connected = false;
                        continue;
                    }

                    JSONObject res = isValid(s);
                    // invalid JSON.
                    if (res.has("ok") && !res.getBoolean("ok")) {
                        writeOut(res);
                        continue;
                    }
                    
                    JSONObject req = new JSONObject(s);
                    res = testField(req, "type");
                    if (!res.getBoolean("ok")) {
                        res = noType(req);
                        writeOut(res);
                        continue;
                    }

                    // Route to appropriate handler.
                    String type = req.getString("type");
                    if (type.equals("name")) {
                        res = handleName(req);
                    } else if (type.equals("start")) {
                        res = handleStartGame(req);
                    } else if (type.equals("guess_letter")) {
                        res = handleGuessLetter(req);
                    } else if (type.equals("get_state")) {
                        res = handleGetState(req);
                    } else if (type.equals("guess_word")) {
                        res = handleGuessWord(req);
                    } else if (type.equals("get_letters")) {
                        res = handleGetLetters(req);
                    } else if (type.equals("give_up")) {
                        res = handleGiveUp(req);
                    } else if (type.equals("leaderboard")) {
                        res = handleGetLeaderboard(req);
                    } else if (type.equals("quit")) {
                        res = handleQuit(req);
                        writeOut(res);
                        connected = false;
                        continue;
                    } else {
                        res = wrongType(req);
                    }
                    writeOut(res);
                }
                overandout();
            }
        } catch (Exception e) {
            e.printStackTrace();
            overandout();
        }
    }

    // Game logic handlers.
    static JSONObject handleStartGame(JSONObject req) {
        System.out.println("Start Game request: " + req.toString());
        JSONObject res = new JSONObject();

        if (playerName == null) {
            return errorResponse("Player name not set. Set your name first.");
        }
        
        // Validate difficulty.
        res = testField(req, "difficulty");
        if (!res.getBoolean("ok")) return res;
        
        difficulty = req.getString("difficulty").toLowerCase();
        String[] wordList;
        
        switch(difficulty) {
            case "easy":
                wordList = EASY_WORDS;
                break;
            case "medium":
                wordList = MEDIUM_WORDS;
                break;
            case "hard":
                wordList = HARD_WORDS;
                break;
            default:
                return errorResponse("Invalid difficulty: " + difficulty + ". Must be easy, medium, or hard.");
        }

        // Select word, reset state.
        if (wordList.length == 0) {
            return errorResponse("Server error: Word list for " + difficulty + " is empty.");
        }
        
        Random rand = new Random();
        currentWord = wordList[rand.nextInt(wordList.length)];
        
        currentWord = currentWord.toLowerCase();
        
        // Reset/init game state.
        guessedLetters.clear();
        wrongGuesses = 0;
        score = 0;
        gameActive = true;
        
        System.out.println("Game started. Difficulty: " + difficulty + ", Word: " + currentWord);

        // Success response.
        res = successResponse("start", "Game started!");
        res.put("mask", getMaskedWord());
        res.put("wrong_guesses", wrongGuesses);
        res.put("hangman_art", getHangmanArt());
        
        // Automatically guess the first three most common letters if difficulty is "easy".
        if (difficulty.equals("easy")) {
             String preGuessed = autoGuessEasy();
             if (!preGuessed.isEmpty()) {
                 res.put("message", "Game started! (Easy mode pre-guessed: " + preGuessed + ")");
                 res.put("mask", getMaskedWord());
             }
        }
        
        return res;
    }
    
    static String autoGuessEasy() {
        // Simple implementation: auto-guess E, T, A if they are in the word.
        String[] commonLetters = {"e", "t", "a"};
        StringBuilder preGuessed = new StringBuilder();
        
        for (String s : commonLetters) {
            char c = s.charAt(0);
            if (currentWord.contains(s) && !guessedLetters.contains(c)) {
                guessedLetters.add(c);
                preGuessed.append(c).append(" ");
            }
        }
        return preGuessed.toString().trim().toUpperCase();
    }
    
    static JSONObject handleGuessLetter(JSONObject req) {
        System.out.println("Guess Letter request: " + req.toString());
        
        if (!gameActive) {
            return errorResponse("No game in progress. Start a new game first.");
        }
        
        JSONObject res = testField(req, "letter");
        if (!res.getBoolean("ok")) return res;
        
        String guessStr = req.getString("letter").trim().toLowerCase();
        
        // Validation.
        if (guessStr.length() != 1 || !Character.isLetter(guessStr.charAt(0))) {
            return errorResponse("Invalid input: guess must be a single alphabetic letter.");
        }
        
        char guess = guessStr.charAt(0);
        
        if (guessedLetters.contains(guess)) {
            return errorResponse("Letter '" + Character.toUpperCase(guess) + "' already guessed.");
        }
        
        // Update state.
        guessedLetters.add(guess);
        boolean correct = currentWord.contains(guessStr);
        String message;

        if (correct) {
            message = "Correct guess! Letter '" + Character.toUpperCase(guess) + "' found.";
        } else {
            wrongGuesses++;
            message = "Incorrect guess! Letter '" + Character.toUpperCase(guess) + "' not found.";
        }
        
        // Check for game end.
        JSONObject gameState = buildGameStateResponse("guess_letter", message);
        String gameStatus = checkGameStatus();
        
        if (!gameStatus.equals("in_progress")) {
            endGame(gameStatus, gameState);
        }

        return gameState;
    }
    
    static JSONObject handleGuessWord(JSONObject req) {
        System.out.println("Guess Word request: " + req.toString());
        
        if (!gameActive) {
            return errorResponse("No game in progress. Start a new game first.");
        }
        
        JSONObject res = testField(req, "word");
        if (!res.getBoolean("ok")) return res;
        
        String wordGuess = req.getString("word").trim().toLowerCase();
        
        // Validation (simple check).
        if (wordGuess.length() != currentWord.length() || !wordGuess.matches("[a-z]+")) {
             return errorResponse("Invalid word guess. Word must be " + currentWord.length() + " letters long and contain only letters.");
        }
        
        // Check for win/loss.
        JSONObject gameState = buildGameStateResponse("guess_word", "");
        
        if (wordGuess.equals(currentWord)) {
            endGame("win", gameState);
            gameState.put("message", "Full word guessed correctly! You win!");
        } else {
            wrongGuesses = MAX_WRONG_GUESSES;
            endGame("loss", gameState);
            gameState.put("message", "Incorrect word guess. You lose!");
        }
        
        return gameState;
    }

    static JSONObject handleGetState(JSONObject req) {
        System.out.println("Get State request: " + req.toString());
        if (!gameActive) {
            return errorResponse("No game in progress.");
        }
        return buildGameStateResponse("get_state", "Current game state.");
    }
    
    static JSONObject handleGetLetters(JSONObject req) {
         System.out.println("Get Letters request: " + req.toString());
         if (!gameActive) {
             return errorResponse("No game in progress.");
         }
         
         JSONObject res = successResponse("get_letters", "Guessed letters retrieved.");
         res.put("guessed_letters", new JSONArray(guessedLetters));
         return res;
    }
    
    static JSONObject handleGiveUp(JSONObject req) {
        System.out.println("Give Up request: " + req.toString());
        if (!gameActive) {
            return errorResponse("No game in progress to give up on.");
        }
        
        JSONObject res = successResponse("give_up", "Game ended. You gave up.");
        res.put("final_word", currentWord.toUpperCase());
        gameActive = false;
        
        return res;
    }

    static JSONObject handleGetLeaderboard(JSONObject req) {
        System.out.println("Leaderboard request: " + req.toString());
        JSONObject res = successResponse("leaderboard", "Leaderboard retrieved.");
        
        JSONArray board = new JSONArray();
        // Sort the leaderboard by descending score.
        leaderboard.sort((a, b) -> Integer.compare((Integer)b.get("score"), (Integer)a.get("score")));
        
        for (Map<String, Object> entry : leaderboard) {
            JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("name", entry.get("name"));
            jsonEntry.put("score", entry.get("score"));
            jsonEntry.put("difficulty", entry.get("difficulty"));
            board.put(jsonEntry);
        }
        
        res.put("leaderboard", board);
        return res;
    }
    
    // Game state helpers.
    /**
     * Builds a response containing the current game state, used by various handlers.
     */
    static JSONObject buildGameStateResponse(String type, String message) {
        JSONObject res = successResponse(type, message);
        res.put("mask", getMaskedWord());
        res.put("wrong_guesses", wrongGuesses);
        res.put("hangman_art", getHangmanArt());
        res.put("guessed_letters", new JSONArray(guessedLetters));
        res.put("game_status", checkGameStatus());
        return res;
    }
    
    /**
     * Checks if the game is over, and returns the status.
     * @return "win", "loss", or "in_progress"
     */
    static String checkGameStatus() {
        // Check for loss.
        if (wrongGuesses >= MAX_WRONG_GUESSES) {
            return "loss";
        }
        
        // Check for win.
        String maskedWord = getMaskedWord();
        if (!maskedWord.contains("_")) {
            return "win";
        }
        
        return "in_progress";
    }
    
    /**
     * Finalizes the game, calculates score, and updates the leaderboard.
     */
    static void endGame(String status, JSONObject res) {
        gameActive = false;
        res.put("game_status", status);
        res.put("final_word", currentWord.toUpperCase());
        
        // Scoring.
        calculateScore(status);
        res.put("score", score);
        
        // Add to leaderboard.
        Map<String, Object> gameResult = new HashMap<>();
        gameResult.put("name", playerName);
        gameResult.put("score", score);
        gameResult.put("difficulty", difficulty);
        leaderboard.add(gameResult);
        
        System.out.println("Game Over. Status: " + status + ", Score: " + score);
    }
    
    /**
     * Simplified scoring logic.
     */
    static void calculateScore(String status) {
        if (status.equals("win")) {
            // Base score = word length * 20.
            score = currentWord.length() * 20;
            // Bonus for a few wrong guesses.
            score += (MAX_WRONG_GUESSES - wrongGuesses) * 10;
            // Difficulty multiplier.
            if (difficulty.equals("medium")) score *= 1.2;
            if (difficulty.equals("hard")) score *= 1.5;
            
        } else {
            // no score for loss or give up.
            score = 0;
        }
    }

    /**
     * Generates the masked word display.
     */
    static String getMaskedWord() {
        StringBuilder masked = new StringBuilder();
        for (char c : currentWord.toCharArray()) {
            if (guessedLetters.contains(c)) {
                masked.append(Character.toUpperCase(c));
            } else {
                masked.append("_");
            }
        }
        return masked.toString();
    }
    
    /**
     * Retrieves the current hangman ASCII art stage.
     */
    static String getHangmanArt() {
        // wrongGuesses is 0-9. MAX_WRONG_GUESSES is 9.
        int stage = Math.min(wrongGuesses, MAX_WRONG_GUESSES);
        return HANGMAN_STAGES[stage];
    }

    // --- PROVIDED HANDLERS & HELPERS (NAME/QUIT/ERROR/LOADERS) ---

    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     */
    static JSONObject handleName(JSONObject req) {
        System.out.println("Name request: " + req.toString());
        JSONObject res = testField(req, "name");
        if (!res.getBoolean("ok")) {
            return res;
        }

        String name = req.getString("name");
        if (name == null || name.trim().isEmpty()) {
            return errorResponse("Name cannot be empty");
        }

        playerName = name.trim();
        res = successResponse("name", "Welcome " + playerName + "! Ready to play Hangman?");
        return res;
    }

    /**
     * Quit handler
     */
    static JSONObject handleQuit(JSONObject req) {
        System.out.println("Quit request: " + req.toString());
        JSONObject res = successResponse("quit", "Goodbye " + (playerName != null ? playerName : "player") + "!");
        return res;
    }
    
    // --- GENERIC RESPONSE HELPERS ---
    
    static JSONObject successResponse(String type, String message) {
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", type);
        res.put("message", message);
        return res;
    }
    
    static JSONObject errorResponse(String message) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", message);
        return res;
    }

    /**
     * Helper: Reset game state for new connection
     */
    static void resetGame() {
        playerName = null;
        currentWord = null;
        difficulty = null;
        guessedLetters = new TreeSet<>();
        wrongGuesses = 0;
        score = 0;
        gameActive = false;
    }
    
    // Helper: Check if field exists in request.
    static JSONObject testField(JSONObject req, String key) {
        JSONObject res = new JSONObject();
        if (!req.has(key)) {
            res.put("ok", false);
            res.put("message", "Field '" + key + "' does not exist in request");
            return res;
        }
        return res.put("ok", true);
    }
    
    /**
     * Helper: Validate JSON.
     */
    static JSONObject isValid(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            try {
                new JSONArray(json);
            } catch (JSONException ne) {
                JSONObject res = new JSONObject();
                res.put("ok", false);
                res.put("message", "Request is not valid JSON");
                return res;
            }
        }
        return new JSONObject();
    }

    /**
     * Error: no type field.
     */
    static JSONObject noType(JSONObject req) {
        System.out.println("No type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "No request type was given");
        return res;
    }

    /**
     * Error: wrong type.
     */
    static JSONObject wrongType(JSONObject req) {
        System.out.println("Wrong type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "Type '" + req.getString("type") + "' is not supported");
        return res;
    }

    /**
     * Load hangman ASCII art stages from resource file.
     */
    static void loadHangmanStages() {
        try {
            InputStream is = HangmanServer.class.getResourceAsStream("/hangman_stages.txt");
            if (is == null) {
                System.err.println("Error: hangman_stages.txt not found in resources");
                is = new FileInputStream("src/main/resources/hangman_stages.txt");
            }
            if (is == null) {
                System.err.println("FATAL: hangman_stages.txt could not be loaded from resources or local path.");
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder currentStage = new StringBuilder();
            int stageIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    HANGMAN_STAGES[stageIndex++] = "\n" + currentStage.toString();
                    currentStage = new StringBuilder();
                } else if (!line.startsWith("STAGE")) {
                    currentStage.append(line).append("\n");
                }
            }
            if (currentStage.length() > 0 && stageIndex < 10) {
                HANGMAN_STAGES[stageIndex] = "\n" + currentStage.toString();
            }
            reader.close();
            System.out.println("Loaded " + (stageIndex + 1) + " hangman stages");
        } catch (Exception e) {
            System.err.println("Error loading hangman stages: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load word lists from resource files.
     */
    static void loadWords() {
        try {
            EASY_WORDS = loadWordList("/easy_words.txt");
            MEDIUM_WORDS = loadWordList("/medium_words.txt");
            HARD_WORDS = loadWordList("/hard_words.txt");

            System.out.println("Loaded word lists: " + EASY_WORDS.length + " easy, " +
                               MEDIUM_WORDS.length + " medium, " + HARD_WORDS.length + " hard");
        } catch (Exception e) {
            System.err.println("Error loading word lists: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper: Load a single word list from file.
     */
    static String[] loadWordList(String filename) throws IOException {
        InputStream is = HangmanServer.class.getResourceAsStream(filename);
        if (is == null) {
             try {
                is = new FileInputStream("src/main/resources" + filename);
             } catch (FileNotFoundException e) {
                 throw new IOException("Word list file not found: " + filename);
             }
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<String> words = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                words.add(line.toLowerCase());
            }
        }
        reader.close();

        return words.toArray(new String[0]);
    }

    /**
     * Write response to client.
     */
    static void writeOut(JSONObject res) {
        try {
            os.writeObject(res.toString());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close connection.
     */
    static void overandout() {
        try {
            if (os != null) os.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
