package io.yunxi.mcp.formfill.controller;

import io.yunxi.mcp.formfill.model.FormFillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息控制器
 * 
 * 处理前端发来的表单填写反馈
 */
@Slf4j
@Controller
public class FormFillWebSocketController {

    /**
     * 接收前端的填写结果反馈
     */
    @MessageMapping("/app/formfill")
    @SendTo("/topic/formfill")
    public FormFillMessage handleFormFillFeedback(FormFillMessage message) {
        log.info("收到前端反馈: type={}, success={}, message={}",
                message.getType(),
                message.getResult() != null ? message.getResult().isSuccess() : null,
                message.getResult() != null ? message.getResult().getMessage() : null);

        return message;
    }
}
