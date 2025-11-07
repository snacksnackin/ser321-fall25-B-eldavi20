import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;


  public static void main (String args[]) {

    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and calling handling one request.
       *
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");
          
        // Added for shopping cart service. Initialize state per connection.
        Map<String, Integer> cart = new HashMap<>();

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s);

          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("addmany")) {
            res = addmany(req);
          } else if (req.getString("type").equals("stringconcatenation")) {
            res = concat(req);
          } else if (req.getString("type").equals("cart")) { // Added shopping cart routing.
            res = cart(req, cart); // Pass request and per connection cart state.
          } else if (req.getString("type").equals("stats")) {
            // Mystery service - discover the protocol
            res = mysteryservice.MysteryService.processRequest(req);
          } else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  
  // handles string concatenation
  static JSONObject concat(JSONObject req) {
    System.out.println("Concatenation request: " + req.toString());

    // Bug Fix 1: Add validation check for string2. Add type to error responses.
    JSONObject res = testField(req, "string1");
    if (!res.getBoolean("ok")) {
        res.put("type", "stringconcatenation"); // add type to error response.
        return res;
    }

    JSONObject res2 = testField(req, "string2"); // new check.
    if (!res2.getBoolean("ok")) {
        res2.put("type", "stringconcatenation"); // add type to error response.
        return res2; // Error returned if missing.
    }

    // Bug Fix 3: Add 'type' to response and change result key.
    res = new JSONObject();
    res.put("ok", true);
    res.put("type", "stringconcatenation"); // added 'type' to success response.

    String str1 = req.getString("string1");
    String str2 = req.getString("string2");
          
    // Change "concat" to "result" as per protocol.
    res.put("result", str1 + str2); // changed from "concat" to "result".

    return res;
}
    
// New cart service method, which handles the shopping cart service.
static JSONObject cart(JSONObject req, Map<String, Integer> cart) {
    System.out.println("Cart request: " + req.toString());
        
    // Validate action field existence.
    JSONObject res = testField(req, "action");
    if (!res.getBoolean("ok")) {
        res.put("type", "cart");
        return res;
    }

    String action;
    try {
        action = req.getString("action");
    } catch (JSONException e) {
        res.put("ok", false);
        res.put("message", "Field 'action' needs to be of type: String");
        res.put("type", "cart");
        return res;
    }

    // Initialize success response.
    JSONObject successRes = new JSONObject();
    successRes.put("type", "cart");
    successRes.put("ok", true);
    successRes.put("action", action);

    switch (action) {
        case "add":
        case "remove":
            return handleCartModify(req, cart, action, successRes);

        case "list":
            return handleCartList(cart, successRes);

        case "clear":
            return handleCartClear(cart, successRes);

        default:
            successRes.put("ok", false);
            successRes.put("message", "Action '" + action + "' is not supported for type 'cart'.");
            return successRes;
    }
}
    
// Cart helper methods.
// Helper to calculate the total number of items (sum of all quantities).
static int calculateTotalItemCount(Map<String, Integer> cart) {
    return cart.values().stream().mapToInt(Integer::intValue).sum();
}

// Handles "add" and "remove" actions.
static JSONObject handleCartModify(JSONObject req, Map<String, Integer> cart, String action, JSONObject successRes) {
    // Validate 'item' field.
    JSONObject itemRes = testField(req, "item");
    if (!itemRes.getBoolean("ok")) {
        itemRes.put("type", "cart");
        return itemRes;
    }
        
    // Validate 'quantity' field.
    JSONObject quantRes = testField(req, "quantity");
    if (!quantRes.getBoolean("ok")) {
        quantRes.put("type", "cart");
        return quantRes;
    }

    String itemName = req.getString("item");
    int quantity;

    try {
        quantity = req.getInt("quantity");
    } catch (JSONException e) {
        successRes.put("ok", false);
        successRes.put("message", "Field 'quantity' needs to be of type: Number");
        return successRes;
    }

    // Validate positive quantity.
    if (quantity <= 0) {
        successRes.put("ok", false);
        successRes.put("message", "Field 'quantity' must be a positive integer");
        return successRes;
    }

    if (action.equals("add")) {
        // Add action logic.
        cart.put(itemName, cart.getOrDefault(itemName, 0) + quantity);
        successRes.put("itemCount", calculateTotalItemCount(cart));
        successRes.put("message", "Added " + quantity + " " + itemName + "(s) to cart");
        return successRes;
            
    } else { // action.equals("remove")
        int currentQuantity = cart.getOrDefault(itemName, 0);

        // Validate item exists (Item not found error).
        if (currentQuantity == 0) {
            successRes.put("ok", false);
            successRes.put("message", "Item '" + itemName + "' not found in cart");
            return successRes;
        }

        // Validate quantity available (Not enough quantity error).
        if (quantity > currentQuantity) {
            successRes.put("ok", false);
            successRes.put("message", "Cannot remove " + quantity + " " + itemName + "(s), only " + currentQuantity + " in cart");
            return successRes;
        }
            
        // Remove action logic.
        currentQuantity -= quantity;
        if (currentQuantity <= 0) {
            cart.remove(itemName);
        } else {
            cart.put(itemName, currentQuantity);
        }
            
        successRes.put("itemCount", calculateTotalItemCount(cart));
        successRes.put("message", "Removed " + quantity + " " + itemName + "(s) from cart");
        return successRes;
    }
}

// Handles "list" action.
static JSONObject handleCartList(Map<String, Integer> cart, JSONObject successRes) {
    JSONArray itemsArray = new JSONArray();
    for (Map.Entry<String, Integer> entry : cart.entrySet()) {
        JSONObject itemObject = new JSONObject();
        itemObject.put("item", entry.getKey());
        itemObject.put("quantity", entry.getValue());
        itemsArray.put(itemObject);
    }
        
    successRes.put("items", itemsArray);
    successRes.put("itemCount", calculateTotalItemCount(cart));
    return successRes;
}

// Handles "clear" action.
static JSONObject handleCartClear(Map<String, Integer> cart, JSONObject successRes) {
    cart.clear();
    successRes.put("message", "Cart cleared");
    return successRes;
}

  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    JSONArray array = req.getJSONArray("nums");
    if (array.length() == 0){
      res.put("ok", false);
      res.put("message", "Array 'nums' cannot be empty");
      return res;
    }

    for (int i = 0; i < array.length(); i ++){
      try{
        result += array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }


//  SOME GENERAL ERROR MESSAGES

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }


// SENDING OUT REPSONSES, CLOSING CONNECTION
  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}
