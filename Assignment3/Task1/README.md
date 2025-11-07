##### Author: Instructor team SE, ASU Polytechnic, CIDSE, SE

##### Purpose
This is starter code for Assignment 3, Task 1.

**What's Provided:**
- Three working example services: `echo`, `add`, `addmany` (server-side only)
- One buggy service: `stringconcatenation` (has bugs to find and fix)
- Two placeholder services to implement: `temperature`, `cart`

**Your Tasks:**
- Part A: Document the `addmany` protocol by reading the server code
- Part B: Find and fix 4 bugs in the `stringconcatenation` service
- Part C: Implement `temperature` and `cart` services following the protocols below
- Part D: Deploy to AWS and test with peers

**How to Run:**
Default port and host:
  gradle Client
  gradle Server

Changing the port:
  gradle Server -Pport=9000
  gradle Client -Pport=9000

Changing the host if needed:
  gradle Client -Phost=localhost -Pport=9000

Running things without Gradle being so verbose with default values
 gradle Server --console=plain -q
 gradle Client --console=plain -q

Running with Gradle wrapper:
 Any combination from above with use
 ./gradlew Server

**How to Test:**
There are two tests classes given, work on the ServerTest class when you write your test cases. 
The ServerTest class assumes a server it running on 8888 and will establish a connection and make different requests. 

The Testing class is only calling server methods without establishen a connection. This is just to show another way how to test things. It does not need a server running. I prefer the first way but to test a server without having to also take network issues into account and only test the methods in it, this is a good option. 


*Use gradle or ./gradlew up to you*

Run all tests, assumes that server runs on 8888:
  gradle test

Run only the ServerTest class, assumes server runs on 8888
  gradle serverTest


Run only the Testing class, this calls methods from SockServer directly, server does not need to run
  gradle simpleTest

## Protocol: ##

### Echo: ###

Request: 

    {
        "type" : "echo", -- type of request
        "data" : <String>  -- String to be echoed 
    }

General response:

    {
        "type" : "echo", -- echoes the initial response
        "ok" : <bool>, -- true or false depending on request
        "echo" : <String>,  -- echoed String if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "echo",
        "ok" : true,
        "echo" : <String> -- the echoed string
    }

Error response:

    {
        "type" : "echo",
        "ok" : false,
        "message" : <String> -- what went wrong
    }

### Add: ###
Request:

    {
        "type" : "add",
        "num1" : <String>, -- first number -- String needs to be an int number e.g. "3"
        "num2" : <String> -- second number -- String needs to be an int number e.g. "4"
    }

General response

    {
        "type" : "add", -- echoes the initial request
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>,  -- result if ok true
        "message" : <String>,  -- error message if ok false
    }

Success response:

    {
        "type" : "add",
        "ok" : true,
        "result" : <int> -- the result of add
    }

Error response:

    {
        "type" : "add",
        "ok" : false,
        "message" : <String> - error message about what went wrong
    }


### AddMany: ###
**TODO - Part A:** You need to document this protocol by:
1. Reading the server implementation in SockServer.java
2. Testing the service with various inputs (write Unit Tests and/or add the service to the client to test)
3. Write the complete protocol specification here (follow the format of echo and add above)
4. Document ALL possible error cases

Request:

    {
        "type" : "addmany",
        "nums" : [<int>, <int>, ...] -- An array of integers to be summed, e.g., [5, 10, 15] or [1, 0, 9] or [42] or [-5, 10, -2], etc.
    }

General response:

    {
        "type" : "addmany", -- echoes the initial request type
        "ok" : <bool>, -- true or false depending on request
        "result" : <int>,  -- result if ok true
        "message" : <String>  -- error message if ok false
    }

Success response:

    {
        "type" : "addmany",
        "ok" : true,
        "result" : <int> -- the result of the summation
    }

