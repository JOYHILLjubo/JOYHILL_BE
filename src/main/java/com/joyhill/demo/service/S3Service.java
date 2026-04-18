package com.joyhill.demo.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    // 리사이징 기준 — 공지 이미지는 가로 최대 1080px, 품질 80%
    private static final int MAX_WIDTH = 1920;
    private static final double QUALITY = 0.95;
    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024; // 원본 30MB까지 허용

    private final S3Client s3Client;
    private final String bucket;
    private final String region;

    public S3Service(S3Client s3Client,
                     @Value("${aws.s3.bucket}") String bucket,
                     @Value("${aws.s3.region}") String region) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
    }

    /**
     * 이미지 리사이징 후 S3 업로드 → public URL 반환
     */
    public String upload(MultipartFile file, String folder) {
        validateImageFile(file);

        byte[] compressed = compressImage(file);

        String ext = "jpg"; // 리사이징 후 항상 JPEG로 저장
        String key = folder + "/" + UUID.randomUUID() + "." + ext;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/jpeg")
                .contentLength((long) compressed.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(compressed));

        return getPublicUrl(key);
    }

    /**
     * S3 오브젝트 삭제 (URL에서 key 추출)
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        if (!imageUrl.startsWith(prefix)) return;

        String key = imageUrl.substring(prefix.length());
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    /**
     * Thumbnailator로 리사이징 + 압축
     * - 가로가 MAX_WIDTH 초과 시 비율 유지하며 축소
     * - 가로가 MAX_WIDTH 이하면 크기 변경 없이 품질만 압축
     */
    private byte[] compressImage(MultipartFile file) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(file.getInputStream())
                    .width(MAX_WIDTH)          // 가로 최대 1080px, 세로 비율 자동
                    .outputFormat("jpg")
                    .outputQuality(QUALITY)    // 품질 80%
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("이미지 압축 중 오류가 발생했습니다.", e);
        }
    }

    private String getPublicUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("이미지 크기는 10MB 이하여야 합니다.");
        }
    }
}
