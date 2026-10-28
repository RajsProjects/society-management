package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.communication.service.SocietyGroupService;
import com.Application.SocietyManagement.core.service.S3Service;
import com.Application.SocietyManagement.core.util.JoinCodeGenerator;
import com.Application.SocietyManagement.society.dto.RegisterSocietyRequest;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.society.service.SocietyService;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security: File Upload, MIME Filtering & S3 Storage Security Test Suite")
class FileUploadSecurityTest {

        @Mock
        private SocietyRepository societyRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private S3Service s3Service;

        @Mock
        private JoinCodeGenerator joinCodeGenerator;

        @Mock
        private SocietyGroupService societyGroupService;

        @Mock
        private S3Client s3Client;

        @Mock
        private S3Presigner s3Presigner;

        private SocietyService societyService;
        private RegisterSocietyRequest validRequest;

        @BeforeEach
        void setUp() {
                societyService = new SocietyService(
                                societyRepository,
                                userRepository,
                                passwordEncoder,
                                s3Service,
                                joinCodeGenerator,
                                societyGroupService);

                validRequest = new RegisterSocietyRequest();
                validRequest.setName("Green Heights");
                validRequest.setRegistrationNumber("REG-SEC-1234");
                validRequest.setEmail("admin@greenheights.com");
                validRequest.setPhone("9876543210");
                validRequest.setPassword("Password@123");
                validRequest.setFirstName("Admin");
                validRequest.setLastName("User");
                validRequest.setAddress("123 Main St");
                validRequest.setCity("Bangalore");
                validRequest.setState("Karnataka");
                validRequest.setPincode("560001");
                validRequest.setTotalFlats(50);
        }

        @Test
        @DisplayName("Upload Security: File exceeding 5MB limit is strictly rejected with 400 Bad Request")
        void uploadExceeding5MB_isRejected() {
                byte[] oversizedContent = new byte[6 * 1024 * 1024]; // 6MB
                MockMultipartFile file = new MockMultipartFile(
                                "document",
                                "large-certificate.pdf",
                                "application/pdf",
                                oversizedContent);

                when(societyRepository.existsByRegistrationNumber("REG-SEC-1234")).thenReturn(false);
                when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(java.util.Optional.empty());

                assertThatThrownBy(() -> societyService.register(validRequest, file))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                                        assertThat(rse.getReason()).contains("Document size exceeds 5MB limit");
                                });

                verify(s3Service, never()).uploadFile(any(), any(), any());
        }

        @Test
        @DisplayName("Upload Security: Executable or non-allowlisted MIME types are rejected with 400 Bad Request")
        void uploadDisallowedMimeType_isRejected() {
                MockMultipartFile exeFile = new MockMultipartFile(
                                "document",
                                "certificate.exe",
                                "application/x-msdownload",
                                "MZ...fake-binary".getBytes());

                when(societyRepository.existsByRegistrationNumber("REG-SEC-1234")).thenReturn(false);
                when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(java.util.Optional.empty());

                assertThatThrownBy(() -> societyService.register(validRequest, exeFile))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                                        assertThat(rse.getReason())
                                                        .contains("Only PDF, JPEG, and PNG files are allowed");
                                });

                verify(s3Service, never()).uploadFile(any(), any(), any());
        }

        @Test
        @DisplayName("Upload Security: Archive files (ZIP/TAR) are rejected with 400 Bad Request")
        void uploadZipArchive_isRejected() {
                MockMultipartFile zipFile = new MockMultipartFile(
                                "document",
                                "certificate.zip",
                                "application/zip",
                                "PK...dummy-zip".getBytes());

                when(societyRepository.existsByRegistrationNumber("REG-SEC-1234")).thenReturn(false);
                when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(java.util.Optional.empty());

                assertThatThrownBy(() -> societyService.register(validRequest, zipFile))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                                                .isEqualTo(HttpStatus.BAD_REQUEST));

                verify(s3Service, never()).uploadFile(any(), any(), any());
        }

        @Test
        @DisplayName("Upload Security: Valid PDF file within size limit is accepted and stored via S3Service")
        void uploadValidPdf_isAccepted() throws IOException {
                MockMultipartFile validPdf = new MockMultipartFile(
                                "document",
                                "certificate.pdf",
                                "application/pdf",
                                "%PDF-1.4 valid dummy pdf content".getBytes());

                when(societyRepository.existsByRegistrationNumber("REG-SEC-1234")).thenReturn(false);
                when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(java.util.Optional.empty());
                when(joinCodeGenerator.generate()).thenReturn("SOC-TEST123");
                when(societyRepository.findByJoinCode("SOC-TEST123")).thenReturn(java.util.Optional.empty());
                when(societyRepository.save(any())).thenAnswer(inv -> {
                        com.Application.SocietyManagement.society.entity.Society s = inv.getArgument(0);
                        s.setId("soc-test-id");
                        return s;
                });
                when(userRepository.save(any())).thenAnswer(inv -> {
                        com.Application.SocietyManagement.users.entity.User u = inv.getArgument(0);
                        u.setId("usr-test-id");
                        return u;
                });
                when(s3Service.uploadFile(any(), eq("application/pdf"), eq("society-documents")))
                                .thenReturn("society-documents/uuid-random-key");

                var response = societyService.register(validRequest, validPdf);

                assertThat(response).isNotNull();
                assertThat(response.get("status")).isEqualTo("PENDING_VERIFICATION");
                verify(s3Service).uploadFile(any(), eq("application/pdf"), eq("society-documents"));
        }

        @Test
        @DisplayName("Path Traversal Defense: S3Service ignores user filename and isolates objects using UUID keys")
        void s3Service_pathTraversalInFilename_neutralizedByUuid() {
                S3Service actualS3Service = new S3Service(s3Client, s3Presigner);
                org.springframework.test.util.ReflectionTestUtils.setField(actualS3Service, "bucket",
                                "civiclink-bucket");

                when(s3Client.putObject(any(PutObjectRequest.class),
                                any(software.amazon.awssdk.core.sync.RequestBody.class)))
                                .thenReturn(null);

                byte[] fileBytes = "test content".getBytes();
                String storedKey = actualS3Service.uploadFile(fileBytes, "application/pdf", "society-documents");

                // Asserts path traversal characters never appear in the S3 key
                assertThat(storedKey).startsWith("society-documents/");
                assertThat(storedKey).doesNotContain("..");
                assertThat(storedKey).doesNotContain("\\");
                assertThat(storedKey.substring("society-documents/".length())).doesNotContain("/");
                assertThat(storedKey.chars().filter(ch -> ch == '/').count()).isEqualTo(1);
        }
}