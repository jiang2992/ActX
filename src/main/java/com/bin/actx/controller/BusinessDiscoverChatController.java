package com.bin.actx.controller;

import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.message.SessionInfo;
import com.bin.actx.message.SessionMessageCollection;
import com.bin.actx.model.RequestResult;
import com.bin.actx.service.ChatService;
import com.bin.actx.service.request.UserChatRequest;
import com.bin.actx.service.result.StreamChatStopResult;
import com.bin.actx.storage.local.repository.SessionMessageRepository;
import com.bin.actx.storage.local.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.text.StringUtils;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

/**
 * 业务发现聊天控制器
 *
 * @author Bin
 * @since 2026/5/12 11:31
 */
@Slf4j
@Controller
@RequestMapping("/business_discover")
public class BusinessDiscoverChatController extends BaseChatController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionMessageRepository sessionMessageRepository;

    @GetMapping("/sessions")
    @ResponseBody
    public RequestResult<List<SessionInfo>> getSessionList(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("User-ID");
        return RequestResult.success(sessionRepository.getUserSessions(userId, "business_discover").getSessions());
    }

    @GetMapping("/sessions/latest")
    @ResponseBody
    public RequestResult<SessionInfo> getLatestSession(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("User-ID");
        return RequestResult.success(sessionRepository.getLatestSession(userId, "business_discover"));
    }

    @GetMapping("/messages")
    @ResponseBody
    public RequestResult<SessionMessageCollection> getMessageList(String sessionId) {
        return RequestResult.success(sessionMessageRepository.getSessionMessages(sessionId));
    }

    /**
     * 流式聊天接口，以SSE形式逐事件推送
     *
     * @param userChatRequest 用户请求
     * @return flux
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> streamChat(HttpServletRequest httpRequest,
            @RequestBody UserChatRequest userChatRequest) {
        String userId = httpRequest.getHeader("User-ID");
        userChatRequest.setUserId(userId);
        if (StringUtils.isEmpty(userChatRequest.getSessionId())) {
            userChatRequest.setSessionId(IdUtils.uuid());
        }
        SessionInfo sessionInfo = new SessionInfo(userChatRequest.getUserId(), userChatRequest.getSessionId(),
                userChatRequest.getMessage());
        boolean isNewSession = sessionRepository.addSession("business_discover", sessionInfo);
        return toSseEvents(chatService.streamChat(userChatRequest, SceneConstant.BUSINESS_DISCOVER),
                isNewSession ? sessionInfo : null);
    }

    /**
     * 停止流式聊天（业务发现）
     *
     * @param httpRequest     httpRequest
     * @param userChatRequest 用户请求
     * @return 停止结果
     */
    @DeleteMapping(value = "/chat/stream")
    @ResponseBody
    public RequestResult<StreamChatStopResult> stopStreamChat(HttpServletRequest httpRequest,
            @RequestBody UserChatRequest userChatRequest) {
        return RequestResult.success(chatService.stopStreamChat(userChatRequest.getSessionId()));
    }

}
