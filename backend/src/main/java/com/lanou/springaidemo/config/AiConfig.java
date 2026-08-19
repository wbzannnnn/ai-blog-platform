package com.lanou.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public ChatMemoryRepository createChatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 内存对话记忆
     */
    @Bean
    public MessageWindowChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10) //保留最近10条对话，防止token溢出
                .build();
    }

    /**
     * 自定义 RestClient，配置超时时间
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时：60秒
        factory.setConnectTimeout((int) Duration.ofSeconds(60).toMillis());
        // 读取超时：120秒
        factory.setReadTimeout((int) Duration.ofSeconds(120).toMillis());
        return RestClient.builder().requestFactory(factory);
    }

    /**
     * 注入ChatClient对象（支持Tool Calling）
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder, 
            MessageWindowChatMemory chatMemory) {
        
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

}
