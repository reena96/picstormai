# Story 2.14: Upload Integration Tests

**Epic**: Epic 2 - Core Upload Experience
**Phase**: Phase C - Network Resilience (Weeks 5-6)
**Status**: Draft
**Priority**: High
**Estimated Effort**: 3 days

---

## User Story

**As a** QA engineer
**I want to** validate complete upload flow from client to cloud
**So that** critical upload path is tested end-to-end

---

## Acceptance Criteria

### AC1: Basic Upload Test
**Given** upload system is complete
**When** I run basic upload test
**Then** 10 photos upload to S3 successfully
**And** database metadata matches S3 objects
**And** all photos have status=COMPLETED

### AC2: Concurrent Upload Test
**Given** 100 photos selected
**When** upload starts with 10 concurrent connections
**Then** all 100 photos complete successfully
**And** max 10 uploads active at any time
**And** no photos lost or duplicated

### AC3: Multipart Upload Test
**Given** 20MB test file
**When** upload starts
**Then** file is split into 4 chunks (5MB each)
**And** all chunks upload successfully
**And** S3 object is 20MB and matches original

### AC4: Performance Benchmark
**Given** 100 photos (2MB each, 200MB total)
**When** upload completes
**Then** total time is <90 seconds
**And** average upload speed is >2MB/s

### AC5: WebSocket Progress Test
**Given** WebSocket client connected
**When** photo upload completes
**Then** PHOTO_UPLOADED message received within 500ms
**And** message includes correct progress data

### AC6: Network Resilience Test
**Given** upload in progress
**When** network disconnects for 10 seconds
**Then** uploads pause automatically
**And** after network restores, uploads resume
**And** all photos eventually complete

### AC7: Retry Logic Test
**Given** upload fails with 500 error
**When** auto-retry occurs
**Then** upload retries 3 times with exponential backoff
**And** after 3 failures, marked as failed

### AC8: Resume Test
**Given** 10MB multipart upload interrupted after chunk 1
**When** upload resumes
**Then** only chunk 2 is uploaded
**And** chunk 1 is NOT re-uploaded

---

## Technical Notes

### Test Infrastructure Setup

```java
// UploadIntegrationTest.java
@SpringBootTest
@Testcontainers
@AutoConfigureWebTestClient
public class UploadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("picstorm_test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    )
        .withServices(LocalStackContainer.Service.S3)
        .withEnv("DEFAULT_REGION", "us-east-1");

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UploadSessionRepository sessionRepository;

    @BeforeEach
    void setup() {
        // Create S3 bucket
        s3Client.createBucket(req -> req.bucket("test-upload-bucket"));
    }
}
```

### Test 1: Basic Upload Flow

```java
@Test
void testBasicUploadFlow() {
    // 1. Create upload session
    var sessionResponse = webClient.post()
        .uri("/api/upload/sessions")
        .header("Authorization", "Bearer " + getTestJwt())
        .bodyValue(new StartUploadSessionRequest(10, 20_000_000L))
        .exchange()
        .expectStatus().isOk()
        .expectBody(StartUploadSessionResponse.class)
        .returnResult()
        .getResponseBody();

    String sessionId = sessionResponse.sessionId();

    // 2. Initiate 10 photo uploads
    List<String> photoIds = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        var photoResponse = webClient.post()
            .uri("/api/upload/sessions/{sessionId}/photos/initiate", sessionId)
            .bodyValue(new InitiatePhotoUploadRequest(
                "photo" + i + ".jpg",
                2_000_000L,
                "image/jpeg"
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBody(InitiatePhotoUploadResponse.class)
            .returnResult()
            .getResponseBody();

        photoIds.add(photoResponse.photoId());

        // 3. Upload to S3 using pre-signed URL
        uploadToS3(photoResponse.uploadUrl(), generateTestImage(2_000_000));
    }

    // 4. Mark photos as completed
    photoIds.forEach(photoId -> {
        webClient.post()
            .uri("/api/upload/sessions/{sessionId}/photos/{photoId}/complete",
                sessionId, photoId)
            .exchange()
            .expectStatus().isOk();
    });

    // 5. Verify database metadata
    List<Photo> photos = photoRepository.findBySessionId(new UploadSessionId(sessionId))
        .collectList()
        .block();

    assertThat(photos).hasSize(10);
    assertThat(photos).allMatch(p -> p.getUploadStatus() == UploadStatus.COMPLETED);

    // 6. Verify S3 objects exist
    photos.forEach(photo -> {
        HeadObjectResponse headResponse = s3Client.headObject(req -> req
            .bucket(photo.getS3Location().getBucket())
            .key(photo.getS3Location().getKey())
        );
        assertThat(headResponse.contentLength()).isEqualTo(2_000_000L);
    });
}
```

