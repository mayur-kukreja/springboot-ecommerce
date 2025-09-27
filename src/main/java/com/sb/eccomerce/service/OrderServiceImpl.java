package com.sb.eccomerce.service;

import com.sb.eccomerce.exceptions.APIException;
import com.sb.eccomerce.exceptions.ResourceNotFoundException;
import com.sb.eccomerce.model.*;
import com.sb.eccomerce.payload.OrderDTO;
import com.sb.eccomerce.payload.OrderItemDTO;
import com.sb.eccomerce.repositries.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    CartRepository cartRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CartService cartService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ProductRepository productRepository;


//    @Transactional
//    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage){
//        Cart cart = cartRepository.findCartByEmail(emailId);
//
//        if (cart == null)
//            throw new ResourceNotFoundException("Cart","email",emailId);
//
//        Address address = addressRepository.findById(addressId).
//                orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));
//
//        Order order = new Order();
//        order.setEmail(emailId);
//        order.setOrderDate(LocalDate.now());
//        order.setTotalAmount(cart.getTotalPrice());
//        order.setOrderStatus("Order Accepted !");
//        order.setAddress(address);
//
//        Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
//        payment.setOrder(order);
//        payment = paymentRepository.save(payment);
//        order.setPayment(payment);
//
//        Order savedOrder = orderRepository.save(order);
//
//        List<CartItem> cartItems = cart.getCartItems();
//        if (cartItems.isEmpty())
//            throw new APIException("Cart is empty");
//
//        List<OrderItem> orderItems = new ArrayList<>();
//        for (CartItem cartItem : cartItems){
//            OrderItem orderItem = new OrderItem();
//            orderItem.setProduct(cartItem.getProduct());
//            orderItem.setQuantity(cartItem.getQuantity());
//            orderItem.setDiscount(cartItem.getDiscount());
//            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
//            orderItem.setOrder(savedOrder);
//            orderItems.add(orderItem);
//        }
//
//        orderItems = orderItemRepository.saveAll(orderItems);
//
//        cart.getCartItems().forEach(item -> {
//            int quantity = item.getQuantity();
//            Product product = item.getProduct();
//
//            product.setQuantity(product.getQuantity() - quantity);
//
//            productRepository.save(product);
//
//            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());
//
//        });
//
//        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
//        orderItems.forEach(item -> orderDTO.getOrderItems().add(
//                modelMapper.map(item, OrderItemDTO.class)));
//
//        orderDTO.setAddressId(addressId);
//
//        return orderDTO;
//
//    }

    @Transactional
    @Override
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId,String pgStatus, String pgResponseMessage) {

        Cart cart = cartRepository.findCartByEmail(emailId);

        if (cart == null)
            throw new ResourceNotFoundException("Cart","email",emailId);

        Address address = addressRepository.findById(addressId).
                orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted !");
        order.setAddress(address);

        Payment payment = new Payment(paymentMethod, pgPaymentId, pgStatus, pgResponseMessage, pgName);
        payment.setOrder(order);
        payment = paymentRepository.save(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty())
            throw new APIException("Cart is empty");

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems){
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        orderItems = orderItemRepository.saveAll(orderItems);

        cart.getCartItems().forEach(item -> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();

            product.setQuantity(product.getQuantity() - quantity);

            productRepository.save(product);

            cartService.deleteProductFromCart(cart.getCartId(), item.getProduct().getProductId());

        });

        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);
        orderItems.forEach(item -> orderDTO.getOrderItems().add(
                modelMapper.map(item, OrderItemDTO.class)));

        orderDTO.setAddressId(addressId);

        return orderDTO;

    }
}
