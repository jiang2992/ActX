package com.bin.actx.controller;

import com.bin.actx.channel.event.StreamEvent;
import com.bin.actx.channel.event.StreamEventType;
import com.bin.actx.message.SessionBriefInfo;
import com.bin.actx.message.SessionInfo;
import lombok.extern.slf4j.Slf4j;
import org.jiang.tools.json.JsonUtils;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

@Slf4j
public abstract class BaseChatController {

    protected Flux<ServerSentEvent<String>> toSseEvents(Flux<StreamEvent> events, SessionInfo sessionInfo) {
        if (sessionInfo != null) {
            SessionBriefInfo sessionBriefInfo = new SessionBriefInfo(sessionInfo.getUserId(), sessionInfo.getSessionId(),
                    sessionInfo.getName());
            events = events.startWith(new StreamEvent(StreamEventType.SESSION, JsonUtils.toJson(sessionBriefInfo)));
        }
        return events.map(event -> ServerSentEvent.<String>builder()
                .event(event.getType().name().toLowerCase())
                .data(event.getData())
                .build());
    }

}