### Test 2: Concurrent Upload Test

```java
@Test
void testConcurrentUploads() {
    // Upload 100 photos concurrently
    String sessionId = createUploadSession(100, 200_000_000L);

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
        int photoIndex = i;
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Initiate upload
            var photoResponse = initiatePhotoUpload(
                sessionId,
                "photo" + photoIndex + ".jpg",
                2_000_000L
            );

            // Upload to S3
            uploadToS3(photoResponse.uploadUrl(), generateTestImage(2_000_000));

            // Complete upload
            completePhotoUpload(sessionId, photoResponse.photoId());
        });
        futures.add(future);
    }

    // Wait for all uploads to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .join();

    // Verify all completed
    UploadSession session = sessionRepository.findById(new UploadSessionId(sessionId))
        .block();

    assertThat(session.getUploadedPhotos()).isEqualTo(100);
    assertThat(session.getFailedPhotos()).isEqualTo(0);
    assertThat(session.getProgressPercent()).isEqualTo(100.0);
}
```

### Test 3: Multipart Upload Test

```java
@Test
void testMultipartUpload() {
    String sessionId = createUploadSession(1, 20_000_000L);

    // Initiate multipart upload for 20MB file
    var photoResponse = initiatePhotoUpload(
        sessionId,
        "large-photo.jpg",
        20_000_000L
    );

    assertThat(photoResponse.uploadId()).isNotNull();
    assertThat(photoResponse.partUrls()).hasSize(4); // 20MB / 5MB = 4 parts

    // Upload each part
    byte[] testFile = generateTestImage(20_000_000);
    List<CompletedPart> completedParts = new ArrayList<>();

    for (int i = 0; i < 4; i++) {
        int start = i * 5_000_000;
        int end = Math.min((i + 1) * 5_000_000, 20_000_000);
        byte[] part = Arrays.copyOfRange(testFile, start, end);

        String etag = uploadToS3(photoResponse.partUrls().get(i), part);
        completedParts.add(new CompletedPart(i + 1, etag));
    }

    // Complete multipart upload
    webClient.post()
        .uri("/api/upload/sessions/{sessionId}/photos/{photoId}/complete-multipart",
            sessionId, photoResponse.photoId())
        .bodyValue(new CompleteMultipartUploadRequest(
            photoResponse.uploadId(),
            completedParts
        ))
        .exchange()
        .expectStatus().isOk();

    // Verify S3 object
    Photo photo = photoRepository.findById(new PhotoId(photoResponse.photoId()))
        .block();

    HeadObjectResponse headResponse = s3Client.headObject(req -> req
        .bucket(photo.getS3Location().getBucket())
        .key(photo.getS3Location().getKey())
    );

    assertThat(headResponse.contentLength()).isEqualTo(20_000_000L);
}
```

### Test 4: Performance Benchmark

```java
@Test
void testPerformanceBenchmark() {
    String sessionId = createUploadSession(100, 200_000_000L);

    Instant startTime = Instant.now();

    // Upload 100 photos (2MB each)
    List<CompletableFuture<Void>> futures = IntStream.range(0, 100)
        .mapToObj(i -> CompletableFuture.runAsync(() ->
            uploadSinglePhoto(sessionId, "photo" + i + ".jpg", 2_000_000L)
        ))
        .toList();

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    Instant endTime = Instant.now();
    long durationSeconds = Duration.between(startTime, endTime).getSeconds();

    // Verify performance
    assertThat(durationSeconds).isLessThan(90); // <90 seconds

    double avgSpeedMbps = (200.0 / durationSeconds); // 200MB / duration
    assertThat(avgSpeedMbps).isGreaterThan(2.0); // >2MB/s

    log.info("Upload performance: {} photos in {}s, avg speed: {}MB/s",
        100, durationSeconds, String.format("%.2f", avgSpeedMbps));
}
```

