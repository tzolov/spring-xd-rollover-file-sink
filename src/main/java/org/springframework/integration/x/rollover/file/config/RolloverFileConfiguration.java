package org.springframework.integration.x.rollover.file.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.x.rollover.file.FileCompressor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Created by bukowm on 08/03/16.
 */
@Configuration
@EnableAsync
public class RolloverFileConfiguration {

    @Value("${xd.stream.name:file-compressor}")
    private String streamName;

    @Value("${rollover.file.thread.poolSize:2}")
    private int poolSize;

    @Value("${rollover.file.thread.maxPoolSize:2}")
    private int maxPoolSize;

    @Value("${rollover.file.thread.queueCapacity:2}")
    private int queueCapacity;

    @Bean
    public FileCompressor fileCompressor() {
        return new FileCompressor();
    }

    @Bean(name = "fileCompressorExecutor")
    public ThreadPoolTaskExecutor fileCompressorExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        taskExecutor.setThreadNamePrefix(streamName + "-");

        taskExecutor.setCorePoolSize(this.poolSize);
        taskExecutor.setMaxPoolSize(this.maxPoolSize);
        taskExecutor.setQueueCapacity(this.queueCapacity);

        return taskExecutor;
    }

}
