package com.bin.actx.controller;

import com.bin.actx.config.constant.SceneConstant;
import com.bin.actx.message.SessionInfo;
import com.bin.actx.message.SessionMessageCollection;
import com.bin.actx.model.RequestResult;
import com.bin.actx.service.ChatService;
import com.bin.actx.service.request.UserChatRequest;
import com.bin.actx.service.result.ChatDirectResult;
import com.bin.actx.service.result.StreamChatStopResult;
import com.bin.actx.storage.local.repository.SessionMessageRepository;
import com.bin.actx.storage.local.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.jiang.tools.text.StringUtils;
import org.jiang.tools.text.id.IdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

/**
 * 聊天控制器
 *
 * @author Bin
 * @since 2026/5/12 11:31
 */
@Controller
public class ChatController extends BaseChatController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionMessageRepository sessionMessageRepository;

    @GetMapping("/session/list")
    @ResponseBody
    public RequestResult<List<SessionInfo>> getSessionList(HttpServletRequest httpRequest) {
        String userId = httpRequest.getHeader("User-ID");
        return RequestResult.success(sessionRepository.getUserSessions(userId, "chat").getSessions());
    }

    @GetMapping("/message/list")
    @ResponseBody
    public RequestResult<SessionMessageCollection> getMessageList(String sessionId) {
        return RequestResult.success(sessionMessageRepository.getSessionMessages(sessionId));
    }

    /**
     * 同步聊天接口，返回完整的对话结果
     *
     * @param userChatRequest 用户请求
     * @return 聊天回应
     */
    @PostMapping("/chat")
    @ResponseBody
    public RequestResult<ChatDirectResult> chat(HttpServletRequest httpRequest, @RequestBody UserChatRequest userChatRequest) {
        String userId = httpRequest.getHeader("User-ID");
        userChatRequest.setUserId(userId);

        // 会话处理
        boolean isNewSession;
        if (StringUtils.isEmpty(userChatRequest.getSessionId())) {
            userChatRequest.setSessionId(IdUtils.uuid());
            isNewSession = true;
        } else {
            isNewSession = !sessionRepository.containsSession(userChatRequest.getSessionId());
        }
        if (isNewSession) {
            String sessionTitle = chatService.extractTitle(userChatRequest);
            SessionInfo sessionInfo = new SessionInfo(userId, userChatRequest.getSessionId(), sessionTitle);
            sessionRepository.addSession("chat", sessionInfo);
        }

        return RequestResult.success(chatService.chat(userChatRequest, SceneConstant.CHAT));
    }

    /**
     * 流式聊天接口，以SSE形式逐事件推送
     *
     * @param userChatRequest 用户请求
     * @return flux
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ServerSentEvent<String>> streamChat(HttpServletRequest httpRequest, @RequestBody UserChatRequest userChatRequest) {
        String userId = httpRequest.getHeader("User-ID");
        userChatRequest.setUserId(userId);

        // 会话处理
        boolean isNewSession;
        if (StringUtils.isEmpty(userChatRequest.getSessionId())) {
            userChatRequest.setSessionId(IdUtils.uuid());
            isNewSession = true;
        } else {
            isNewSession = !sessionRepository.containsSession(userChatRequest.getSessionId());
        }
        SessionInfo sessionInfo = null;
        if (isNewSession) {
            String sessionTitle = chatService.extractTitle(userChatRequest);
            sessionInfo = new SessionInfo(userId, userChatRequest.getSessionId(), sessionTitle);
            sessionRepository.addSession("chat", sessionInfo);
        }

        return toSseEvents(chatService.streamChat(userChatRequest, SceneConstant.CHAT), sessionInfo);
    }

    /**
     * 停止流式聊天
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

    @DeleteMapping("/session/{id}")
    @ResponseBody
    public RequestResult<Void> deleteSession(@PathVariable("id") String id) {
        sessionRepository.deleteSession(id);
        return RequestResult.success();
    }

}
