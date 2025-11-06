# Assignment 3 Task 2: Hangman Game Protocol

**Author:** Elliot Davis
**Date:** 11/5/2025

---

## How to Run
You can use Gradle to run things, running with ./gradlew is of course also an option
**Server:**
Default
```bash
gradle Server
```

With arguments
```bash
gradle Server -Pport=8888
```

**Client:**
Default
```bash
gradle Client --console=plain -q
```

With arguments
```bash
gradle Client -Phost=localhost -Pport=8888
```

---

## Video Demonstration

**Link:** https://www.loom.com/share/b0b3f6858f5449e3968526ecc29c6639

The video demonstrates:
- Starting server and client
- Complete game playthrough
- All implemented features

---

## Implemented Features Checklist

### Core Features (Required)
- [x] Set Player Name (provided as example)
- [x] Start New Game
- [x] Guess Letter
- [x] Game State
- [x] Win/Lose Detection
- [x] Graceful Quit

### Medium Features (Enhanced Gameplay)
- [x] Difficulty Selection
- [x] Word Guessing
- [x] Guessed Letters Command

### Advanced Features (Competition)
- [x] Scoring System
- [x] Leaderboard

**Note:** Mark [x] for completed features, [ ] for not implemented.

---

## Protocol Specification

### Overview
This is a request and response protocol, using JSON, over a socket connection. Each message is a JSON object, and requests require a "type" field. Successful responses include "ok": true and the matching "type". Error responses include "ok": false and a "message" explaining the error.

---

### 1. Set Player Name

**Request:**
{
    "type": "name",
    "name": "<string>"
}

**Success Response:**
{
    "type": "name",
    "ok": true,
    "message": "Welcome <name>! Ready to play Hangman?"
}

**Error Response:**
{
    "ok": false,
    "message": "Name cannot be empty"
}

---

### 2. Start New Game

**Request:**
{
    "type": "start",
    "difficulty": "<string>" // Required: "easy", "medium", or "hard"
}

**Success Response:**
{
    "type": "start",
    "ok": true,
    "message": "Game started!",
    "mask": "_______",
    "wrong_guesses": 0,
    "hangman_art": "<string>"
}

**Error Response(s):**
{
    "ok": false,
    "message": "Player name not set. Set your name first."
}

OR

{
    "ok": false,
    "message": "Invalid difficulty: <value>. Must be easy, medium, or hard."
}

---

### 3. Guess Letter

**Request:**
{
    "type": "guess_letter",
    "letter": "<string>" // Single character.
}

**Success Response:**
{
    "type": "guess_letter",
    "ok": true,
    "mask": "H_NGM_N", // Updated word.
    "wrong_guesses": 1,
    "hangman_art": "<string>", // Updated ASCII art.
    "guessed_letters": ["A", "E", "I", "O", "U"],
    "message": "Incorrect guess! Letter 'Z' not found.",
    "game_status": "in_progress" // OR "win" OR "loss"
    // If "win" or "loss", include:
    // "final_word": "HANGMAN",
    // "score": 150 
}

**Error Response(s):**
{
    "ok": false,
    "message": "No game in progress. Start a new game first."
}

OR

{
    "ok": false,
    "message": "Invalid input: guess must be a single letter."
}

OR

{
    "ok": false,
    "message": "Letter 'A' already guessed."
}

---

### 4. Guess Word

**Request:**
{
    "type": "guess_word",
    "word": "<string>" // Entire word.
}

**Success Response:**
{
    "type": "guess_word",
    "ok": true,
    "mask": "HANGMAN", // Full word revealed at game end.
    "wrong_guesses": 0, // Final state after guess.
    "hangman_art": "<string>", // Final ASCII art.
    "message": "Full word guessed correctly! You win!",
    "game_status": "win", // OR "loss".
    "final_word": "HANGMAN",
    "score": 200
}

**Error Response(s):**
{
    "ok": false,
    "message": "No game in progress. Start a new game first."
}

OR

{
    "ok": false,
    "message": "Invalid input: word guess is too short/long or contains invalid characters."
}

---

### 5. Get Game State

**Request:**
{
    "type": "get_state"
}

