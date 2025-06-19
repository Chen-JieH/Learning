import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 处理/hello 路径的处理器
 */

public class SimpleHttpHandle implements HttpHandler{
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            Headers headers = httpExchange.getRequestHeaders();
            // 直接获取第一个 Content-Type 的值（如果存在）
            String contentType = headers.getFirst("Content-Type");
            StringBuilder response = new StringBuilder();
            response.append("Hello World!").append("<br/>");
            response.append("请求方法：").append(httpExchange.getRequestMethod()).append("<br/>");
            response.append("body：").append(getParameter(httpExchange)).append("<br/>");
            response.append("header:").append(getHeader(httpExchange)).append("<br/>");
            response.append("record:").append(logRequest(httpExchange)).append("<br/>");
            if (contentType != null) {
                response.append("Content-Type: ").append(contentType).append("<br/>");
            }
            handleResponse(httpExchange, response.toString());
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void handleResponse(HttpExchange httpExchange, String string) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("<html/>")
                .append("<body>")
                .append(string)
                .append("</body>")
                .append("</html>");
        String responseContent = content.toString();
        byte[] responseBytes = responseContent.getBytes("utf-8");
        httpExchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        httpExchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = httpExchange.getResponseBody();
        os.write(responseBytes);
        os.flush();
        if (!"keep-alive".equals(httpExchange.getRequestHeaders().getFirst("Connection"))) {
            os.close();
            System.out.println("close!");
        }
    }

    /**
     * 获取请求Header
     * @param httpExchange
     * @return
     */
    private String getHeader(HttpExchange httpExchange) {
        Headers headers = httpExchange.getRequestHeaders();
        return headers.entrySet().stream()
                .map((Map.Entry<String, List<String>> entry) ->entry.getKey() + ":" + entry.getValue().toString())
                .collect(Collectors.joining("<br/>"));
    }

    /**
     * 获取Body
     * @param httpExchange
     * @return
     * @throws IOException
     */
    private String getParameter(HttpExchange httpExchange) throws IOException {
        String paramStr = "";
        if (httpExchange.getRequestMethod().equals("GET")) {
            paramStr = httpExchange.getRequestURI().getQuery();
        } else {
            BufferedReader br = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), "utf-8"));
            StringBuilder str = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                str.append(line);
            }
            paramStr = str.toString();
        }
        return paramStr;
    }

    /**
     * 获取请求时间，IP，UA的日志方法
     * @param httpExchange
     * @return
     * @throws IOException
     */
    private String logRequest(HttpExchange httpExchange) throws IOException {
        StringBuilder record = new StringBuilder();
        record.append("请求时间：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("<br/>");
        record.append("请求IP：").append(httpExchange.getRemoteAddress().getAddress().getHostAddress()).append("<br/>");
        record.append("请求路径：").append(httpExchange.getRequestURI().getPath()).append("<br/>");
        record.append("UA：").append(httpExchange.getRequestHeaders().getFirst("User-Agent")).append("<br/>");
        return record.toString();
    }
}
