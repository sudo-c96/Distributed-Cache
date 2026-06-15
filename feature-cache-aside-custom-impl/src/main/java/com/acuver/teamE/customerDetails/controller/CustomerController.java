package com.acuver.teamE.customerDetails.controller;

import com.acuver.teamE.customerDetails.constants.IAppConstants;
import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> insertCustomer(@Validated @RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.saveCustomer(customer));
    }

    @GetMapping
    public CustomerResponse getAllCustomers(
            @RequestParam(value="pageNo", defaultValue = IAppConstants.DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(value="pageSize", defaultValue = IAppConstants.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value="sortBy", defaultValue = IAppConstants.DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(value="sortDir", defaultValue = IAppConstants.DEFAULT_SORT_DIRECTION, required = false) String sortDir
    ) {
        return customerService.getAllCustomers(pageNo,pageSize,sortBy,sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable(name = "id") String id) {
        return ResponseEntity.status(HttpStatus.FOUND).body(customerService.getCustomerById(id));
    }

    @GetMapping("/field")
    public ResponseEntity<List<Customer>> getCustomers(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gender){

        List<Customer> customers = customerService.getCustomers(id, age, minAge, maxAge, email, gender);

        if (customers == null){
            return ResponseEntity.notFound().build();
        }else {
            return ResponseEntity.ok(customers);
        }
    }

    @GetMapping("/new")
    public ResponseEntity<List<Customer>> getCustomersNew(
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String gender){
        List<Customer> customers = customerService.getCustomersNew(age, minAge, maxAge, email, gender);

        if (customers == null){
            return ResponseEntity.notFound().build();
        }else {
            return ResponseEntity.ok(customers);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomerById(@Validated @RequestBody Customer customer, @PathVariable(name = "id") String id) {
        return ResponseEntity.ok(customerService.updateCustomerById(customer,id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomerById(@PathVariable(name = "id") String id) {
        customerService.deleteCustomerById(id);
        return ResponseEntity.ok("Customer deleted successfully!");
    }

}
