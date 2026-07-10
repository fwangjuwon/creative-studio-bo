package com.nsmall.ns_creative_studio_bo.domain.prompt.service;

import com.nsmall.ns_creative_studio_bo.common.exception.BizException;
import com.nsmall.ns_creative_studio_bo.domain.prompt.ImageGenerationErrorCode;
import com.nsmall.ns_creative_studio_bo.domain.prompt.util.ImageValidationUtil.ValidatedImage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
@Slf4j
public abstract class AbstractImagePromptService {

    private static final long IMAGE_GENERATION_TIMEOUT_SECONDS = 120;

    /** 자식 클래스가 보유한 ImageGenerationService 인스턴스를 노출 */
    protected abstract ImageGenerationService imageGenerationService();

    /** 도메인별 가이드 프롬프트 합성 — 자식이 구현 */
    protected abstract String buildReferenceGuide(String userPrompt);

    /**
     * 참조 이미지 기반 N개 병렬 생성 + CompletableFuture.join() unwrap 일체화.
     * CompletionException 으로 감싸진 BizException 은 unwrap 후 재throw.
     *
     * @param userPrompt 사용자 프롬프트(가이드 합성 입력)
     * @param images           참조 이미지 목록 (1장 이상)
     * @param count            병렬 생성 개수
     * @param imageAspectRatio 사용자가 선택한 이미지 비율 태그값 (nullable) — 초광각/초장축 비율이면
     *                         모델은 기본 해상도로 생성 후 Java에서 목표 해상도로 업스케일한다
     * @return 생성된 이미지 접근 URL 목록
     */
    protected final List<String> generateImagesWithReference(
            String userPrompt, List<ValidatedImage> images, int count, String imageAspectRatio) {

        String guide = buildReferenceGuide(userPrompt);
        ImageGenerationService svc = imageGenerationService();

        CopyOnWriteArrayList<String> savedUrls = new CopyOnWriteArrayList<>();
        List<CompletableFuture<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String url = svc.generateImageMime(guide, images, imageAspectRatio);
                    savedUrls.add(url);
                    return url;
                }).orTimeout(IMAGE_GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                .toList();
        try {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (CompletionException e) {
            savedUrls.forEach(svc::deleteImageFile);
            if (e.getCause() instanceof BizException bizEx) throw bizEx;
            if (e.getCause() instanceof TimeoutException) {
                log.error("참조 이미지 기반 이미지 생성 타임아웃 ({}s 초과)", IMAGE_GENERATION_TIMEOUT_SECONDS);
                throw new BizException(ImageGenerationErrorCode.IMAGE_GENERATION_FAILED);
            }
            throw e;
        }
    }
}
