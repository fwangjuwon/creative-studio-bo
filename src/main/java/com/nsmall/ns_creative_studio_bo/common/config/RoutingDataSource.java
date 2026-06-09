package com.nsmall.ns_creative_studio_bo.common.config;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * projectName  : ns-creative-studio-bo
 * packageName  : com.nsmall.ns_creative_studio_bo.common.config
 * fileName     : RoutingDataSource
 * usage        : 읽기 전용 트랜잭션 여부에 따라 실시간으로 데이터소스를 선택
 * author       : bbbtw21@calltech.kr
 * since        : 2026-06-08
 * history
 * ==================================================================
 * DATE             AUTHOR                      DESC.
 * ==================================================================
 * 2026-06-08      bbbtw21@calltech.kr          생성
 */
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected @Nullable Object determineCurrentLookupKey() {
        // 현재 트랜잭션이 readOnly이면 secondary, 아니면 primary 반환
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? "secondary" : "primary";
    }
}