**Success Response:**
{
    "type": "get_state",
    "ok": true,
    "message": "Current game state.", 
    "mask": "H_NGM_N",
    "wrong_guesses": 1,
    "hangman_art": "<string>",
    "guessed_letters": ["H", "N", "G", "M"],
    "game_status": "in_progress"
}

**Error Response(s):**
{
    "ok": false,
    "message": "No game in progress."
}

---

### 6. Get Guessed Letters

**Request:**
{
    "type": "get_letters"
}

**Success Response:**
{
    "type": "get_letters",
    "ok": true,
    "guessed_letters": ["A", "E", "I", "O", "U"]
}

**Error Response(s):**
{
    "ok": false,
    "message": "No game in progress."
}

---

### 7. Give Up

**Request:**
{
    "type": "give_up"
}

**Success Response:**
{
    "type": "give_up",
    "ok": true,
    "message": "Game ended. You gave up.",
    "final_word": "HANGMAN"
}

**Error Response(s):**
{
    "ok": false,
    "message": "No game in progress to give up on."
}

---

### 8. Get Leaderboard

**Request:**
{
    "type": "leaderboard"
}

**Success Response:**
{
    "type": "leaderboard",
    "ok": true,
    "leaderboard": [
        {"name": "Adam", "score": 250, "difficulty": "hard"},
        {"name": "Bob", "score": 150, "difficulty": "medium"}
    ]
}

**Error Response(s):**
{
    "ok": false,
    "message": "Could not retrieve leaderboard data."
}

---

### 9. Quit

**Request:**
{
    "type": "quit"
}

**Success Response:**
{
    "type": "quit",
    "ok": true,
    "message": "Goodbye <name>!"
}

## Error Handling Strategy

[Explain your approach to error handling:]

**Server-side validation:**
- [What validations does your server perform?]
  - Required field presence, such as type, name, difficulty, and letter/word.
  - Type/format validation (e.g. difficulty must be "easy", "medium", or "hard", etc.).
  - State validation (e.g. must set name before start).
  - JSON validity, handled by the isValid helper.

- [How do you handle missing fields?]
Use the testField(req, key) helper function to check for field existence. If a required field is missing, return a response with "ok": false and a message: "Field 'key' does not exist in request."

- [How do you handle invalid data types?]
The org.json library helps with basic type checking. For semantic invalid data (such as an unrecognized difficulty value), return a response with "ok": false and a specific error message (e.g., "Invalid difficulty: ...", "Invalid input: guess must be a single letter.").

- [How do you handle game state errors?]
Each request handler checks the gameActive and playerName server variables. If a request is received that is invalid for the current state, return a response with "ok": false and a message: "No game in progress. Start a new game first."

---

## Robustness

[Explain how you ensured robustness:]

**Server robustness:**
- [How does server handle invalid input without crashing?]
    - The main loop uses a try-catch block around in.readObject() to handle client disconnection.
    - Incoming JSON is validated for basic syntax by isValid(), and for required fields by testField().
    - Handler methods are wrapped to ensure an "ok": false response is sent for logical and/or input errors instead of throwing unhandled exceptions.

**Client robustness:**
- [How does client handle unexpected responses?]
    - The sendRequest() helper function uses a try-catch block for communication errors, and checks if the response is null prior to processing. It checks for the "ok" field to determine success/failure.
    - The client is "dumb" and relies on the server's "ok": false and "message" fields to report issues to the user.

- [What happens if server is unavailable?]
    - The initial new Socket(host, port) in main will throw an Exception, and an error message is printed before the client exits (System.exit(1) on failure to connect).
    - If the connection is lost during gameplay, sendRequest() will catch the Exception, print "Error communicating with server," and return null, which the calling menu handler will handle.

---

## Assumptions (if applicable)

[List any assumptions you made about the protocol or game rules]

1. The client sends a type: "name" request only once at the beginning of the game.

2. Word lists are non-empty, and the server can load them.

3. The server maintains state only for a single concurrent player.

---

## Known Issues

[List any known bugs or limitations]

**FIXED:**
1. The client-side giveUp() method currently has a [TODO] and is not yet implemented to send the type: "give_up" request.

2. The current implementation of HangmanClient only exits the loop on quit or connection failure. It should instead return to the showInitialMenu if a critical error occurs mid-game.

---

