package com.bdreview.platform.messaging;

import com.bdreview.platform.business.Business;
import com.bdreview.platform.business.BusinessRepository;
import com.bdreview.platform.common.ForbiddenException;
import com.bdreview.platform.common.PageRequestDefaults;
import com.bdreview.platform.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spec §16: a consumer's direct inquiry to a business owner reuses the same
 * thread/infra as owner-reply-to-review (§7) — this service is that shared
 * channel. Direction is inferred from whether the sender is the business's
 * owner or not; a thread is always scoped to exactly one consumer + one business.
 */
@Service
public class MessageService {

    private final MessageThreadRepository threadRepository;
    private final MessageRepository messageRepository;
    private final BusinessRepository businessRepository;

    public MessageService(MessageThreadRepository threadRepository,
                           MessageRepository messageRepository,
                           BusinessRepository businessRepository) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public Message send(UUID senderUserId, SendMessageRequest request) {
        Business business = businessRepository.findById(request.businessId())
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        boolean senderIsOwner = business.getOwnerUserId().equals(senderUserId);
        UUID consumerUserId = senderIsOwner ? resolveOtherPartyOrThrow(business.getId(), senderUserId) : senderUserId;

        MessageThread thread = threadRepository.findByConsumerUserIdAndBusinessId(consumerUserId, business.getId())
                .orElseGet(() -> threadRepository.save(MessageThread.builder()
                        .consumerUserId(consumerUserId).businessId(business.getId()).build()));

        return messageRepository.save(Message.builder()
                .threadId(thread.getId()).senderUserId(senderUserId).content(request.content()).build());
    }

    private UUID resolveOtherPartyOrThrow(UUID businessId, UUID ownerUserId) {
        // Owner replying: they must specify which consumer thread via a separate endpoint (send-in-thread);
        // sending "cold" as an owner with no existing thread isn't a valid direction for this MVP channel.
        throw new ForbiddenException(
                "Owners can only reply within an existing thread — use POST /messages/threads/{threadId}/reply");
    }

    @Transactional
    public Message reply(UUID senderUserId, UUID threadId, String content) {
        MessageThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Thread not found"));
        Business business = businessRepository.findById(thread.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        boolean isParticipant = thread.getConsumerUserId().equals(senderUserId)
                || business.getOwnerUserId().equals(senderUserId);
        if (!isParticipant) {
            throw new ForbiddenException("You are not part of this conversation");
        }

        return messageRepository.save(Message.builder()
                .threadId(threadId).senderUserId(senderUserId).content(content).build());
    }

    @Transactional
    public void markRead(UUID readerUserId, UUID threadId) {
        messageRepository.markThreadReadBy(threadId, readerUserId, Instant.now());
    }

    public Page<Message> history(UUID threadId, int page, int size) {
        return messageRepository.findByThreadIdOrderByCreatedAtAsc(
                threadId, PageRequest.of(page, PageRequestDefaults.clamp(size)));
    }

    public long unreadCountFor(UUID threadId, UUID readerUserId) {
        return messageRepository.countByThreadIdAndReadAtIsNullAndSenderUserIdNot(threadId, readerUserId);
    }

    public List<MessageThread> myThreadsAsConsumer(UUID consumerUserId) {
        return threadRepository.findByConsumerUserId(consumerUserId);
    }

    public List<MessageThread> threadsForMyBusinesses(UUID ownerUserId) {
        List<UUID> businessIds = businessRepository.findByOwnerUserIdAndDeletedAtIsNull(ownerUserId)
                .stream().map(Business::getId).toList();
        return threadRepository.findByBusinessIdIn(businessIds);
    }
}
