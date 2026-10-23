package app.g_agent.policy_service.service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;

@Service
public class B2ClientFactory {

    @Value("${backblaze.s3.endpoint-url}")
    private String ENDPOINT_URL;
    @Value("${backblaze.s3.bucket-name}")
    private String BUCKETNAME;

    private final Logger logger = LoggerFactory.getLogger(B2ClientFactory.class);
    private final String profileName = "gisca";

    public S3Client createClient() {

        Matcher matcher = Pattern.compile("https://s3\\.([a-z0-9-]+)\\.backblazeb2\\.com")
                .matcher(ENDPOINT_URL.trim());

        String region = matcher.find() ? matcher.group(1) : null;
        if (region == null) {
            logger.error("Can't find a region in the endpoint URL: " + ENDPOINT_URL);
            return null;
        }

        try {
            return S3Client.builder()
                    .endpointOverride(new URI(ENDPOINT_URL))
                    .region(Region.of(region))
                    .credentialsProvider(ProfileCredentialsProvider.create(profileName))
                    .build();
        } catch (Exception e) {
            logger.error("Error uploading file to S3: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private S3Presigner createPresigner() {
        Matcher matcher = Pattern.compile("https://s3\\.([a-z0-9-]+)\\.backblazeb2\\.com")
                .matcher(ENDPOINT_URL.trim());

        String region = matcher.find() ? matcher.group(1) : null;
        if (region == null) {
            logger.error("Can't find a region in the endpoint URL: " + ENDPOINT_URL);
            return null;
        }

        try {
            return S3Presigner.builder()
                    .endpointOverride(new URI(ENDPOINT_URL))
                    .region(Region.of(region))
                    .credentialsProvider(ProfileCredentialsProvider.create(profileName))
                    .build();
        } catch (Exception e) {
            logger.error("Error creating S3 presigner: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public String getPreSignedUrl(String bucketUrl) {

        if (bucketUrl != null && !bucketUrl.isEmpty()) {

            String path = bucketUrl.substring(bucketUrl.indexOf(".com/") + 5);
            int firstSlash = path.indexOf('/');
            String key = path.substring(firstSlash + 1);

            // create presigner
            S3Presigner presigner = this.createPresigner();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(BUCKETNAME)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(Duration.ofMinutes(120)) // URL valid for 10 minutes
                    .build();

            String preSignedUrl = presigner.presignGetObject(presignRequest).url().toString();
            presigner.close();
            return preSignedUrl;
        } else {
            return "";
        }
    }

}