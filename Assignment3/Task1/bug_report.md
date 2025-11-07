# StringConcatenation Debugging Exercise - Instructor Reference

## Overview
The stringconcatenation service is implemented in both client and server, but has **4 bugs** that prevent it from working correctly according to the protocol specification.

The Correct Protocolis in the README.md

---

## The 4 Bugs

### Bug #1:  Server Missing Field Validation for string2 & Missing Type in Error Responses
**Location:** `SockServer.java`, lines 176-187

**The Problem:**
The concat method only calls testField for "string1". If "string2" is missing, the code crashes on req.getString("string2"); with a JSONException, violating the protocol by not returning a proper ok: false error response.

**The Fix:**
Add a check for "string2" using testField. Also, add missing type in error responses.

Code Fix (Lines 176-187):
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

**Why it matters:** 
The protocol requires two strings. The server must validate all required fields and return an error instead of crashing or throwing an unhandled exception.

**How did you find this:**
Unit Test Input: {"type": "stringconcatenation", "string1": "A"}
Expected Error: {"ok": false, "message": "Field string2 does not exist in request"}

### Bug #2:  Client Sends Wrong Field Name for string1
**Location:** `SockClient.java`, lines 72-75

**The Problem:**
The client constructs the request using the key "string_1", but the protocol specification requires the key to be "string1". This causes the server to correctly fail with a "Field string1 does not exist" error, even when the client attempts a valid request.

**The Fix:**
Change the client's request field name to match the protocol.

Code Fix (lines 72-75):
// Bug Fix 2: Change "string_1" to "string1".
json.put("string1", str1); // changed from "string_1".
json.put("string2", str2);
break;

**Why it matters:** 
This is a direct protocol violation. The client must use the exact field names specified in the protocol for the server to successfully process the request.

**How did you find this:**
Unit Test Input: Correct client execution results in a server error: "Field string1 does not exist in request".

### Bug #3:  Server Returns Wrong Result Field Name
**Location:** `SockServer.java`, lines 189-201

**The Problem:**
The protocol specification requires the concatenated string to be returned in the field "result". The server implementation places it in the field "concat".

**The Fix:**
Change the output field name in the server to match the protocol.

Code Fix (lines 189-201):
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

**Why it matters:** 
This is a protocol violation in the response. A client expecting the field "result" (as defined in README.md) will fail to find it, even when the request was successful.

**How did you find this:**
Successful Request: {"type": "stringconcatenation", "string1": "A", "string2": "B"}.

Actual Response: {"ok": true, "concat": "AB"}.

Expected Response: {"ok": true, "result": "AB"}.

### Bug #4:  Client Fails to Handle String Concatenation Response
**Location:** `SockClient.java`, lines 97-106

**The Problem:**
In the successful response handler, the client assumes that any non-echo service (including concatenation) returns an integer result by calling res.getInt("result"). String concatenation returns a string ("result" is a string), causing a JSONException (JSONObject.getInt) when the client tries to read the value.

**The Fix:**
Add a specific check for the "stringconcatenation" type and read the "result" field as a string.

Code Fix (lines 97-106):
            // Bug Fix 4: Check for stringconcatenation and read the 'result' as a String.
            else if (res.getString("type").equals("stringconcatenation")) {
                System.out.println(res.getString("result")); // Read as a string.
            } else {
                System.out.println(res.getInt("result")); // Handled by 'add' and 'addmany'.
        }
    } else {
        System.out.println(res.getString("message"));
    }
}

**Why it matters:** 
The client violates its own expected protocol by incorrectly attempting to parse a string result as an integer, leading to a crash instead of displaying the correct output.

**How did you find this:**
Successful Request (After fixing Bugs 1-3): Client receives {"ok": true, "type": "stringconcatenation", "result": "AB"} but crashes on reading it.
