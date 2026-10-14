package com.w.esshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 客服回复
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyVO {

    /** 回复内容 */
    private String content;

    /** 是否命中知识库（智能客服回复时恒为 true） */
    private Boolean matched;

    /** 命中的知识库分类 */
    private String category;

    /** 回复来源: AI-大模型, RULE-规则库 */
    private String source;

    /** 推荐问题（未命中时给出，引导用户） */
    private List<String> suggestions;
}