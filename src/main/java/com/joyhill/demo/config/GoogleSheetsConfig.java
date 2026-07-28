package com.joyhill.demo.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.util.List;

@Configuration
public class GoogleSheetsConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsConfig.class);

    @Value("${google.sheets.credentials-path:}")
    private String credentialsPath;

    // credentials-path가 비어있으면 null 빈으로 등록됨 — 동기화 기능만 비활성화되고 앱 기동에는 영향 없음.
    @Bean
    public Sheets sheetsClient() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("google.sheets.credentials-path가 설정되지 않아 구글시트 동기화 기능이 비활성화됩니다.");
            return null;
        }
        try (FileInputStream in = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS));
            return new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("JOYHILL")
                    .build();
        } catch (Exception e) {
            log.error("구글시트 서비스 계정 인증 초기화 실패 — 동기화 기능이 비활성화됩니다.", e);
            return null;
        }
    }
}