Error response (Missing 'nums' field'):

    {
        "type" : "addmany",
        "ok" : false,
        "message" : "Field nums does not exist in request"
    }
    
Error response (Empty 'nums' array):

    {
        "type" : "addmany",
        "ok" : false,
        "message" : "Array 'nums' cannot be empty"
    }
    
Error response (Array contains a float or string):

    {
        "type" : "addmany",
        "ok" : false,
        "message" : "Values in array need to be ints"
    }
    
Error response (Wrong type value):

    {
        "type" : "addmany",
        "ok" : false,
        "message" : "Type subtract is not supported."
    }

### StringConcatenation: ###
This service will concatenate two strings provided by the client. The client will send a request to the server with two strings to be concatenated. 
The server will concatenate the strings and send back the result to the client.

**NOTE - Part B:** This service has 4 bugs! The implementation (both client and server) does NOT match the protocol below. 
Find and fix all bugs in the code to match the protocol, then document them in bug_report.md. 

Request:

    {
        "type" : "stringconcatenation",
        "string1" : <String>, -- first string
        "string2" : <String> -- second string
    }

General response:

    {
        "type" : "stringconcatenation",
        "ok" : <bool>, -- true or false depending on request
        "result" : <String>,  -- concatenated string if ok true
        "message" : <String>  -- error message if ok false
    }

Success response:

    {
        "type" : "stringconcatenation",
        "ok" : true,
        "result" : <String> -- concatenated string
    }

Error response:

    {
        "type" : "stringconcatenation",
        "ok" : false,
        "message" : <String> -- error message about what went wrong
    }


### Temperature: ###
**TODO - Part C:** You need to implement this service following the protocol specification below.

This service converts temperatures between fahrenheit, celsius, and kelvin. 
The client sends a temperature value and the source/target units, and the server performs the conversion.

Request:

    {
        "type" : "temperature",
        "value" : <String>, -- numeric value to convert (can include decimals) e.g. "32.5"
        "from" : <String>, -- source unit: "fahrenheit", "celsius", or "kelvin"
        "to" : <String> -- target unit: "fahrenheit", "celsius", or "kelvin"
    }

General response:

    {
        "type" : "temperature",
        "ok" : <bool>, -- true or false depending on request
        "result" : <Number>, -- converted temperature if ok true (rounded to 2 decimals)
        "formula" : <String>, -- conversion formula with values if ok true
        "message" : <String> -- error message if ok false
    }

Success response:

    {
        "type" : "temperature",
        "ok" : true,
        "result" : 0.28,
        "formula" : "(32.5°F - 32) × 5/9 = 0.28°C"
    }

Error response (invalid unit):

    {
        "ok" : false,
        "message" : "Field 'from' must be one of: fahrenheit, celsius, kelvin"
    }

Error response (below absolute zero):

    {
        "ok" : false,
        "message" : "Temperature cannot be below absolute zero (-273.15°C / -459.67°F / 0K)"
    }


### Cart: ###
**TODO - Part C:** You need to implement this service following the protocol specification below.

This service manages a shopping cart. The client can add items, remove items, list the cart contents, and clear the cart. The cart persists for the duration of the client connection.

**Action: add**

Request:

    {
        "type" : "cart",
        "action" : "add",
        "item" : <String>, -- item name
        "quantity" : <Number> -- positive integer quantity to add
    }

Success response:

    {
        "type" : "cart",
        "ok" : true,
        "action" : "add",
        "itemCount" : <Number>, -- total items in cart (sum of all quantities)
        "message" : <String> -- confirmation message e.g. "Added 3 apple(s) to cart"
    }

**Action: remove**

Request:

    {
        "type" : "cart",
        "action" : "remove",
        "item" : <String>, -- item name
        "quantity" : <Number> -- positive integer quantity to remove
    }

Success response:

    {
        "type" : "cart",
        "ok" : true,
        "action" : "remove",
        "itemCount" : <Number>, -- remaining total items in cart
        "message" : <String> -- confirmation message e.g. "Removed 2 apple(s) from cart"
    }

**Action: list**

Request:

    {
        "type" : "cart",
        "action" : "list"
    }

Success response:

    {
        "type" : "cart",
        "ok" : true,
        "action" : "list",
        "items" : [
            {
                "item" : <String>,
                "quantity" : <Number>
            }
        ], -- array of items in cart (empty array if cart is empty)
        "itemCount" : <Number> -- total items
    }

**Action: clear**

Request:

    {
        "type" : "cart",
        "action" : "clear"
    }

Success response:

    {
        "type" : "cart",
        "ok" : true,
        "action" : "clear",
        "message" : "Cart cleared"
    }

**Error responses:**


Item not found (remove):

    {
        "ok" : false,
        "message" : "Item 'apple' not found in cart"
    }

Not enough quantity (remove):

    {
        "ok" : false,
        "message" : "Cannot remove 5 apple(s), only 2 in cart"
    }

Invalid quantity (the "type" - int, string, double - is correct but the number is invalid)

    {
        "ok" : false,
        "message" : "Field 'quantity' must be a positive integer"
    }


### General error responses: ###
These are used for all requests.

Error response: When a required field "key" is not in request

    {
        "ok" : false
        "message" : "Field <key> does not exist in request"
    }

Error response: When a required field "key" is not of correct "type" (int, string, double)

    {
        "ok" : false
        "message" : "Field <key> needs to be of type: <type>"
    }

Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "Type <type> is not supported."
    }


Error response: When the "type" is not supported, so an unsupported request

    {
        "ok" : false
        "message" : "req not JSON"
    }

---

## Task 1.2: Mystery Service Discovery ##

A mystery service is included in this starter code (provided as a JAR file). Your task is to:
1. Discover the complete protocol through systematic testing
2. Document your discovery process in `discovery_log.md`
3. Write the complete protocol specification in `protocol.md`

The service is already integrated into the server. All details must be discovered through testing.

See `discovery_and_protocol_TEMPLATE.md` for the required documentation format.
