package org.example;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SelfDefinedThreadPool {

    //核心线程数和最大线程数等参数配置
    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 10;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 50;

    //任务配置
    private static final int TASK_PER_SECOND = 10;
    private static final int DURATION_SECOND = 30;
    private static final int TOTAL_TASK = TASK_PER_SECOND * DURATION_SECOND;

    //统计变量
    private static final AtomicInteger completedTask = new AtomicInteger(0);
    private static final AtomicInteger rejectedTask = new AtomicInteger(0);
    private static final AtomicInteger maxActiveTask = new AtomicInteger(0);
    private static final AtomicInteger maxPoolSize = new AtomicInteger(0);
    private static final AtomicLong totalExecutionTime = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(QUEUE_CAPACITY),
                new CustomThreadFactory(),
                new CustomRejectedExecutionHandler()
        );

        //启动一个单线程的定时任务执行器作为监视器，每秒打印线程池的状态
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(() -> printStats(executor), 0, 1, TimeUnit.SECONDS);

        System.out.println("Strating to submit tasks");
        ScheduledExecutorService submit = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger taskId = new AtomicInteger(1);

        //提交任务
        submit.scheduleAtFixedRate(() -> {
            for (int i = 0; i < TASK_PER_SECOND; i++) {
                int curId = taskId.getAndIncrement();
                executor.execute(new Task(curId));
            }
        }, 0, 1, TimeUnit.SECONDS);

        //等待所有提交的任务执行完成，停止提交新的任务
        Thread.sleep(DURATION_SECOND * 1000);
        submit.shutdown();

        //循环检查任务是否完成
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(500);
        }

        //关闭监视器
        monitor.shutdown();
        printFinalStats(executor);

    }

    //自定义线程工厂
    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "my-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    //自定义拒绝策略
    static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (r instanceof Task) {
                Task task = (Task) r;
                rejectedTask.incrementAndGet();
                System.out.printf("Rejected: Task ID: %d, Time: %s%n", task.taskId, System.currentTimeMillis());
            }
        }
    }

    //自定义任务类
    static class Task implements Runnable {
        private final int taskId;
        private final long startTime;

        public Task(int taskId) {
            this.taskId = taskId;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                //模拟任务执行
                int executionTime = 1000 + (int) (Math.random() * 2000);
                Thread.sleep(executionTime);

                //记录执行时间
                long endTime = System.currentTimeMillis();
                totalExecutionTime.addAndGet(endTime - startTime);

                //打印任务的相关信息
                System.out.printf("[Task] ID: %d, Thread: %d, Time: %dms%n", taskId, Thread.currentThread().getId(), endTime - startTime);

                //更新统计信息
                completedTask.incrementAndGet();

                //检查更新活跃线程数
                int activeCount = Thread.activeCount();
                if (activeCount > maxActiveTask.get()) {
                    maxActiveTask.set(activeCount);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    //打印线程池的状态
    private static void printStats(ThreadPoolExecutor executor) {
        int poolSize = executor.getPoolSize();
        int activeCount = executor.getActiveCount();
        long completed = executor.getCompletedTaskCount();
        int queueSize = executor.getQueue().size();

        if (poolSize > maxPoolSize.get()) {
            maxPoolSize.set(poolSize);
        }

        System.out.printf("[Monitor] pool size: %d, active: %d, completed: %d, queue size: %d%n",
                poolSize, activeCount, completed, queueSize);

    }

    //打印最终统计信息
    private static void printFinalStats(ThreadPoolExecutor executor) {
        System.out.println("\n=====Final Stats =====");
        System.out.println("Total completed: " + completedTask.get());
        System.out.println("Total tasks submitted: " + TOTAL_TASK);
        System.out.println("Total rejected: " + rejectedTask.get());

        if (completedTask.get() > 0) {
            double avgTime = totalExecutionTime.get() / (double) completedTask.get();
            System.out.printf("Avg time: %.2fms%n", avgTime);
        }

        System.out.println("Maximun active threads:" + maxActiveTask.get());
        System.out.println("Maximun pool size:" + maxPoolSize.get());
        System.out.println("Final queue size:" + executor.getQueue().size());
    }

}
