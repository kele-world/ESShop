package com.w.esshop.controller;

import com.w.esshop.common.PageResult;
import com.w.esshop.common.Result;
import com.w.esshop.dto.OrderDTO;
import com.w.esshop.dto.OrderQueryDTO;
import com.w.esshop.entity.Order;
import com.w.esshop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 */
@Tag(name = "订单模块", description = "下单、订单状态流转、订单查询")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "创建订单（下单）")
    @PostMapping("/create")
    public Result<Order> create(@Valid @RequestBody OrderDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Order order = orderService.create(userId, dto);
        return Result.success("下单成功", order);
    }

    @Operation(summary = "我买到的订单")
    @GetMapping("/my")
    public Result<PageResult<Order>> myOrders(OrderQueryDTO query, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.pageMyOrders(userId, query));
    }

    @Operation(summary = "我卖出的订单")
    @GetMapping("/my-sold")
    public Result<PageResult<Order>> mySold(OrderQueryDTO query, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(orderService.pageMySold(userId, query));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/detail/{id}")
    public Result<Order> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @Operation(summary = "付款")
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.pay(userId, id);
        return Result.success("付款成功");
    }

    @Operation(summary = "完成订单")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.complete(userId, id);
        return Result.success("订单已完成");
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.cancel(userId, id);
        return Result.success("订单已取消");
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.deleteOrder(userId, id);
        return Result.success("已删除");
    }
}