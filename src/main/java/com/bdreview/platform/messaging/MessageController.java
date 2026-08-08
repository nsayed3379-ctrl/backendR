package com.bdreview.platform.messaging;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** Consumer starts (or continues) a thread with a business — see spec §16. */
    @PostMapping
    public ResponseEntity<Message> send(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messageService.send(CurrentUser.id(), request));
    }

    /** Either party replies inside an existing thread — this is also how owners reply to reviews' DM channel. */
    @PostMapping("/threads/{threadId}/reply")
    public ResponseEntity<Message> reply(@PathVariable UUID threadId, @Valid @RequestBody ReplyRequest request) {
        return ResponseEntity.ok(messageService.reply(CurrentUser.id(), threadId, request.content()));
    }

    @PostMapping("/threads/{threadId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID threadId) {
        messageService.markRead(CurrentUser.id(), threadId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<PageResponse<Message>> history(@PathVariable UUID threadId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponse.of(messageService.history(threadId, page, size)));
    }

    @GetMapping("/threads/mine")
    public ResponseEntity<List<MessageThread>> myThreads() {
        return ResponseEntity.ok(messageService.myThreadsAsConsumer(CurrentUser.id()));
    }

    @GetMapping("/threads/business-inbox")
    public ResponseEntity<List<MessageThread>> businessInbox() {
        CurrentUser.requireRole("BUSINESS_OWNER");
        return ResponseEntity.ok(messageService.threadsForMyBusinesses(CurrentUser.id()));
    }
}
