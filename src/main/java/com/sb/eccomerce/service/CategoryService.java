package com.sb.eccomerce.service;

import com.sb.eccomerce.model.Category;
import com.sb.eccomerce.payload.CategoryDTO;
import com.sb.eccomerce.payload.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    CategoryDTO createCategory(CategoryDTO categoryDTO);

    CategoryDTO deleteCategory(Long categoryId);

    CategoryDTO updateCategory(CategoryDTO categoryDTO,Long categoryId);


}
