package com.smartitsm.sla.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartitsm.sla.dto.KnowledgeRecommendationDto;
import com.smartitsm.sla.dto.KnowledgeRecommendRequest;
import com.smartitsm.sla.entity.KnowledgeRecommendation;
import com.smartitsm.sla.repository.KnowledgeRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Knowledge article recommendation service.
 *
 * <p>Recommends articles based on ticket category + keyword overlap against
 * a static in-memory knowledge corpus (the canonical articles live in
 * knowledge-service; we mirror a representative subset for resilience and
 * to keep this service self-contained).
 *
 * <p>The relevance score (0..1) is computed as a weighted blend:
 * <pre>
 *   score = 0.5 * categoryMatch + 0.3 * keywordOverlap + 0.2 * recency
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRecommendationService {

    private final KnowledgeRecommendationRepository recommendationRepository;

    // In-memory KB corpus. The recommendation engine is deterministic
    // and does not require a network round-trip; production code wires this
    // up to the knowledge-service client.
    private static final List<KnowledgeArticle> KB_CORPUS = List.of(
            article(1L, "How to restart a Windows service",
                    "Step-by-step guide to safely restart a Windows service via services.msc or PowerShell.",
                    "INCIDENT", List.of("service", "restart", "windows", "crash")),
            article(2L, "Email delivery failure troubleshooting",
                    "Diagnose common SMTP / Exchange delivery failures including DNS, MX records, and SPF.",
                    "INCIDENT", List.of("email", "smtp", "exchange", "delivery", "dns")),
            article(3L, "VPN connectivity issues",
                    "Resolve VPN client connection failures including certificate, DNS, and routing problems.",
                    "INCIDENT", List.of("vpn", "network", "connectivity", "remote")),
            article(4L, "Password reset for end users",
                    "Self-service password reset instructions for employees.",
                    "SERVICE_REQUEST", List.of("password", "reset", "account", "login")),
            article(5L, "Printer setup and driver installation",
                    "Install network printers and update drivers on Windows and macOS.",
                    "SERVICE_REQUEST", List.of("printer", "driver", "install", "windows", "macos")),
            article(6L, "Database connection pool exhausted",
                    "How to detect, diagnose and remediate JDBC connection pool exhaustion.",
                    "INCIDENT", List.of("database", "jdbc", "connection", "pool", "performance")),
            article(7L, "Kubernetes pod restart loop",
                    "Diagnose CrashLoopBackOff and pod restart loops in Kubernetes.",
                    "INCIDENT", List.of("kubernetes", "pod", "crash", "restart", "k8s")),
            article(8L, "New laptop onboarding checklist",
                    "IT onboarding steps for provisioning a new laptop to a new hire.",
                    "SERVICE_REQUEST", List.of("laptop", "onboarding", "new hire", "provisioning")),
            article(9L, "SSL certificate renewal procedure",
                    "Steps to renew and deploy SSL certificates on internal services.",
                    "CHANGE_REQUEST", List.of("ssl", "certificate", "renewal", "tls", "security")),
            article(10L, "Memory leak analysis with heap dumps",
                    "Use jmap / jhat / Eclipse MAT to find memory leaks in Java services.",
                    "INCIDENT", List.of("java", "memory", "leak", "heap", "performance")),
            article(11L, "Active Directory account lockout",
                    "Resolve frequent AD account lockouts by finding the source client.",
                    "INCIDENT", List.of("active directory", "ad", "lockout", "account", "login")),
            article(12L, "Office 365 license assignment",
                    "Assign and revoke Microsoft 365 licenses for users.",
                    "SERVICE_REQUEST", List.of("office", "365", "license", "m365", "microsoft"))
    );

    /**
     * Recommend up to {@code topK} articles for the given ticket.
     * Persists a {@link KnowledgeRecommendation} for every article returned.
     */
    @Transactional
    public List<KnowledgeRecommendationDto> recommend(KnowledgeRecommendRequest request) {
        log.info("Recommending knowledge for ticket {}", request.getTicketId());
        int topK = (request.getTopK() == null || request.getTopK() <= 0) ? 5 : request.getTopK();

        List<ScoredArticle> scored = scoreCorpus(request);
        List<ScoredArticle> top = scored.stream()
                .sorted(Comparator.comparingDouble(ScoredArticle::score).reversed())
                .limit(topK)
                .toList();

        List<KnowledgeRecommendationDto> result = new ArrayList<>(top.size());
        for (ScoredArticle sa : top) {
            KnowledgeRecommendation rec = KnowledgeRecommendation.builder()
                    .ticketId(request.getTicketId())
                    .knowledgeId(sa.article().id())
                    .relevanceScore(sa.score())
                    .matchReason(sa.reason())
                    .recommendedAt(LocalDateTime.now())
                    .clicked(false)
                    .helpful(false)
                    .build();
            recommendationRepository.save(rec);
            result.add(toDto(rec, sa.article()));
        }
        return result;
    }

    /**
     * Mark a recommendation as clicked by the user.
     */
    @Transactional
    public KnowledgeRecommendationDto markClicked(Long id) {
        KnowledgeRecommendation rec = recommendationRepository.getById(id);
        if (rec == null) {
            return null;
        }
        rec.setClicked(true);
        rec.setClickedAt(LocalDateTime.now());
        recommendationRepository.updateById(rec);
        return toDto(rec, lookupArticle(rec.getKnowledgeId()));
    }

    /**
     * Mark a recommendation as helpful (positive feedback).
     */
    @Transactional
    public KnowledgeRecommendationDto markHelpful(Long id) {
        KnowledgeRecommendation rec = recommendationRepository.getById(id);
        if (rec == null) {
            return null;
        }
        rec.setHelpful(true);
        rec.setFeedbackAt(LocalDateTime.now());
        recommendationRepository.updateById(rec);
        return toDto(rec, lookupArticle(rec.getKnowledgeId()));
    }

    /**
     * List recommendations already produced for a ticket.
     */
    public List<KnowledgeRecommendationDto> getByTicket(Long ticketId) {
        return recommendationRepository.list(
                new QueryWrapper<KnowledgeRecommendation>()
                        .eq("ticket_id", ticketId)
                        .orderByDesc("relevance_score")
        ).stream()
                .map(rec -> toDto(rec, lookupArticle(rec.getKnowledgeId())))
                .collect(Collectors.toList());
    }

    // ========== SCORING ==========

    List<ScoredArticle> scoreCorpus(KnowledgeRecommendRequest request) {
        String category = request.getTicketCategory();
        Set<String> keywords = normalizeKeywords(request.getKeywords(), request.getTicketTitle(), request.getTicketDescription());

        List<ScoredArticle> scored = new ArrayList<>(KB_CORPUS.size());
        for (KnowledgeArticle a : KB_CORPUS) {
            double categoryScore = category != null && category.equalsIgnoreCase(a.category()) ? 1.0 : 0.0;
            double keywordScore = keywordOverlap(keywords, a.tags());
            double recencyScore = 0.8; // corpus is small; treat all as equally fresh

            double relevance = 0.5 * categoryScore + 0.3 * keywordScore + 0.2 * recencyScore;
            String reason = buildReason(categoryScore, keywordScore, category, keywords);
            scored.add(new ScoredArticle(a, clamp01(relevance), reason));
        }
        return scored;
    }

    double keywordOverlap(Set<String> query, List<String> articleTags) {
        if (query == null || query.isEmpty() || articleTags == null || articleTags.isEmpty()) {
            return 0.0;
        }
        long matches = articleTags.stream()
                .filter(t -> query.contains(t.toLowerCase()))
                .count();
        return clamp01((double) matches / articleTags.size());
    }

    String buildReason(double categoryScore, double keywordScore, String category, Set<String> keywords) {
        StringBuilder sb = new StringBuilder();
        if (categoryScore > 0 && category != null) {
            sb.append("category=").append(category).append(';');
        }
        if (keywordScore > 0 && keywords != null && !keywords.isEmpty()) {
            sb.append("matched ").append((int) Math.round(keywordScore * 100)).append("% keywords");
        }
        if (sb.length() == 0) {
            sb.append("default ranking");
        }
        return sb.toString();
    }

    Set<String> normalizeKeywords(List<String> explicit, String title, String description) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (explicit != null) {
            explicit.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.toLowerCase().trim())
                    .forEach(set::add);
        }
        addTokens(set, title);
        addTokens(set, description);
        return set;
    }

    void addTokens(java.util.Set<String> set, String text) {
        if (text == null || text.isBlank()) return;
        for (String token : text.toLowerCase().split("[^a-z0-9]+")) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                set.add(token);
            }
        }
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "have", "has",
            "are", "was", "were", "but", "not", "you", "your", "can", "cannot",
            "into", "all", "any", "our", "out", "his", "her", "they", "their"
    );

    KnowledgeArticle lookupArticle(Long id) {
        return KB_CORPUS.stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElse(new KnowledgeArticle(id, "Unknown", "", "", Collections.emptyList()));
    }

    KnowledgeRecommendationDto toDto(KnowledgeRecommendation rec, KnowledgeArticle article) {
        if (rec == null) return null;
        return KnowledgeRecommendationDto.builder()
                .id(rec.getId())
                .ticketId(rec.getTicketId())
                .knowledgeId(rec.getKnowledgeId())
                .knowledgeTitle(article.title())
                .knowledgeSummary(article.summary())
                .relevanceScore(rec.getRelevanceScore())
                .matchReason(rec.getMatchReason())
                .clicked(rec.getClicked())
                .clickedAt(rec.getClickedAt())
                .helpful(rec.getHelpful())
                .feedbackAt(rec.getFeedbackAt())
                .recommendedAt(rec.getRecommendedAt())
                .createdAt(rec.getCreatedAt())
                .build();
    }

    static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static KnowledgeArticle article(long id, String title, String summary, String category, List<String> tags) {
        return new KnowledgeArticle(id, title, summary, category, tags);
    }

    // ---- Records / helpers ----
    public record KnowledgeArticle(Long id, String title, String summary, String category, List<String> tags) {}
    public record ScoredArticle(KnowledgeArticle article, double score, String reason) {}
}