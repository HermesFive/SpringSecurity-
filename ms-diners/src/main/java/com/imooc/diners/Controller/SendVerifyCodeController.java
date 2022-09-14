package com.imooc.diners.Controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.diners.service.SendVerifyCoderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class SendVerifyCodeController {

    @Resource
    private SendVerifyCoderService sendVerifyCoderService;

    @Resource
    private HttpServletRequest httpServletRequest;

    @GetMapping("send")
    public ResultInfo send(String phone){
        sendVerifyCoderService.send(phone);
        return ResultInfoUtil.buildSuccess("发送成功",httpServletRequest.getServletPath());
    }
}
