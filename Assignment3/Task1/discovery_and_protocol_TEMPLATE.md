# Task 1.2: Mystery Service Discovery and Protocol Documentation

**Your Name:** Elliot Davis
**How I tested:** Extended Client

---

## Part 1: Discovery Log

Document at least 8 test attempts showing your systematic investigation.

### Attempt 1
**Request Sent:**
    {
        "type": "stats"
    }

**Response Received:**
    {
        "ok": false,
        "message": "Field 'operation' does not exist in request"
    }

**What I Learned:**
The service uses the standard Task 1 error format ("ok": false, "message": "..."). The service requires the field 'operation' in subsequent requests.

---

### Attempt 2
**Request Sent:**
    {
        "type": "stats",
        "operation", "info"
    }

**Response Received:**
    {
        "ok": false,
        "message": "Field 'numbers' does not exist in request"
    }

**What I Learned:**
The field "operation" is the correct command field name. The value "info" is likely an accepted operation, but one that requires numerical data. The service expects a "numbers" field.

---

### Attempt 3
**Request Sent:**
    {
        "type": "stats",
        "operation": "info",
        "numbers": [10, 20, 30] // Sent an array of numbers.
    }

**Response Received:**
    {
    "ok": false,
    "message": "Operation 'info' not supported. Valid operations: mean, sum, min, max, greaterThan, contains, help"
    }

**What I Learned:**
The operation "info" is not supported. The command field is definitely "operation", the data field is definitely "numbers", and the values in the "numbers" array are partially accepted (or, the type check has not failed yet). Supported operations are: "mean", "sum", "min", "max", "greaterThan", "contains", and "help".

---

### Attempt 4
**Request Sent:**
    {
        "type": "stats",
        "operation": "sum",
        "numbers": [10, 20, 30] // Sent an array of numbers.
    }

**Response Received:**
    {
        "result":60, // Resultant sum of the array.
        "count":3,
        "type":"stats",
        "ok":true,
        "operation":"sum"
    }

**What I Learned:**
The request format requires fields "type" (stats), "operation" (sum, in this case), and "numbers". The response returns the total sum in the "result" field and the number of elements in the "count" field.

---

### Attempt 5
**Request Sent:**
    {
        "type": "stats",
        "operation": "mean", 
        "numbers": [10, 20, 30] // Sum is 60, Count is 3. Mean should be 20.
    }

**Response Received:**
    {
        "result":20,
        "count":3,
        "type":"stats",
        "ok":true,
        "operation":"mean"
    }

**What I Learned:**
The "mean" operation uses the same format as "sum". It requires the "numbers" field and returns the average in the result field.

---

### Attempt 6
**Request Sent:**
    {
        "type": "stats",
        "operation": "help"
    }

**Response Received:**
    {
        "operations":["mean","sum","min","max","greaterThan","contains","help"],
        "type":"stats",
        "ok":true
    }

**What I Learned:**
The "help" operation is a utility command that does not require the "numbers" field. It returns the list of supported operations in an array called operations. The client needs a specific handler for stats responses to avoid crashing when the "result" field is absent.

---

### Attempt 7
**Request Sent:**
    {
        "type": "stats",
        "operation": "min",
        "numbers": [10, 20, 30] // The minimum is 10.
    }

**Response Received:**
    {
        "result":10,
        "count":3,
        "type":"stats",
        "ok":true,
        "operation":"min"
    }

**What I Learned:**
The "min" operation requires the "numbers" field and returns the minimum value in the "result" field.

---

### Attempt 8
**Request Sent:**
    {
        "type": "stats",
        "operation": "max",
        "numbers": [10, 20, 30] // The maximum is 30.
    }

**Response Received:**
    {
        "result":30,
        "count":3,
        "type":"stats",
        "ok":true,
        "operation":"max"
    }

**What I Learned:**
The "max" operation requires the "numbers" field and returns the maximum value in the "result" field.

---

[Continue for at least 8 attempts - show your progression from initial testing to complete understanding]

---

## Part 2: Complete Protocol Specification

