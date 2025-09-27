package com.sb.eccomerce.repositries;

import com.sb.eccomerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long>  {
}
    