### Test 5: WebSocket Progress Test

```java
@Test
void testWebSocketProgress() throws Exception {
    // Connect to WebSocket
    StompSession session = connectWebSocket(getTestJwt());

    String uploadSessionId = createUploadSession(10, 20_000_000L);

    // Subscribe to session topic
    BlockingQueue<UploadProgressMessage> messages = new LinkedBlockingQueue<>();

    session.subscribe("/topic/upload-sessions/" + uploadSessionId,
        new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return UploadProgressMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((UploadProgressMessage) payload);
            }
        });

    // Upload 1 photo
    var photoResponse = initiatePhotoUpload(uploadSessionId, "photo.jpg", 2_000_000L);
    uploadToS3(photoResponse.uploadUrl(), generateTestImage(2_000_000));
    completePhotoUpload(uploadSessionId, photoResponse.photoId());

    // Wait for WebSocket message
    UploadProgressMessage message = messages.poll(500, TimeUnit.MILLISECONDS);

    assertThat(message).isNotNull();
    assertThat(message.type()).isEqualTo("PHOTO_UPLOADED");
    assertThat(message.sessionId()).isEqualTo(uploadSessionId);
    assertThat(((PhotoUploadedMessage) message).uploadedCount()).isEqualTo(1);
}
```

### Test 6: Network Resilience Test

```java
@Test
@DirtiesContext
void testNetworkResilience() {
    // Use Toxiproxy to simulate network issues
    ToxiproxyContainer toxiproxy = new ToxiproxyContainer()
        .withNetwork(network);
    toxiproxy.start();

    ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(
        localstack, LocalStackContainer.Service.S3.getPort()
    );

    String sessionId = createUploadSession(10, 20_000_000L);

    // Upload 5 photos successfully
    for (int i = 0; i < 5; i++) {
        uploadSinglePhoto(sessionId, "photo" + i + ".jpg", 2_000_000L);
    }

    // Simulate network disconnect for 10 seconds
    proxy.setConnectionCut(true);
    Thread.sleep(10_000);
    proxy.setConnectionCut(false);

    // Upload remaining 5 photos (should succeed after reconnect)
    for (int i = 5; i < 10; i++) {
        uploadSinglePhoto(sessionId, "photo" + i + ".jpg", 2_000_000L);
    }

    // Verify all completed
    UploadSession session = sessionRepository.findById(new UploadSessionId(sessionId))
        .block();

    assertThat(session.getUploadedPhotos()).isEqualTo(10);
}
```

### Test 7: Retry Logic Test

```java
@Test
void testRetryLogic() {
    // Mock S3 to fail 2 times, then succeed
    AtomicInteger attemptCount = new AtomicInteger(0);

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenAnswer(invocation -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                throw SdkException.create("Network error", new IOException());
            }
            return PutObjectResponse.builder().build();
        });

    String sessionId = createUploadSession(1, 2_000_000L);

    // Upload should succeed after 2 retries
    uploadSinglePhoto(sessionId, "photo.jpg", 2_000_000L);

    // Verify retry count
    assertThat(attemptCount.get()).isEqualTo(3); // Initial + 2 retries
}
```

### Test 8: Resume Test

```java
@Test
void testMultipartResume() {
    String sessionId = createUploadSession(1, 10_000_000L);

    var photoResponse = initiatePhotoUpload(sessionId, "photo.jpg", 10_000_000L);

    // Upload first chunk
    byte[] chunk1 = generateTestImage(5_000_000);
    String etag1 = uploadToS3(photoResponse.partUrls().get(0), chunk1);

    // Simulate interruption (don't upload chunk 2)

    // Resume: Only upload chunk 2
    byte[] chunk2 = generateTestImage(5_000_000);
    String etag2 = uploadToS3(photoResponse.partUrls().get(1), chunk2);

    // Complete multipart
    completeMultipartUpload(sessionId, photoResponse.photoId(), photoResponse.uploadId(),
        List.of(
            new CompletedPart(1, etag1),
            new CompletedPart(2, etag2)
        ));

    // Verify completed
    Photo photo = photoRepository.findById(new PhotoId(photoResponse.photoId())).block();
    assertThat(photo.getUploadStatus()).isEqualTo(UploadStatus.COMPLETED);
}
```

