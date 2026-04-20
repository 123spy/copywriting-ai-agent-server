package com.spy.server.common;

import com.spy.server.constant.CommonConstant;
import lombok.Data;

@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 50;

    /**
     * 搜索字段
     */
    private String searchText;

    /**
     * 排序字段
     */
    private String sortField = "id";

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
