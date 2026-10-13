package com.bin.actx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * WebUI 控制器，用于渲染页面
 *
 * @author Bin
 * @since 2026/5/12 11:33
 */
@Controller("/")
public class WebUIController {

    @GetMapping
    public String index() {
        return "index";
    }

}
