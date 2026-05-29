package com.smartitsm.classification.service;

import com.smartitsm.classification.dto.ClassificationRequest;
import com.smartitsm.classification.dto.ClassificationResponse;
import com.smartitsm.classification.entity.TicketClassification;
import com.smartitsm.classification.repository.TicketClassificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TicketClassificationService.
 */
@ExtendWith(MockitoExtension.class)
class TicketClassificationServiceTest {

    @Mock
    private TicketClassificationRepository classificationRepository;

    @Mock
    private com.smartitsm.common.ai.AIClient aiClient;

    @InjectMocks
    private TicketClassificationService classificationService;

    private ClassificationRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = ClassificationRequest.builder()
            .ticketId(1L)
            .ticketNumber("TKT-20240529-0001")
            .title("Server is down")
            .description("Production server is not responding, all users affected")
            .requesterId("user001")
            .requesterDepartment("IT")
            .requesterPriority("NORMAL")
            .affectedUsers(50)
            .source("PORTAL")
            .build();
    }

    @Test
    void testClassifyTicket_WithIncidentKeywords() {
        // Arrange
        when(classificationRepository.save(any(TicketClassification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassificationResponse response = classificationService.classifyTicket(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("INCIDENT", response.getCategory());
        assertNotNull(response.getPriority());
        assertNotNull(response.getAssignedGroup());
        assertTrue(response.getProcessingTimeMs() >= 0);
        verify(classificationRepository, times(1)).save(any(TicketClassification.class));
    }

    @Test
    void testClassifyTicket_WithServiceRequestKeywords() {
        // Arrange
        ClassificationRequest request = ClassificationRequest.builder()
            .ticketId(2L)
            .ticketNumber("TKT-20240529-0002")
            .title("Please create new email account")
            .description("Need a new email account for new employee")
            .requesterId("user002")
            .requesterDepartment("HR")
            .build();

        when(classificationRepository.save(any(TicketClassification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassificationResponse response = classificationService.classifyTicket(request);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("SERVICE_REQUEST", response.getCategory());
    }

    @Test
    void testClassifyTicket_WithChangeRequestKeywords() {
        // Arrange
        ClassificationRequest request = ClassificationRequest.builder()
            .ticketId(3L)
            .ticketNumber("TKT-20240529-0003")
            .title("Need to upgrade database server")
            .description("Please upgrade the database to latest version")
            .requesterId("user003")
            .build();

        when(classificationRepository.save(any(TicketClassification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassificationResponse response = classificationService.classifyTicket(request);

        // Assert
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("CHANGE_REQUEST", response.getCategory());
    }

    @Test
    void testClassifyTicket_VIPRequesterGetsHigherPriority() {
        // Arrange
        ClassificationRequest request = ClassificationRequest.builder()
            .ticketId(4L)
            .ticketNumber("TKT-20240529-0004")
            .title("Minor issue with printer")
            .description("One printer in office is having paper jams")
            .requesterId("vip001")
            .requesterPriority("VIP")
            .affectedUsers(1)
            .build();

        when(classificationRepository.save(any(TicketClassification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassificationResponse response = classificationService.classifyTicket(request);

        // Assert
        assertNotNull(response);
        // VIP requester should get bumped to at least MEDIUM priority
        assertNotEquals("LOW", response.getPriority());
    }

    @Test
    void testClassifyTicket_HighAffectedUsersGetsHigherPriority() {
        // Arrange
        ClassificationRequest request = ClassificationRequest.builder()
            .ticketId(5L)
            .ticketNumber("TKT-20240529-0005")
            .title("Printer not working")
            .description("Need printer access")
            .affectedUsers(100)
            .build();

        when(classificationRepository.save(any(TicketClassification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassificationResponse response = classificationService.classifyTicket(request);

        // Assert
        assertNotNull(response);
        // High affected users should result in higher priority
        assertTrue(
            "HIGH".equals(response.getPriority()) || "CRITICAL".equals(response.getPriority()),
            "Expected HIGH or CRITICAL priority for 100 affected users"
        );
    }

    @Test
    void testClassifyTicket_AssignsCorrectGroup() {
        // Arrange
        when(classificationRepository.save(any(TicketClassification.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ClassificationResponse response = classificationService.classifyTicket(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals("L2_Support", response.getAssignedGroup());
    }

    @Test
    void testGetClassificationByTicketId() {
        // Arrange
        TicketClassification classification = TicketClassification.builder()
            .ticketId(1L)
            .ticketNumber("TKT-20240529-0001")
            .category("INCIDENT")
            .priority("HIGH")
            .assignedGroup("L2_Support")
            .build();

        when(classificationRepository.getOne(any())).thenReturn(classification);

        // Act
        TicketClassification result = classificationService.getClassificationByTicketId(1L);

        // Assert
        assertNotNull(result);
        assertEquals("INCIDENT", result.getCategory());
        assertEquals("HIGH", result.getPriority());
    }

    @Test
    void testGetClassificationByTicketId_NotFound() {
        // Arrange
        when(classificationRepository.getOne(any())).thenReturn(null);

        // Act
        TicketClassification result = classificationService.getClassificationByTicketId(999L);

        // Assert
        assertNull(result);
    }
}