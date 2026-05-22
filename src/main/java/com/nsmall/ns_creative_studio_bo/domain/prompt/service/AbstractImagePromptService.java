package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;

/**
 * 참조 이미지 기반 N개 병렬 이미지 생성을 위한 추상 부모 클래스 (Template Method)
 *
 * <pre>
 * 자식 클래스(상품/인물 프롬프트 서비스)는 도메인별 가이드 프롬프트 워딩만 제공한다.
 * N개 병렬 호출 + CompletableFuture.join() 에서 발생하는 CompletionException 의 BizException unwrap
 * 책임은 모두 본 클래스가 일괄 처리한다.
 * </pre>
 *
 * @author iamjy
 * @since 2026-05-20
 */
public abstract class AbstractImagePromptService {

    /** 자식 클래스가 보유한 ImageGenerationService 인스턴스를 노출 */
    protected abstract ImageGenerationService imageGenerationService();

    /** 도메인별 가이드 프롬프트 합성 — 자식이 구현 */
    protected abstract String buildReferenceGuide(String userPrompt);

    /**
     * 참조 이미지 기반 N개 병렬 생성 + CompletableFuture.join() unwrap 일체화.
     * CompletionException 으로 감싸진 BizException 은 unwrap 후 재throw.
     *
     * @param userPrompt 사용자 프롬프트(가이드 합성 입력)
     * @param bytes      참조 이미지 바이너리
     * @param mimeType   참조 이미지 MIME 타입
     * @param count      병렬 생성 개수
     * @return 생성된 이미지 접근 URL 목록
     */
    protected final List<String> generateImagesWithReference(
            String userPrompt, byte[] bytes, String mimeType, int count) {

        String guide = buildReferenceGuide(userPrompt);
        ImageGenerationService svc = imageGenerationService();

        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(
                        () -> svc.generateImageMime(guide, bytes, mimeType)))
                .toList();
        try {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (CompletionException e) {
            // CompletableFuture 내부에서 발생한 BizException을 unwrap하여 재throw
            // → GlobalExceptionHandler의 BizException 핸들러가 올바르게 처리
            if (e.getCause() instanceof BizException bizEx) {
                throw bizEx;
            }
            throw e;
        }
    }
}
