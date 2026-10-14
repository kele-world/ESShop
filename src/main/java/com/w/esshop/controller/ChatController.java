package com.w.esshop.controller;

import com.w.esshop.common.Result;
import com.w.esshop.dto.ChatReplyVO;
import com.w.esshop.dto.ChatRequestDTO;
import com.w.esshop.entity.ChatKnowledge;
import com.w.esshop.entity.ChatMessage;
import com.w.esshop.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能客服控制器
 */
@Tag(name = "智能客服", description = "在线客服问答、常见问题")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<ChatReplyVO> send(@Valid @RequestBody ChatRequestDTO dto, HttpServletRequest request) {
        Long userId = getUserIdOrNull(request);
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "guest";
        }
        return Result.success(chatService.chat(userId, sessionId, dto.getContent()));
    }

    @Operation(summary = "会话历史")
    @GetMapping("/history")
    public Result<List<ChatMessage>> history(@RequestParam String sessionId) {
        return Result.success(chatService.history(sessionId));
    }

    @Operation(summary = "清空会话")
    @DeleteMapping("/history")
    public Result<Void> clear(@RequestParam String sessionId) {
        chatService.clear(sessionId);
        return Result.success("已清空");
    }

    @Operation(summary = "常见问题")
    @GetMapping("/hot-questions")
    public Result<List<ChatKnowledge>> hotQuestions() {
        return Result.success(chatService.hotQuestions());
    }

    private Long getUserIdOrNull(HttpServletRequest request) {
        try { return (Long) request.getAttribute("userId"); } catch (Exception e) { return null; }
    }
}