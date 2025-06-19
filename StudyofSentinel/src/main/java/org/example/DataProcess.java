package org.example;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Random;

@RestController
public class DataProcess {
    private static final Logger log = LoggerFactory.getLogger(DataProcess.class);
    private final Random rand = new Random();
    @GetMapping("/api/data/process")
    @SentinelResource(
            value = "process",
            blockHandler = "handleBlock",//限流处理方法
            fallback = "handleFallBack" //熔断异常处理方法
    )
    public ApiAnswer process(HttpServletRequest request) throws InterruptedException {
        //记录请求开始时间和线程名
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        //记录请求的基本信息
        log.info("请求开始 | 时间：{} | 线程：{} | 接口：{}", new Date(startTime), threadName, request.getRequestURI());
        try {
            //模拟业务逻辑，随机睡眠2-3秒
            int delay = rand.nextInt(201) + 100;
            Thread.sleep(delay);

            //模拟业务执行成功或者抛出异常
            if (rand.nextBoolean()) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("请求成功 | 耗时：{}", duration);
                return ApiAnswer.success();
            } else {
                throw new RuntimeException("服务异常");
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            log.error("请求中断 | 时间: {} | 线程: {} | 接口: {} | 耗时: {}ms | 错误: {}",
                    new Date(), threadName, request.getRequestURI(), duration, e.getMessage());
            return ApiAnswer.error(500, "请求失败");
        }
    }

    //限流处理方法
    public ApiAnswer handleBlock(HttpServletRequest request, BlockException ex) {
        String threadName = Thread.currentThread().getName();

        //记录限流日志
        log.warn("发生请求限流 | 时间: {} | 线程: {} | 接口: {} ",
                new Date(), threadName, request.getRequestURI());
        return ApiAnswer.error(429, "请求过多，请稍后再试");
    }

    //熔断处理方法
    public ApiAnswer handleFallBack(HttpServletRequest request, Throwable t) {
        String threadName = Thread.currentThread().getName();

        //记录熔断日志
        log.error("发生请求熔断 | 时间: {} | 线程: {} | 接口: {} ",
                new Date(), threadName, request.getRequestURI());
        return ApiAnswer.error(503, "服务繁忙，暂时无法处理请求");
    }

}
