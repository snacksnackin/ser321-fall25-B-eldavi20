import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.Scanner;

/**
 * SockClient class for connecting to a server and sending JSON requests.
 */
class SockClient {
    static Socket sock = null;
    static String host = "localhost";
    static int port = 8888;
    static OutputStream out;
    // Using and Object Stream here and a Data Stream as return. Could both be the same type I just wanted
    // to show the difference. Do not change these types.
    static ObjectOutputStream os;
    static DataInputStream in;

    public static void main (String args[]) {

        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }

        try {
            connect(host, port); // connecting to server
            System.out.println("Client connected to server.");
            boolean requesting = true;
            while (requesting) {
                System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - string concatenation, 4 - cart, 5 - stats (0 to quit)");
                // added '4 - cart' and '5 - stats'.
                Scanner scanner = new Scanner(System.in);
                int choice = Integer.parseInt(scanner.nextLine());
                // You can assume the user put in a correct input, you do not need to handle errors here
                // You can assume the user inputs a String when asked and an int when asked. So you do not have to handle user input checking
                JSONObject json = new JSONObject(); // request object
                switch(choice) {
                    case 0:
                        System.out.println("Choose quit. Thank you for using our services. Goodbye!");
                        requesting = false;
                        break;
                    case 1:
                        System.out.println("Choose echo, which String do you want to send?");
                        String message = scanner.nextLine();
                        json.put("type", "echo");
                        json.put("data", message);
                        break;
                    case 2:
                        System.out.println("Choose add, enter first number:");
                        String num1 = scanner.nextLine();
                        json.put("type", "add");
                        json.put("num1", num1);
                        
                        System.out.println("Enter second number:");
                        String num2 = scanner.nextLine();
                        json.put("num2", num2);
                        break;
                    case 3:
                        System.out.println("Choose string concatenation, enter first string:");
                        String str1 = scanner.nextLine();
                        System.out.println("Enter second string:");
                        String str2 = scanner.nextLine();
                        json.put("type", "stringconcatenation");
                        
                        // Bug Fix 2: Change "string_1" to "string1".
                        json.put("string1", str1); // changed from "string_1".
                        json.put("string2", str2);
                        break;
                    case 4: // Shopping cart service implementation.
                        json.put("type", "cart");
                        System.out.println("Cart Service: 1 - add, 2 - remove, 3 - list, 4 - clear");
                        int cartAction = Integer.parseInt(scanner.nextLine());
                        
                        switch(cartAction) {
                            case 1: // Add.
                                json.put("action", "add");
                                System.out.println("Enter item name:");
                                json.put("item", scanner.nextLine());
                                System.out.println("Enter quantity (integer):");
                                json.put("quantity", scanner.nextLine()); // Send as String.
                                break;
                            case 2: // Remove.
                                json.put("action", "remove");
                                System.out.println("Enter item name:");
                                json.put("item", scanner.nextLine());
                                System.out.println("Enter quantity (integer):");
                                json.put("quantity", scanner.nextLine()); // Send as String.
                                break;
                            case 3: // List.
                                json.put("action", "list");
                                break;
                            case 4: // Clear.
                                json.put("action", "clear");
                                break;
                            default:
                                System.out.println("Invalid cart action.");
                                json = new JSONObject(); // Clear invalid request.
                        }
                        break;
                    case 5:
                        System.out.println("Choose stats.");
                        json.put("type", "stats");
                        json.put("operation", "max");
                        
                        JSONArray numbersArray = new JSONArray();
                        numbersArray.put(10);
                        numbersArray.put(20);
                        numbersArray.put(30);
                        json.put("numbers", numbersArray);
                        break;
                }
            
                if(!requesting) {
                    continue;
                }

                // write the whole message
                os.writeObject(json.toString());
                // make sure it wrote and doesn't get cached in a buffer
                os.flush();

                // handle the response
                // - not doing anything other than printing some things, make this better
                // !! you will most likely need to parse the response for the other 2 services!
                String i = (String) in.readUTF();
                JSONObject res = new JSONObject(i);
                System.out.println("Got response: " + res);
                
                if (res.getBoolean("ok")){
                    if (res.getString("type").equals("echo")) {
                        System.out.println(res.getString("echo"));
                    // Bug Fix 4: Check for stringconcatenation and read the 'result' as a String.
                    } else if (res.getString("type").equals("stringconcatenation")) {
                        System.out.println(res.getString("result")); // Read as a string.
                    } else if (res.getString("type").equals("cart")) {
                        String action = res.getString("action");
                        if (action.equals("list")) {
                            System.out.println("Cart Contents (Total: " + res.getInt("itemCount") + " items):");
                            JSONArray items = res.getJSONArray("items");
                            for (int j = 0; j < items.length(); j++) {
                                JSONObject item = items.getJSONObject(j);
                                System.out.println(" - " + item.getString("item") + ": " + item.getInt("quantity"));
                            }
                        } else if (action.equals("clear")) { // add, remove, and clear.
                            System.out.println(res.getString("message")); // prints "Cart cleared".
                        } else {
                            System.out.println(res.getString("message") + " (New total: " + res.getInt("itemCount") + " items)");
                        }
                    } else {
                        System.out.println(res.getInt("result")); // Handled by 'add' and 'addmany'.
                    }
                } else {
                    System.out.println(res.getString("message"));
                }
            } // Closes while (requesting) loop
            
            // want to keep requesting services so don't close connection
            //overandout();
        } // Closes the try block

        catch (Exception e) {
            e.printStackTrace();
        }
    } // Closes the main method

    private static void overandout() throws IOException {
        //closing things, could
        in.close();
        os.close();
        sock.close(); // close socked after sending
    }

    public static void connect(String host, int port) throws IOException {
        // open the connection
        sock = new Socket(host, port); // connect to host and socket on port 8888

        // get output channel
        out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new ObjectOutputStream(out);

        in = new DataInputStream(sock.getInputStream());
    }
}
