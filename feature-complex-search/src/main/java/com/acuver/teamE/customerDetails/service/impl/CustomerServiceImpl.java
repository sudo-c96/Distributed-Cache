package com.acuver.teamE.customerDetails.service.impl;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import com.acuver.teamE.customerDetails.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Customer saveCustomer(Customer customer) {
        Customer newCustomer = customerRepository.save(customer);
        return newCustomer;
    }

    @Override
    public CustomerResponse getAllCustomers(int pageNo, int pageSize, String sortBy, String sortDir) {
        //Sorting dynamically based on Input
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        //Creating Pageable instance
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        //Creating Page object
        Page<Customer> customers = customerRepository.findAll(pageable);

        //Getting content from Page object
        List<Customer> listOfCustomers = customers.getContent();


        CustomerResponse customerResponse = new CustomerResponse();
        customerResponse.setContent(listOfCustomers);
        customerResponse.setPageNo(customers.getNumber());
        customerResponse.setPageSize(customers.getSize());
        customerResponse.setCount(customers.getTotalElements());
        customerResponse.setTotalPages(customers.getTotalPages());
        customerResponse.setLast(customers.isLast());

        return customerResponse;
    }

//    @Override
//    public Customer getCustomerById(String id) {
//        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));
//        return fetchedCustomer;
//    }

//    @Cacheable(value = "customers", key = "{#root.methodName, #id, #age, #minAge, #maxAge, #email, #gender}")
    @Override
    public List<Customer> getCustomers(String id, Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        List<Customer> fetchedCustomers = new ArrayList<>();

        if (id!=null){
            Optional<Customer> customerOptional = customerRepository.findById(id);
            customerOptional.ifPresent(fetchedCustomers::add);
        } else if (age != null) {
            fetchedCustomers = customerRepository.findByAge(age);
        } else if (minAge != null && maxAge != null) {
            fetchedCustomers = customerRepository.findByAgeBetween(minAge, maxAge);
        } else if (email != null) {
            fetchedCustomers = customerRepository.findByEmailId(email);
        } else if (minAge != null) {
            fetchedCustomers = customerRepository.findByAgeGreaterThan(minAge);
        } else if (maxAge != null) {
            fetchedCustomers = customerRepository.findByAgeLessThan(maxAge);
        } else if (gender != null) {
            fetchedCustomers = customerRepository.findByGender(gender);
        }
        return fetchedCustomers.isEmpty() ? null : fetchedCustomers;
    }

    @Override
    public Customer updateCustomerById(Customer customer, String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));

        fetchedCustomer.setFirstName(customer.getFirstName() == null ? fetchedCustomer.getFirstName() : customer.getFirstName());
        fetchedCustomer.setLastName(customer.getLastName() == null ? fetchedCustomer.getLastName() : customer.getLastName());
        fetchedCustomer.setGender(customer.getGender() == null ? fetchedCustomer.getGender() : customer.getGender());
        fetchedCustomer.setAge(customer.getAge() == null ? fetchedCustomer.getAge() : customer.getAge());
        fetchedCustomer.setContactNo(customer.getContactNo() == null ? fetchedCustomer.getContactNo() : customer.getContactNo());
        fetchedCustomer.setEmailId(customer.getEmailId() == null ? fetchedCustomer.getEmailId() : customer.getEmailId());

        Customer updatedCustomer =customerRepository.save(fetchedCustomer);
        return updatedCustomer;
    }

    @Override
    public void deleteCustomerById(String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));
        customerRepository.delete(fetchedCustomer);
    }

}
