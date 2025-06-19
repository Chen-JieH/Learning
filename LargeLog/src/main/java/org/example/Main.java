package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    private static final String logFileName = "large.log";
    private static final long fileSize = 500 * 1024 * 1024;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static void main(String[] args) {
        try {
            //创建大日志
            createLogFile();
            //测试两种读取日志的方式
            readLargeLogA();
            readLargeLogB(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //创建大日志文件
    private static void createLogFile() throws IOException {
        File logFile = new File(logFileName);
        if (logFile.exists() && logFile.length() > fileSize) {
            System.out.println("log file already exists!");
            return;
        }

        System.out.println("Creating large log file...");
        long startTime = System.currentTimeMillis();

        //日志样例
        String[] sampleLogs = {
                " INFO User login success, userId=%d",
                " ERROR Failed to connect to database",
                " WARN Disk space low, only %dMB remaining",
                " DEBUG Processing request ID %d",
                " INFO Session created for user %d"
        };

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(logFileName), StandardCharsets.UTF_8)){
            long writen = 0;
            long count = 0;
            while (writen < fileSize) {
                LocalDateTime now = LocalDateTime.now();
                String timestamp = now.format(formatter);

                //随机选择记录的日志类型
                String logType = sampleLogs[(int) (count % sampleLogs.length)];
                String logLine = timestamp + String.format(logType, count)+ "\n";
                writer.write(logLine);
                count++;
                writen += logLine.getBytes(StandardCharsets.UTF_8).length;
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.printf("log file created. size: %.2fMB time: %d ms%n",
                logFile.length() / (1024.0 * 1024.0), (endTime - startTime) / 1000);

    }

    //一次性读取整个日志文件
    private static void readLargeLogA() {

        //记录读取开始时间和此时占用内存
        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        try {
            //读取整个日志文件
            String content = new String(Files.readAllBytes(Paths.get(logFileName)), StandardCharsets.UTF_8);

            //将文件分割成行，并读取前10行内容
            String[] lines = content.split("\n");
            System.out.println("\n First 10 lines of logFile");
            for (int i = 0; i < Math.min(10, lines.length); i++){
                System.out.println(lines[i]);
            }

            long endTime = System.currentTimeMillis();
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            //输出读取方法耗费的时间，占用内存等信息
            System.out.println("[Method A] Total time: " + (endTime - startTime) + " ms");
            System.out.println("[Method A] Peak memory: " + (endMemory - startMemory) / (1024 * 1024) + "MB");
            System.out.println("[Method A] Exception: None");
        } catch (OutOfMemoryError e) {
            long endTime = System.currentTimeMillis();
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            System.out.println("[Method A] Total time: " + (endTime - startTime) + " ms");
            System.out.println("[Method A] Peak memory: " + (endMemory - startMemory) / (1024 * 1024) + "MB");
            System.out.println("[Method A] Exception: " + e.toString());
        } catch (Exception e) {
            System.out.println("[Method A] Exception: " + e.toString());
        }
    }

    //分页流式读取方法
    private static void readLargeLogB(int linesToRead) {
         long startTime = System.currentTimeMillis();
         long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

         try (BufferedReader reader = Files.newBufferedReader(Paths.get(logFileName),StandardCharsets.UTF_8)) {
             List<String> lines = new ArrayList<>();
             String line;
             int count = 0;
             System.out.println("\n First 10 lines of logFile");
             while ((line = reader.readLine()) != null && count < linesToRead){
                 if (count < 10){
                     System.out.println(line);
                 }
                 lines.add(line);
                 count++;
             }
             long endTime = System.currentTimeMillis();
             long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

             System.out.println("[Method B] Total time: " + (endTime - startTime) + " ms");
             System.out.println("[Method B] Peak memory: " + (endMemory - startMemory) / (1024 * 1024) + "MB");
             System.out.println("[Method B] Read lines: " + lines.size());
             System.out.println("[Method B] Exception: None");
         } catch (Exception e) {
             System.out.println("[Method B] Exception: " + e.toString());
         }
    }
}