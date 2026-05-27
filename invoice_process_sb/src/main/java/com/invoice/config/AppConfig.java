package com.invoice.config;

//import io.minio.MinioClient;
//import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

   /* @Value("${app.minio.url}")
    private String minioUrl;*/

    /*@Value("${app.minio.access-key}")
    private String minioAccessKey;

    @Value("${app.minio.secret-key}")
    private String minioSecretKey;*/

    /*@Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }*/
}

