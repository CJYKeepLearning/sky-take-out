package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);
    void insert(Dish dish);
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);
    List<Integer> statusOk(List<Long> ids);
    /**
     * 根据主键删除菜品
     * @param id
     */
    @Delete("delete from dish where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id集合批量删除菜品
     * @param ids
     */
    void deleteByIds(List<Long> ids);

    /**
     * 根据id获取菜品
     * @param id
     * @return
     */
    Dish getById(Long id);

    /**
     * 修改菜品
     * @param dish
     */
    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);
}
