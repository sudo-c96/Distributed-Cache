package com.acuver.teamE.customerDetails.service;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;

import java.util.List;

public interface CustomerService {
    Customer saveCustomer(Customer customer);

    CustomerResponse getAllCustomers(int pageNo, int pageSize, String sortBy, String sortDir);

    Customer getCustomerById(String id);

    Customer updateCustomerById(Customer customer, String id);

    void deleteCustomerById(String id);

}
