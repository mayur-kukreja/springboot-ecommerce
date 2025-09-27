package com.sb.eccomerce.service;

import com.sb.eccomerce.exceptions.APIException;
import com.sb.eccomerce.exceptions.ResourceNotFoundException;
import com.sb.eccomerce.model.Category;
import com.sb.eccomerce.payload.CategoryDTO;
import com.sb.eccomerce.payload.CategoryResponse;
import com.sb.eccomerce.repositries.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageableDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageableDetails);

        List<Category> categories = categoryPage.getContent();
        if (categories.isEmpty()){
            throw new APIException("No Category Created till now");
        }

        List<CategoryDTO> categoryDTOS =  categories.stream().map(category -> modelMapper.map(category, CategoryDTO.class)).toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());

        return categoryResponse;
    }

    public CategoryDTO createCategory(CategoryDTO categoryDTO){
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category categoryFromDb = categoryRepository.findByCategoryName(category.getCategoryName());
        if (categoryFromDb != null){
            throw  new APIException("Category with the name "+category.getCategoryName()+" already exist");
        }
        Category savedCategory = categoryRepository.save(category);
        return modelMapper.map(savedCategory,CategoryDTO.class);
    }

    public CategoryDTO deleteCategory(Long categoryId){
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                ()->new ResourceNotFoundException("Category","categoryId",categoryId));
        categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
    }

    public CategoryDTO updateCategory(CategoryDTO categoryDTO,Long categoryId){
      Category savedCategory = categoryRepository.findById(categoryId).orElseThrow(
              ()->new ResourceNotFoundException("Category","categoryId",categoryId));
      Category category = modelMapper.map(categoryDTO, Category.class);
      category.setCategoryId(categoryId);
      savedCategory = categoryRepository.save(category);
      return modelMapper.map(savedCategory, CategoryDTO.class);
    }


 }
