package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

public class NioRpcServer {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final Map<String, Object> serviceRegistry = new HashMap<>();
    private final ExecutorService businessThreadPool;

    public NioRpcServer(int port) {
        this.port = port;
        this.businessThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started on port " + port);

        while (true) {
            selector.select();
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                }
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        System.out.println("New client connected: " + channel.getRemoteAddress());
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = channel.read(buffer);

        if (read == -1) {
            System.out.println("Client disconnected: " + channel.getRemoteAddress());
            channel.close();
            key.cancel();
            return;
        }

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        String request = new String(bytes).trim();
        System.out.println("Received request: " + request);

        // 使用线程池处理业务逻辑
        businessThreadPool.execute(() -> {
            try {
                String response = handleRequest(request);
                byte[] req =response.getBytes();
                ByteBuffer byteBuffer = ByteBuffer.allocate(req.length);
                byteBuffer.put(req);
                byteBuffer.flip();
//                ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                channel.write(byteBuffer);
            } catch (Exception e) {
                System.out.println("exception: " + e);
                e.printStackTrace();
                try {
                    channel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private String handleRequest(String request) {
        String[] parts = request.split("\\|");
        if (parts.length != 3) {
            return "error=Invalid request format";
        }

        String serviceName = parts[0];
        String methodName = parts[1];
        String params = parts[2];

        Object service = serviceRegistry.get(serviceName);
        if (service == null) {
            return "error=Service not found";
        }

        try {
            // 简单模拟方法调用
            if (service instanceof MathService) {
                MathService mathService = (MathService) service;
                if ("add".equals(methodName)) {
                    String[] nums = params.split(",");
                    int a = Integer.parseInt(nums[0]);
                    int b = Integer.parseInt(nums[1]);
                    int result = mathService.add(a, b);
                    return "result=" + result;
                }
            } else if (service instanceof HelloService) {
                HelloService helloService = (HelloService) service;
                if ("sayHello".equals(methodName)) {
                    String greeting = helloService.sayHello(params);
                    return "result=" + greeting;
                }
            }
            return "error=Method not found";
        } catch (Exception e) {
            return "error=" + e.getMessage();
        }
    }

    public void registerService(String serviceName, Object service) {
        serviceRegistry.put(serviceName, service);
    }

    public static void main(String[] args) throws IOException {
        NioRpcServer server = new NioRpcServer(8090);
        server.registerService("MathService", new MathServiceImpl());
        server.registerService("HelloService", new HelloServiceImpl());
        server.start();
    }
}

// 服务接口
interface MathService {
    int add(int a, int b);
}

interface HelloService {
    String sayHello(String name);
}

// 服务实现
class MathServiceImpl implements MathService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}

class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}