package com.sb.eccomerce.service;

import com.sb.eccomerce.exceptions.APIException;
import com.sb.eccomerce.exceptions.ResourceNotFoundException;
import com.sb.eccomerce.model.Cart;
import com.sb.eccomerce.model.Category;
import com.sb.eccomerce.model.Product;
import com.sb.eccomerce.payload.CartDTO;
import com.sb.eccomerce.payload.ProductDTO;
import com.sb.eccomerce.payload.ProductResponse;
import com.sb.eccomerce.repositries.CartRepository;
import com.sb.eccomerce.repositries.CategoryRepository;
import com.sb.eccomerce.repositries.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private CartRepository cartRepository;

    @Value("${project.image}")
    private String path;

    @Autowired
    private CartService cartService;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO){
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Category","categoryId",categoryId));

        boolean isProductPresent = true;

        List<Product> products = category.getProducts();
        for (Product value : products){
            if (value.getProductName().equals(productDTO.getProductName())){
                isProductPresent = false;
                break;

            }
        }

        if (isProductPresent){
            Product product = modelMapper.map(productDTO, Product.class);
            product.setImage("default.png");
            product.setCategory(category);
            double specialPrice =  product.getPrice()-
                    ((product.getDiscount() * 0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);

            Product savedProduct = productRepository.save(product);

            return modelMapper.map(savedProduct, ProductDTO.class);
        } else{
            throw  new APIException("Product already exist!!");
        }
    }

    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyWord, String category){
        Sort sortByAndOrder =  sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize,sortByAndOrder);

       Specification<Product> spec = Specification.where(null);
        if ( keyWord != null && ! keyWord.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" +  keyWord.toLowerCase() + "%"));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }
        Page<Product> pageProducts =  productRepository.findAll(spec,pageDetails);

        List<Product> products = pageProducts.getContent();

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;

    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder){
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()->
                        new ResourceNotFoundException("category","categoryId",categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ?Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByCategoryOrderByPriceAsc(category, pageDetails);

        List<Product> products = pageProducts.getContent();

        if (products.isEmpty()){
            throw new APIException(category.getCategoryName()+"category does not have any product");
        }

        List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class)).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;


    }

    public ProductResponse searchProductByKeyword(String keyword,Integer pageNumber, Integer pageSize, String sortBy, String sortOrder){
       Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
               ?Sort.by(sortBy).ascending()
               :Sort.by(sortBy).descending();

       Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
       Page<Product> pageProducts = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%',pageDetails);

       List<Product> products = pageProducts.getContent();

       if(products.isEmpty()){
            throw new APIException("Product not found with keyword: "+ keyword);
       }

       List<ProductDTO> productDTOS = products.stream()
                .map(product -> modelMapper.map(product, ProductDTO.class)).toList();

       ProductResponse productResponse = new ProductResponse();
       productResponse.setContent(productDTOS);
       productResponse.setPageNumber(pageProducts.getNumber());
       productResponse.setPageSize(pageProducts.getSize());
       productResponse.setTotalElements(pageProducts.getTotalElements());
       productResponse.setTotalPages(pageProducts.getTotalPages());
       productResponse.setLastPage(pageProducts.isLast());

       return  productResponse;
    }

    public ProductDTO updateProduct(Long productId, ProductDTO productDTO){
        Product productFromDb =  productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("product","productId",productId));

        Product product = modelMapper.map(productDTO, Product.class);

        productFromDb.setProductName(product.getProductName());
        productFromDb.setDescription(product.getDescription());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setDiscount(product.getDiscount());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setSpecialPrice(product.getSpecialPrice());

        Product savedProduct =  productRepository.save(productFromDb);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().map(
                    p -> modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());
            cartDTO.setProducts(products);

            return cartDTO;
        }).collect(Collectors.toList());

        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(),productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    public ProductDTO deleteProduct(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("product","productId",productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException{
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        String fileName = fileService.uploadImage(path,image);
        productFromDb.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);

    }



}