### Chaos Testing

```java
@Test
void testChaosUpload() {
    // Random network interruptions during upload
    String sessionId = createUploadSession(50, 100_000_000L);

    Random random = new Random();
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < 50; i++) {
        int photoIndex = i;
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // Random delay
            sleep(random.nextInt(1000));

            // 20% chance of initial failure (will retry)
            if (random.nextInt(100) < 20) {
                throw new RuntimeException("Simulated failure");
            }

            uploadSinglePhoto(sessionId, "photo" + photoIndex + ".jpg", 2_000_000L);
        });
        futures.add(future);
    }

    // Wait for all uploads (with retries)
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // Verify all eventually completed
    UploadSession session = sessionRepository.findById(new UploadSessionId(sessionId))
        .block();

    assertThat(session.getUploadedPhotos() + session.getFailedPhotos()).isEqualTo(50);
}
```

### Load Test (50 Concurrent Users)

```java
@Test
@Tag("load-test")
void testLoadWith50ConcurrentUsers() {
    ExecutorService executor = Executors.newFixedThreadPool(50);

    List<Future<Boolean>> results = IntStream.range(0, 50)
        .mapToObj(userId -> executor.submit(() -> {
            // Each user uploads 10 photos
            String sessionId = createUploadSession(10, 20_000_000L);

            for (int i = 0; i < 10; i++) {
                uploadSinglePhoto(sessionId, "photo" + i + ".jpg", 2_000_000L);
            }

            return true;
        }))
        .toList();

    // Wait for all users
    results.forEach(future -> {
        try {
            assertThat(future.get()).isTrue();
        } catch (Exception e) {
            fail("Upload failed for user", e);
        }
    });

    executor.shutdown();
}
```

---

## Prerequisites
- Stories 2.1-2.13 completed - ALL MUST BE COMPLETE

---

## Testing Requirements

### Integration Tests
- [ ] Basic upload: 10 photos → S3 → Database
- [ ] Concurrent upload: 100 photos with 10 concurrent connections
- [ ] Multipart upload: 20MB file in 4 chunks
- [ ] Performance: 100 photos in <90 seconds
- [ ] WebSocket: Progress updates received within 500ms
- [ ] Network resilience: Pause on disconnect, resume on reconnect
- [ ] Retry logic: 3 retries with exponential backoff
- [ ] Resume: Multipart resume from last chunk

### Load Tests
- [ ] 50 concurrent users uploading
- [ ] 1000 photos total uploaded successfully
- [ ] No database deadlocks
- [ ] S3 rate limits not exceeded

### Chaos Tests
- [ ] Random network interruptions
- [ ] Random failures with retries
- [ ] All uploads eventually complete

---

## Definition of Done
- [ ] All acceptance criteria met
- [ ] All 8 core integration tests passing
- [ ] Testcontainers setup working (PostgreSQL, Redis, LocalStack)
- [ ] Performance benchmarks met (<90s for 100 photos)
- [ ] Load tests passing (50 concurrent users)
- [ ] Chaos tests passing (random failures handled)
- [ ] All tests run in CI/CD pipeline
- [ ] Test coverage report generated
- [ ] Code reviewed and approved

---

## Notes
- Testcontainers provides isolated test environment (PostgreSQL, Redis, S3)
- LocalStack simulates AWS S3 locally (no AWS costs)
- Toxiproxy simulates network issues (latency, disconnects)
- Performance benchmarks ensure system meets SLAs
- Load tests verify system scales to production load
- Chaos tests ensure resilience to random failures

---

**Status Log:**
- 2025-11-11: Story created (Draft)
