package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WebServer {
    private static final int THREADS = 100;
    private static final int PORT = 9000;

    public static void main(String[] args) throws IOException {
        new WebServer(PORT);
    }

    public WebServer(int port) throws IOException {
        ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
        System.out.println("Listening on " + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        while (true) {
            try {
                Socket socket = server.accept();
                pool.execute(new RequestHandler(socket));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class RequestHandler implements Runnable {
        private Socket socket;

        public RequestHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintStream out = new PrintStream(socket.getOutputStream())) {

                String request = in.readLine();
                if (request == null) return;

                System.out.println("Received: " + request);
                String path = request.split(" ")[1].substring(1);
                
                String headerLine;
                while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                    System.out.println("Received: " + headerLine);
                }
                System.out.println("FINISHED PARSING HEADER\n");

                out.print(createResponse(path));
                out.flush();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String createResponse(String request) throws Exception {
        StringBuilder builder = new StringBuilder();

        if (request.isEmpty() || request.equals("root.html")) {
            File file = new File("www/root.html");
            if (file.exists()) {
                String links = buildFileList(new File("www"));
                String page = new String(readFileInBytes(file));
                page = page.replace("${links}", links);
                
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("Content-Length: " + page.getBytes().length + "\n");
                builder.append("\n");
                builder.append(page);
            } else {
                builder.append("HTTP/1.1 404 Not Found\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("File not found: www/root.html");
            }

        } else if (request.startsWith("multiply?")) {
            
            Map<String, String> query_pairs = splitQuery(request.replace("multiply?", ""));

            String num1Str = query_pairs.get("num1");
            String num2Str = query_pairs.get("num2");
            Integer num1 = null;
            Integer num2 = null;
            String errorMessage = null;
            String statusCode = "HTTP/1.1 200 OK\n";

            if (num1Str == null || num2Str == null) {
                errorMessage = "Missing required parameters. Please provide 'num1' and 'num2' (e.g., /multiply?num1=3&num2=4).";
                statusCode = "HTTP/1.1 400 Bad Request\n";
            } else {
                try {
                    num1 = Integer.parseInt(num1Str);
                } catch (NumberFormatException e) {
                    errorMessage = "Invalid format for 'num1'. Must be a valid integer.";
                }
                
                try {
                    num2 = Integer.parseInt(num2Str);
                } catch (NumberFormatException e) {
                    if (errorMessage == null) {
                        errorMessage = "Invalid format for 'num2'. Must be a valid integer.";
                    } else {
                        errorMessage = "Invalid format for 'num1' and 'num2'. Both must be valid integers.";
                    }
                }

                if (errorMessage == null) {
                    Integer result = num1 * num2;
                    String page = "<html><body><h1>Result is: " + result + "</h1></body></html>";
                    builder.append(statusCode);
                    builder.append("Content-Type: text/html; charset=utf-8\n");
                    builder.append("Content-Length: " + page.getBytes().length + "\n");
                    builder.append("\n");
                    builder.append(page);
                } else {
                }
            }
            
            if (errorMessage != null) {
                String errorPage = "<html><body><h1>400 Bad Request</h1><p>" + errorMessage + "</p></body></html>";
                builder.append(statusCode);
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("Content-Length: " + errorPage.getBytes().length + "\n");
                builder.append("\n");
                builder.append(errorPage);
            }

        } else if (request.startsWith("github?")) {
            
            Map<String, String> query_pairs = splitQuery(request.replace("github?", ""));
            String errorMessage = null;
            String statusCode = null;
            String content = "";

            try {
                String apiQuery = query_pairs.get("query");
                
                if (apiQuery == null || apiQuery.isEmpty()) {
                    errorMessage = "Missing 'query' parameter for GitHub API request (e.g., /github?query=users/...).";
                    statusCode = "HTTP/1.1 400 Bad Request\n";
                } else {
                    String json = fetchURL("https://api.github.com/" + apiQuery);

                    if (json.contains("\"message\":\"Not Found\"")) {
                        errorMessage = "GitHub resource not found for query: " + apiQuery;
                        statusCode = "HTTP/1.1 404 Not Found\n";
                    } else if (json.startsWith("[")) {
                        statusCode = "HTTP/1.1 200 OK\n";
                        
                        StringBuilder repoList = new StringBuilder("<html><body><h1>GitHub Repository Details</h1><ul>");
                        
                        String repos = json.substring(1, json.length() - 1);
                        String[] repoObjects = repos.split("\\},\\{");

                        for (String repoJson : repoObjects) {
                            if (!repoJson.startsWith("{")) repoJson = "{" + repoJson;
                            if (!repoJson.endsWith("}")) repoJson = repoJson + "}";
                            
                            String full_name = "";
                            String id = "";
                            String owner_login = "";

                            int nameStart = repoJson.indexOf("\"full_name\":\"");
                            if (nameStart != -1) {
                                nameStart += "\"full_name\":\"".length();
                                int nameEnd = repoJson.indexOf("\"", nameStart);
                                if (nameEnd != -1) full_name = repoJson.substring(nameStart, nameEnd);
                            }
                            
                            int idStart = repoJson.indexOf("\"id\":");
                            if (idStart != -1) {
                                idStart += "\"id\":".length();
                                int idEnd = repoJson.indexOf(",", idStart);
                                if (idEnd != -1) id = repoJson.substring(idStart, idEnd).trim();
                            }

                            int ownerStart = repoJson.indexOf("\"owner\":");
                            if (ownerStart != -1) {
                                int loginStart = repoJson.indexOf("\"login\":\"", ownerStart);
                                if (loginStart != -1) {
                                    loginStart += "\"login\":\"".length();
                                    int loginEnd = repoJson.indexOf("\"", loginStart);
                                    if (loginEnd != -1) owner_login = repoJson.substring(loginStart, loginEnd);
                                }
                            }

                            repoList.append("<li>");
                            repoList.append("<strong>Name:</strong> " + full_name);
                            repoList.append(" | <strong>ID:</strong> " + id);
                            repoList.append(" | <strong>Owner:</strong> " + owner_login);
                            repoList.append("</li>");
                        }
                        repoList.append("</ul></body></html>");
                        content = repoList.toString();

                        builder.append(statusCode);
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("Content-Length: " + content.getBytes().length + "\n");
                        builder.append("\n");
                        builder.append(content);

                    } else {
                        errorMessage = "The GitHub API returned data that could not be parsed (not a list of repos).";
                        statusCode = "HTTP/1.1 400 Bad Request\n";
                    }
                }
            } catch (Exception e) {
                errorMessage = "An unexpected server error occurred during the GitHub request or parsing: " + e.getMessage();
                statusCode = "HTTP/1.1 500 Internal Server Error\n";
            }
            
            if (errorMessage != null) {
                String statusNum = statusCode.split(" ")[1];
                String errorPage = "<html><body><h1>" + statusNum + " Error</h1><p>" + errorMessage + "</p></body></html>";
                builder.append(statusCode);
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("Content-Length: " + errorPage.getBytes().length + "\n");
                builder.append("\n");
                builder.append(errorPage);
            }

        } else if (request.startsWith("jsonquery?")) {
            
            Map<String, String> query_pairs = splitQuery(request.replace("jsonquery?", ""));

            String dataStr = query_pairs.get("data");
            String targetKey = query_pairs.get("key");
            String errorMessage = null;
            String resultValue = null;
            String statusCode = null;

            if (dataStr == null || targetKey == null) {
                errorMessage = "Missing required parameters. Please provide 'data' (URL-encoded JSON) and 'key'.";
                statusCode = "HTTP/1.1 400 Bad Request\n";
            } else {
                try {
                    String searchPattern = "\"" + targetKey + "\":";
                    int keyIndex = dataStr.indexOf(searchPattern);
                    
                    if (keyIndex == -1) {
                        errorMessage = "Key '" + targetKey + "' not found in the provided JSON data.";
                        statusCode = "HTTP/1.1 404 Not Found\n";
                    } else {
                        int start = keyIndex + searchPattern.length();
                        int end = dataStr.indexOf(",", start);
                        if (end == -1) {
                            end = dataStr.indexOf("}", start);
                        }
                        
                        if (end != -1) {
                            resultValue = dataStr.substring(start, end).trim();
                            if (resultValue.startsWith("\"") && resultValue.endsWith("\"")) {
                                resultValue = resultValue.substring(1, resultValue.length() - 1);
                            }
                            
                            statusCode = "HTTP/1.1 200 OK\n";
                        } else {
                            errorMessage = "Malformed JSON structure detected after the key.";
                            statusCode = "HTTP/1.1 400 Bad Request\n";
                        }
                    }
                } catch (Exception e) {
                    errorMessage = "Failed to process data. Ensure the JSON is valid and properly URL-encoded.";
                    statusCode = "HTTP/1.1 400 Bad Request\n";
                }
            }

            String content;
            builder.append(statusCode);
            builder.append("Content-Type: text/html; charset=utf-8\n");

            if (errorMessage != null) {
                String statusNum = statusCode.split(" ")[1];
                content = "<html><body><h1>" + statusNum + " Error</h1><p>" + errorMessage + "</p></body></html>";
            } else {
                content = "<html><body><h1>JSON Query Result</h1><p>The value for key '<strong>" + targetKey + "</strong>' is: <strong>" + resultValue + "</strong></p></body></html>";
            }
            builder.append("Content-Length: " + content.getBytes().length + "\n");
            builder.append("\n");
            builder.append(content);

        } else if (request.startsWith("revstring?")) {
            
            Map<String, String> query_pairs = splitQuery(request.replace("revstring?", ""));

            String originalText = query_pairs.get("text");
            String offsetStr = query_pairs.get("offset");
            String errorMessage = null;
            String statusCode = "HTTP/1.1 200 OK\n";
            
            if (originalText == null || offsetStr == null) {
                errorMessage = "Missing required parameters. Please provide 'text' and a numeric 'offset' (e.g., /revstring?text=Engineer&offset=3).";
                statusCode = "HTTP/1.1 400 Bad Request\n";
            } else {
                try {
                    int offset = Integer.parseInt(offsetStr);
                    
                    if (offset < 0) {
                         errorMessage = "Offset must be a positive integer or zero.";
                         statusCode = "HTTP/1.1 400 Bad Request\n";
                    } else if (offset > originalText.length()) {
                        errorMessage = "Offset (" + offset + ") cannot be greater than the length of the string (" + originalText.length() + ").";
                        statusCode = "HTTP/1.1 400 Bad Request\n";
                    } else {
                        String prefix = originalText.substring(0, offset);
                        String suffixToReverse = originalText.substring(offset);
                        
                        String reversedSuffix = new StringBuilder(suffixToReverse).reverse().toString();
                        String finalResult = prefix + reversedSuffix;

                        String page = "<html><body><h1>String Reversal with Offset</h1><p>Result: <strong>" + finalResult + "</strong></p></body></html>";
                        builder.append(statusCode);
                        builder.append("Content-Type: text/html; charset=utf-8\n");
                        builder.append("Content-Length: " + page.getBytes().length + "\n");
                        builder.append("\n");
                        builder.append(page);
                    }
                } catch (NumberFormatException e) {
                    errorMessage = "Invalid 'offset' parameter. It must be a valid integer.";
                    statusCode = "HTTP/1.1 400 Bad Request\n";
                }
            }

            if (errorMessage != null) {
                String errorPage = "<html><body><h1>400 Bad Request</h1><p>" + errorMessage + "</p></body></html>";
                builder.append(statusCode);
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("Content-Length: " + errorPage.getBytes().length + "\n");
                builder.append("\n");
                builder.append(errorPage);
            }
        
        } else if (request.contains("file/")) {
            
            File file = new File(request.replace("file/", ""));

            if (file.exists()) {
                String contentType = getContentType(file);
                byte[] fileBytes = readFileInBytes(file);

                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: " + contentType + "\n");
                builder.append("Content-Length: " + fileBytes.length + "\n");
                builder.append("\n");
                builder.append(new String(fileBytes));
            } else {
                builder.append("HTTP/1.1 404 Not Found\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("File not found: " + file);
            }
            
        } else if (request.equals("json")) {
            
            String jsonContent = "{\"title\": \"Random Image\", \"url\": \"/random\"}";
            
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: application/json; charset=utf-8\n");
            builder.append("Content-Length: " + jsonContent.getBytes().length + "\n");
            builder.append("\n");
            builder.append(jsonContent);

        } else if (request.equals("random")) {
            
            File file = new File("www/index.html");
            if (file.exists()) {
                byte[] fileBytes = readFileInBytes(file);
                
                builder.append("HTTP/1.1 200 OK\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("Content-Length: " + fileBytes.length + "\n");
                builder.append("\n");
                builder.append(new String(fileBytes));
            } else {
                builder.append("HTTP/1.1 404 Not Found\n");
                builder.append("Content-Type: text/html; charset=utf-8\n");
                builder.append("\n");
                builder.append("File not found: www/index.html");
            }
        }
        else {
            
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("<html><body><h1>404 Not Found</h1><p>The requested path was not recognized.</p></body></html>");
        }

        return builder.toString();
    }
    
    private static Map<String, String> splitQuery(String query) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        try {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return query_pairs;
    }

    private static String fetchURL(String urlString) throws IOException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        
        conn.setRequestProperty("User-Agent", "Java/WebServer");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }

    private static byte[] readFileInBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileData = new byte[(int) file.length()];
            fis.read(fileData);
            return fileData;
        }
    }

    private static String getContentType(File file) {
        if (file.getName().endsWith(".html")) return "text/html";
        if (file.getName().endsWith(".css")) return "text/css";
        if (file.getName().endsWith(".js")) return "application/javascript";
        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg")) return "image/jpeg";
        if (file.getName().endsWith(".png")) return "image/png";
        return "text/plain";
    }

    private static String buildFileList(File directory) {
        StringBuilder links = new StringBuilder();
        links.append("<ul>");
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        links.append("<li>" + file.getName() + "</li>");
                    }
                }
            }
        }
        links.append("</ul>");
        return links.toString();
    }
}
