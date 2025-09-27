package com.sb.eccomerce.service;

import com.sb.eccomerce.exceptions.APIException;
import com.sb.eccomerce.exceptions.ResourceNotFoundException;
import com.sb.eccomerce.model.Cart;
import com.sb.eccomerce.model.CartItem;
import com.sb.eccomerce.model.Product;
import com.sb.eccomerce.payload.CartDTO;
import com.sb.eccomerce.payload.ProductDTO;
import com.sb.eccomerce.repositries.CartItemRepository;
import com.sb.eccomerce.repositries.CartRepository;
import com.sb.eccomerce.repositries.ProductRepository;
import com.sb.eccomerce.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ModelMapper modelMapper;

    public CartDTO addProductToCart(Long productId, Integer quantity){
        Cart cart = createCart();

        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product","productId",productId));

        CartItem cartItem  = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null){
            throw new APIException("Product"+ product.getProductName()+ "already exists in the cart");
        }

        if(product.getQuantity() == 0){
            throw new APIException(product.getProductName()+" is not available");
        }

        if (product.getQuantity() < quantity){
            throw new APIException("Please ,make an order of the"+ product.getProductName()+ "less than or equal to the quantity"+product.getQuantity()+".");
        }

        CartItem newCartItem =new CartItem();

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        cart.setTotalPrice(cart.getTotalPrice()+(product.getSpecialPrice()*quantity));

        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
           ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class) ;
           map.setQuantity(item.getQuantity());
           return map;
        });

        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    public List<CartDTO> getAllCarts(){
        List<Cart> carts = cartRepository.findAll();

        if (carts.size() == 0){
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().map(
                    cartItem -> {
                        ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                        productDTO.setQuantity(cartItem.getQuantity());
                        return productDTO;
                    }).collect(Collectors.toList());

            cartDTO.setProducts(products);

            return cartDTO;
        }).collect(Collectors.toList());

        return cartDTOS;
    }

    public CartDTO getCart(String emailId, Long cartId){
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);

        if (cart == null){
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cart.getCartItems().forEach(c -> c.getProduct().setQuantity(c.getQuantity()));

        List<ProductDTO> products = cart.getCartItems().stream().map(p ->
                modelMapper.map(p.getProduct(), ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity){
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart", "cartid", cartId));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0){
            throw new APIException(product.getProductName()+" is not available");
        }

        if (product.getQuantity() < quantity){
            throw new APIException("Please, make an order of the "+ product.getProductName()+ " less than or equal to the quantity"+ product.getQuantity()+".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null){
            throw new APIException("Product"+ product.getProductName()+" not available in the cart!!");
        }

        int newQuantity = cartItem.getQuantity() + quantity;

        if (newQuantity < 0){
            throw  new APIException("The resulting quantity cannot be negative");
        }

        if(newQuantity == 0){
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity()+quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice()* quantity));
            cartRepository.save(cart);
        }

        CartItem updateItem = cartItemRepository.save(cartItem);

        if (updateItem.getQuantity() == 0){
            cartItemRepository.deleteById(updateItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        });

        cartDTO.setProducts(productDTOStream.toList());

        return cartDTO;
    }

    @Transactional
    public String deleteProductFromCart(Long cartId, Long productId){
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null){
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice()-(cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product" + cartItem.getProduct().getProductName()+ " removed from the cart !!!";
    }

    public void updateProductInCarts(Long cartId, Long productId){
        Cart cart = cartRepository.findById(cartId).orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if(cartItem == null){
            throw new APIException("Product"+ product.getProductName()+ " not available in the cart!!!");
        }

        double cartPrice = cart.getTotalPrice()-(cartItem.getProductPrice()*cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);

    }

    private Cart createCart(){
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());

        return cartRepository.save(cart);
    }


}
