package com.w.esshop.controller;

import com.w.esshop.common.Result;
import com.w.esshop.dto.PayVO;
import com.w.esshop.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器
 */
@Tag(name = "支付模块", description = "模拟支付收银台（未对接真实支付网关）")
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Operation(summary = "创建支付单（打开收银台）")
    @PostMapping("/create")
    public Result<PayVO> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        String payMethod = body.get("payMethod") == null ? "MOCK" : String.valueOf(body.get("payMethod"));
        return Result.success(paymentService.createPayment(userId, orderId, payMethod));
    }

    @Operation(summary = "确认支付（模拟支付成功）")
    @PostMapping("/confirm")
    public Result<PayVO> confirm(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String payNo = String.valueOf(body.get("payNo"));
        return Result.success("支付成功", paymentService.confirmPayment(userId, payNo));
    }

    @Operation(summary = "查询订单支付信息")
    @GetMapping("/order/{orderId}")
    public Result<PayVO> getByOrderId(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(paymentService.getByOrderId(userId, orderId));
    }
}
