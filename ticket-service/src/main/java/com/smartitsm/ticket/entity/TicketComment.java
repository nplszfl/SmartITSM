package com.smartitsm.ticket.entity;

import com.smartitsm.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * TicketComment entity - represents comments/notes on tickets.
 */
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_comment")
public class TicketComment extends BaseEntity {

    private Long ticketId;              // Reference to the ticket
    private String content;             // Comment content
    private String authorId;            // User who wrote the comment
    private String authorName;          // Display name of the author
    private String authorType;          // REQUESTER, AGENT, SYSTEM
    private String visibility;          // INTERNAL, EXTERNAL (visible to requester)
    private String parentCommentId;     // For threaded replies
    private String attachmentUrls;      // JSON array of attachment URLs
}