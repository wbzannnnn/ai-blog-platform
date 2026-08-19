package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.entity.Tags;
import com.lanou.springaidemo.mapper.TagsMapper;
import com.lanou.springaidemo.service.TagsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Service
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {

}
