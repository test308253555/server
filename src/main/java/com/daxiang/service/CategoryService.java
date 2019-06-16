package com.daxiang.service;

import com.daxiang.mbg.mapper.CategoryMapper;
import com.daxiang.mbg.po.Category;
import com.daxiang.mbg.po.Page;
import com.daxiang.model.PageRequest;
import com.daxiang.model.Response;
import com.daxiang.mbg.po.CategoryExample;
import com.daxiang.model.UserCache;
import com.daxiang.model.vo.CategoryVo;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jiangyitao.
 */
@Service
public class CategoryService extends BaseService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private PageService pageService;

    /**
     * 添加分类
     *
     * @param category
     * @return
     */
    public Response add(Category category) {
        category.setCreateTime(new Date());
        category.setCreatorUid(getUid());

        int insertRow;
        try {
            insertRow = categoryMapper.insertSelective(category);
        } catch (DuplicateKeyException e) {
            return Response.fail("命名冲突");
        }
        return insertRow == 1 ? Response.success("添加Category成功") : Response.fail("添加Category失败");
    }

    public Response delete(Integer categoryId) {
        if (categoryId == null) {
            return Response.fail("categoryId不能为空");
        }

        // 检查该分类下是否有page
        Page query = new Page();
        query.setCategoryId(categoryId);
        List<Page> pages = pageService.selectByPage(query);
        if (!CollectionUtils.isEmpty(pages)) {
            return Response.fail("分类下有page，无法删除");
        }

        int deleteRow = categoryMapper.deleteByPrimaryKey(categoryId);
        return deleteRow == 1 ? Response.success("删除分类成功") : Response.fail("删除分类失败，请稍后重试");
    }

    /**
     * 查询列表
     *
     * @return
     */
    public Response list(Category category, PageRequest pageRequest) {
        boolean needPaging = pageRequest.needPaging();
        if (needPaging) {
            PageHelper.startPage(pageRequest.getPageNum(), pageRequest.getPageSize());
        }

        List<Category> categories = selectByCategory(category);
        List<CategoryVo> categoryVos = categories.stream().map(c -> CategoryVo.convert(c, UserCache.getNickNameById(c.getCreatorUid()))).collect(Collectors.toList());

        if (needPaging) {
            // java8 stream会导致PageHelper total计算错误
            // 所以这里用categories计算total
            long total = com.daxiang.model.Page.getTotal(categories);
            return Response.success(com.daxiang.model.Page.build(categoryVos, total));
        } else {
            return Response.success(categoryVos);
        }
    }

    public List<Category> selectByCategory(Category category) {
        if (category == null) {
            category = new Category();
        }

        CategoryExample categoryExample = new CategoryExample();
        CategoryExample.Criteria criteria = categoryExample.createCriteria();

        if (category.getId() != null) {
            criteria.andIdEqualTo(category.getId());
        }
        if (category.getProjectId() != null) {
            criteria.andProjectIdEqualTo(category.getProjectId());
        }
        if (category.getType() != null) {
            criteria.andTypeEqualTo(category.getType());
        }
        if (!StringUtils.isEmpty(category.getName())) {
            criteria.andNameEqualTo(category.getName());
        }
        categoryExample.setOrderByClause("create_time desc");

        return categoryMapper.selectByExample(categoryExample);
    }

}
