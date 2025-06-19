package org.example;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
public class SentinelRuleConfig {
    @PostConstruct
    public void initRules() {
        List<FlowRule> flowRules = new ArrayList<>();

        //限流规则，QPS最多5次每秒
        FlowRule flowRule = new FlowRule("process")
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(5) // 阈值5 QPS
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_WARM_UP) // 预热模式
                .setWarmUpPeriodSec(10); // 预热时间10秒
        flowRules.add(flowRule);

        // 熔断规则，10秒内20次请求，异常比例大于0.5触发熔断，熔断持续5秒
        List<DegradeRule> degradeRules = new ArrayList<>();
        DegradeRule degradeRule = new DegradeRule("process")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setCount(0.5) // 异常比例50%
                .setTimeWindow(5) // 熔断时间5秒
                .setMinRequestAmount(10); // 最小请求数
        degradeRules.add(degradeRule);
        //加载规则到sentinel
        FlowRuleManager.loadRules(flowRules);
        DegradeRuleManager.loadRules(degradeRules);
    }
}