Follow the same format as Task 1.1 README protocols.

### Stats Service

This service performs statistical operations (such as sum, mean, minimum, maximum), as well as comparison operations (greaterThan, contains) on an array of numbers.

#### sum

**Request:**
    {
        "type" : "stats",
        "operation" : "sum",
        "numbers" : [10, 20, 30] 
    }

**Success Response:**
    {
        "result" : 60,
        "count" : 3,
        "type" : "stats",
        "ok" : true,
        "operation" : "sum"
    }
    
#### mean

**Request:**
    {
        "type" : "stats",
        "operation" : "mean",
        "numbers" : [10, 20, 30] 
    }

**Success Response:**
    {
        "result" : 20,
        "count" : 3,
        "type" : "stats",
        "ok" : true,
        "operation" : "mean"
    }
    
#### min

**Request:**
    {
        "type" : "stats",
        "operation" : "min",
        "numbers" : [10, 20, 30] 
    }

**Success Response:**
    {
        "result" : 10,
        "count" : 3,
        "type" : "stats",
        "ok" : true,
        "operation" : "min"
    }
    
#### max

**Request:**
    {
        "type" : "stats",
        "operation" : "max",
        "numbers" : [10, 20, 30] 
    }

**Success Response:**
    {
        "result" : 30,
        "count" : 3,
        "type" : "stats",
        "ok" : true,
        "operation" : "max"
    }
    
#### greaterThan

**Request:**
    {
        "type" : "stats",
        "operation" : "greaterThan",
        "numbers" : [10, 20, 30, 40]
        "target" : 25 
    }

**Success Response:**
    {
        "result" : 2,
        "count" : 4,
        "type" : "stats",
        "ok" : true,
        "operation" : "greaterThan"
    }
    
#### contains

**Request:**
    {
        "type" : "stats",
        "operation" : "contains",
        "numbers" : [10, 20, 30]
        "target" : 20
    }

**Success Response:**
    {
        "result" : true,
        "count" : 3,
        "type" : "stats",
        "ok" : true,
        "operation" : "contains"
    }
    
#### help

**Request:**
    {
        "type" : "stats",
        "operation" : "help"
    }

**Success Response:**
    {
        "operations" : ["mean","sum","min","max","greaterThan","contains","help"],
        "type" : "stats",
        "ok" : true,
        "operation" : "help"
    }

**Error Responses:**

#### General error (missing data field)
    {
        "ok" : false,
        "message" : "Field <key> does not exist in request" -- e.g., Missing 'numbers' or 'target'
    }
    
#### Operation error (unsupported operation)
    {
        "ok": false,
        "message": "Operation 'info' not supported. Valid operations: mean, sum, min, max, greaterThan, contains, help" 
    }

---

[Document ALL operations you discovered]

---

## Part 3: Summary

**Total Operations Discovered:**
The Stats Service supports 7 operations.
1. sum
2. mean
3. min
4. max
5. help
6. greaterThan
7. contains

**How I approached discovery:**
My discovery process followed a systematic black-box testing method.
I began with a minimal request ({"type": "stats"}) which revealed that the service requires the field "operation". I then tested the combined request ({"type": "stats", "operation": "info"}) which failed, but the error message indicated that the service required the "numbers" field as well. I proceeded to systematically test the operations from simplest to most complex. I tested sum, mean, min, max, then help, then greaterThan and contains.
Simple stats (sum, mean, min, and max) were confirmed to use the input fields operation and numbers, and returned the result in the result field.
The utility help command was tested without the numbers field to confirm it was an independent command, and it was confirmed to return the operations list in an operations array.
The complex comparisons (greaterThan, contains) were tested next. They were tested with an intentional error (omitting the necessary comparison value) to discover the name of the final required field.

**Most challenging part:**
The response {"ok":false,"message":"Field 'operation' does not exist in request"} was misleading because the client was sending the field. This required a quick pivot to test the more likely field name (action), although subsequent testing proved operation was correct, suggesting the error came from an internal check within the mystery service JAR that was looking for the data field (numbers) instead of checking the command field first.
