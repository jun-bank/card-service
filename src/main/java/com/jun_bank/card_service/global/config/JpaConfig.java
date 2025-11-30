package com.jun_bank.card_service.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 설정
 * - JPA Auditing 활성화 (createdAt, updatedAt, createdBy, updatedBy 자동 설정)
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}