package com.bin.actx.plugin.core.tool;

import com.bin.actx.context.RequestContext;
import com.bin.actx.plugin.hook.RequestHook;
import com.bin.actx.service.request.RequestMessagePayload;
import java.util.Set;

/**
 * 为请求消息装配工具
 *
 * @author Bin
 * @since 2026/8/19 15:39
 */
public class StaticToolPlugin extends ToolBasePlugin implements RequestHook {

    @Override
    public void requestFit(RequestMessagePayload requestMessagePayload, RequestContext requestContext) {
        Set<Class<?>> toolKitList = super.extractToolkitsForMetadata(requestContext);

        if (!toolKitList.isEmpty()) {
            requestMessagePayload.addTool(toolManager.getToolCallbackList(toolKitList));
        }
    }

}
