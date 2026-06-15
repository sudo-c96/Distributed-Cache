package com.acuver.teamE.customerDetails.service.impl;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.exception.ResourceNotFoundException;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import com.acuver.teamE.customerDetails.service.CustomerService;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RMapCache<String, Customer> customerRMapCache;

    //Post API using Write Through
    @Override
    public Customer saveCustomer(Customer customer) {
        customer.setId(UUID.randomUUID().toString());
        customerRMapCache.put(customer.getId(), customer);
        return customer;
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

    //Get API using Read Through
    @Override
    public Customer getCustomerById(String id) {
        Customer fetchedCustomer = customerRMapCache.get(id);
        if (fetchedCustomer == null)
            throw new ResourceNotFoundException("Customer","id",id);
        return fetchedCustomer;
    }

    //Update API using Write Through
    @Override
    public Customer updateCustomerById(Customer customer, String id) {
        Customer fetchedCustomer = customerRMapCache.get(id);
        if (fetchedCustomer == null)
            throw new ResourceNotFoundException("Customer","id",id);

        fetchedCustomer.setFirstName(customer.getFirstName() == null ? fetchedCustomer.getFirstName() : customer.getFirstName());
        fetchedCustomer.setLastName(customer.getLastName() == null ? fetchedCustomer.getLastName() : customer.getLastName());
        fetchedCustomer.setGender(customer.getGender() == null ? fetchedCustomer.getGender() : customer.getGender());
        fetchedCustomer.setAge(customer.getAge() == null ? fetchedCustomer.getAge() : customer.getAge());
        fetchedCustomer.setContactNo(customer.getContactNo() == null ? fetchedCustomer.getContactNo() : customer.getContactNo());
        fetchedCustomer.setEmailId(customer.getEmailId() == null ? fetchedCustomer.getEmailId() : customer.getEmailId());

        customerRMapCache.put(id, fetchedCustomer);
        return fetchedCustomer;
    }

    //Delete API using Write Through
    @Override
    public void deleteCustomerById(String id) {
        Customer fetchedCustomer = customerRMapCache.get(id);
        if (fetchedCustomer != null)
            customerRMapCache.remove(id);
        else
            throw new ResourceNotFoundException("Customer","id",id);
    }

}
