package com.acuver.teamE.customerDetails.service.impl;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.exception.ResourceNotFoundException;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import com.acuver.teamE.customerDetails.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@CacheConfig(cacheNames = {"Customer"})
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

    @Override
    @Cacheable(key = "#id")
    public Customer getCustomerById(String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer","id",id));
        return fetchedCustomer;
    }

    @Override
    @CachePut(key = "#id")
    public Customer updateCustomerById(Customer customer, String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer","id",id));

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
    @CacheEvict(key = "#id")
    public void deleteCustomerById(String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer","id",id));
        customerRepository.delete(fetchedCustomer);
    }

}
