package com.itsm.boot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ITSM 启动器。
 * 组件扫描范围:业务模块(module.*)与引擎(engine);
 * 三大可插拔 starter 通过 AutoConfiguration.imports 装配(条件装配,见设计文档 10.2)。
 */
@SpringBootApplication(scanBasePackages = {"com.itsm.boot", "com.itsm.engine", "com.itsm.module"})
@ConfigurationPropertiesScan("com.itsm")
@MapperScan({
        "com.itsm.module.platform.mapper",
        "com.itsm.module.ticket.mapper",
        "com.itsm.module.catalog.mapper",
        "com.itsm.module.knowledge.mapper",
        "com.itsm.module.report.mapper"
})
public class ItsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItsmApplication.class, args);
    }
}

