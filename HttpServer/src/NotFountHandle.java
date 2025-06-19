import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;


public class NotFountHandle implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        // 设置响应头
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");

        // 准备 404 响应内容
        String response = "404 Not Found\n";
        byte[] responseBytes = response.getBytes("UTF-8");

        // 发送 404 响应头
        exchange.sendResponseHeaders(404, responseBytes.length);

        // 获取响应输出流并写入响应内容
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);

        // 关闭输出流
        outputStream.close();
    }
}
