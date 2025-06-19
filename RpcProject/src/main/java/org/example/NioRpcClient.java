package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NioRpcClient {
    private final String host;
    private final int port;
    private SocketChannel channel;
    private final AtomicInteger requestId = new AtomicInteger(0);

    public NioRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));

        while (!channel.finishConnect()) {
            // 等待连接完成
            System.out.println("Waiting to connect...");
        }
        System.out.println("Connected to server");
    }

    public String sendRequest(String request) throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.wrap(request.getBytes());
        channel.write(buffer);
        Thread.sleep(1000);

        ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(responseBuffer);

        if (bytesRead > 0) {
            responseBuffer.flip();
            byte[] bytes = new byte[responseBuffer.remaining()];
            responseBuffer.get(bytes);
            return new String(bytes).trim();
        }
        return null;
    }

    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    NioRpcClient client = new NioRpcClient("localhost", 8090);
                    client.connect();

                    for (int j = 0; j < requestsPerThread; j++) {
                        long startTime = System.currentTimeMillis();
                        // 模拟两种请求
                        String request;
                        if (Math.random() > 0.5) {
                            request = "MathService|add|" + j + "," + (j + 1);
                        } else {
                            request = "HelloService|sayHello|User" + j;
                        }

                        String response = client.sendRequest(request);
                        long elapsed = System.currentTimeMillis() - startTime;
                        System.out.println("Request: " + request +
                                ", Response: " + response +
                                ", Time: " + elapsed + "ms");
                    }

                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("All requests completed");
    }
}