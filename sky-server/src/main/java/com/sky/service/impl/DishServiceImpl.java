package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetMealDishMapper setMealDishMapper;

    @Transactional // 同时保存菜品和口味，需要保证方法是一个原子性的（要么全成功要么全失败）
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dish.setStatus(StatusConstant.DISABLE);
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值。（xml使用了useGeneratedKeys）
        Long dishId = dish.getId();
        // 向口味表插入N条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 批量插入
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 对菜品进行分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    public void delete(List<Long> ids) {
        // 判断当前菜品是否能够删除 -- 是否存在起售中的菜品？？
        List<Integer> statusOnsaleIds = dishMapper.statusOk(ids);
        log.info("总数:{}",statusOnsaleIds);
        if (statusOnsaleIds !=null && statusOnsaleIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        // 判断当前菜品是否能够删除 -- 是否被套餐关联？？
        List<Integer> setmealIds = setMealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        // 删除菜品表中的菜品数据
        log.info("总数:{}",setmealIds);
        // 删除菜品关联的口味数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            dishFlavorMapper.deleteByDishId(id);
//        }
        // 根据菜品id集合批量删除菜品数据
        dishMapper.deleteByIds(ids);
        // 根据菜品id集合批量删除口味数据
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getById(id);
        // 根据菜品id查询口味id
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        // 修改菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);

        // 修改菜品口味
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            // 批量插入
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
