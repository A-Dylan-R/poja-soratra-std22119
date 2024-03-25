package hei.school.soratra.endpoint.soratra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/soratra")
public class SoratraController {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    @Autowired
    public SoratraController(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @PutMapping("/{id}")
    public void putSoratra(@PathVariable String id, @RequestBody String phrase) {
        // Sauvegarde de la phrase en minuscule
        String lowercasePhrase = phrase.toLowerCase();
        byte[] bytes = lowercasePhrase.getBytes();
        String bucketName = "soratra-bucket";
        String key = id + ".txt";
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));
    }

    @GetMapping("/{id}")
    public SoratraResponse getSoratra(@PathVariable String id) {
        // Génération des URLs pré-signées
        String bucketName = "soratra-bucket";
        String originalKey = id + ".txt";
        String transformedKey = id + "-transformed.txt";

        Instant expirationDate = Instant.now().plusSeconds(3600); // 1 heure

        GetObjectPresignRequest originalRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(b -> b.bucket(bucketName).key(originalKey))
                .signatureDuration(Duration.ofHours(1))
                .build();
        PresignedGetObjectRequest originalPresignedRequest = s3Presigner.presignGetObject(originalRequest);
        URL originalUrl = originalPresignedRequest.url();

        GetObjectPresignRequest transformedRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(b -> b.bucket(bucketName).key(transformedKey))
                .signatureDuration(Duration.ofHours(1))
                .build();
        PresignedGetObjectRequest transformedPresignedRequest = s3Presigner.presignGetObject(transformedRequest);
        URL transformedUrl = transformedPresignedRequest.url();

        // Création de la réponse
        SoratraResponse response = new SoratraResponse();
        response.setOriginalUrl(originalUrl.toString());
        response.setTransformedUrl(transformedUrl.toString());

        return response;
    }

    // Classe pour la réponse JSON
    public static class SoratraResponse {
        private String originalUrl;
        private String transformedUrl;

        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getTransformedUrl() {
            return transformedUrl;
        }

        public void setTransformedUrl(String transformedUrl) {
            this.transformedUrl = transformedUrl;
        }
    }
}
