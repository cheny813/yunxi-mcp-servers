package io.yunxi.mcp.formfill.controller;

import io.yunxi.mcp.formfill.model.FormFillMessage;
import io.yunxi.mcp.formfill.service.FormFillerService;
import io.yunxi.mcp.formfill.service.PageStructureRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 消息控制器。
 *
 * <p>前端通过 {@code /app/formfill} 回写的消息分两类，后端区别处理：
 * <ul>
 *   <li><b>get_structure</b>：真实页面 DOM 字段清单。这是真实页面结构的消费入口——
 *       后端将其注册进 {@link PageStructureRegistry}，成为后续填表下发的结构依据，
 *       不再广播回 {@code /topic/formfill}（避免把结构噪声推给所有订阅者）。</li>
 *   <li><b>fill / result</b>：填表执行回执。仅记录日志（可观测性），
 *       不向业务 topic 广播，避免污染正式指令流。</li>
 * </ul>
 */
@Slf4j
@Controller
public class FormFillWebSocketController {

    /** 表单填写指令下发主题，FormFillerService 通过该常量向 WebSocket 推送指令 */
    public static final String TOPIC = "/topic/formfill";

    private final PageStructureRegistry structureRegistry;
    private final FormFillerService formFillerService;

    public FormFillWebSocketController(PageStructureRegistry structureRegistry,
                                       FormFillerService formFillerService) {
        this.structureRegistry = structureRegistry;
        this.formFillerService = formFillerService;
    }

    /**
     * 接收前端回写消息（结构上报 / 执行回执）。
     *
     * <p>本方法作为 WebSocket 入口保留 @SendTo 透传约定，但按 type 短路：
     * get_structure 已被注册表消费，不广播；fill/result 仅日志。
     * 返回 null 表示不向 topic 推送任何内容（@SendTo 对 null 不做广播）。
     */
    @MessageMapping("/app/formfill")
    @SendTo("/topic/formfill")
    public FormFillMessage handleFormFillFeedback(FormFillMessage message) {
        if (message == null) {
            log.warn("收到空的前端反馈消息");
            return null;
        }

        String type = message.getType();
        if ("get_structure".equals(type)) {
            // 消费前端真实页面结构，注册进后端，不再广播
            structureRegistry.registerFromMessage(message);
            log.info("已消费前端页面结构回写: scene={}", message.getScene());
            return null;
        }

        // fill / result / 其他：仅记录执行回执，供可观测性，不广播
        log.info("收到前端回执: type={}, success={}, message={}",
                type,
                message.getResult() != null ? message.getResult().isSuccess() : null,
                message.getResult() != null ? message.getResult().getMessage() : null);
        return null;
    }
}
