import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    public static void main(String[] args) throws IOException {
        //创建http服务并绑定对应的端口
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        //创建一个HttpContext，将路径为/hello的请求映射到自定义的处理器
        httpServer.createContext("/hello", new SimpleHttpHandle());
        //创建不存在路径的404响应处理器
        httpServer.createContext("/", new NotFountHandle());
        //设置服务器的线程池对象
        httpServer.setExecutor(Executors.newFixedThreadPool(10));
        //启动服务器
        httpServer.start();
    }
}