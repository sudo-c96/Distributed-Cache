package com.acuver.teamE.customerDetails.repository;

import com.acuver.teamE.customerDetails.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    List<Customer> findByAge(Integer age);
    List<Customer> findByAgeBetween(Integer minAge, Integer maxAge);
    List<Customer> findByEmailId(String email);
    List<Customer> findByAgeGreaterThan(Integer minAge);
    List<Customer> findByAgeLessThan(Integer maxAge);
    List<Customer> findByGender(String gender);

}
