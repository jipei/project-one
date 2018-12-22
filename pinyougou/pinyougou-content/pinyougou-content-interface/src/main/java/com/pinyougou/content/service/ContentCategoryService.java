package com.pinyougou.content.service;

import com.pinyougou.pojo.TbContent;
import com.pinyougou.pojo.TbContentCategory;
import com.pinyougou.service.BaseService;
import com.pinyougou.vo.PageResult;

import java.util.List;

public interface ContentCategoryService extends BaseService<TbContentCategory> {

    PageResult search(Integer page, Integer rows, TbContentCategory contentCategory);


}