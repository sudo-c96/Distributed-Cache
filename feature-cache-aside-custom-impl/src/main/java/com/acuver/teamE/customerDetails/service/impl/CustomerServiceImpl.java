package com.acuver.teamE.customerDetails.service.impl;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.acuver.teamE.customerDetails.entity.response.CustomerResponse;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import com.acuver.teamE.customerDetails.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
@CacheConfig(cacheNames = {"Customer"})
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CacheManager cacheManager;

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
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));
        return fetchedCustomer;
    }

    @Override
    @Cacheable(key = "{#root.methodName, #id, #age, #minAge, #maxAge, #email, #gender}",
            unless = "#result == null || #result.isEmpty()")
    public List<Customer> getCustomers(String id, Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        List<Customer> fetchedCustomers = new ArrayList<>();
        if (id != null) {
            log.info("fetching data from database for id: {}",id);
            Optional<Customer> customerOptional = customerRepository.findById(id);
            customerOptional.ifPresent(fetchedCustomers::add);
        } else if (age != null) {
            log.info("fetching data from database for age: {}",age);
            fetchedCustomers = customerRepository.findByAge(age);
        } else if (minAge != null && maxAge != null) {
            log.info("fetching data from database for age between a range of: {} and {}",minAge,maxAge);
            fetchedCustomers = customerRepository.findByAgeBetween(minAge, maxAge);
        } else if (email != null) {
            log.info("fetching data from database for email: {}",email);
            fetchedCustomers = customerRepository.findByEmailId(email);
        } else if (minAge != null) {
            log.info("fetching data from database for age greater than: {}",minAge);
            fetchedCustomers = customerRepository.findByAgeGreaterThan(minAge);
        } else if (maxAge != null) {
            log.info("fetching data from database for age less than: {}",maxAge);
            fetchedCustomers = customerRepository.findByAgeLessThan(maxAge);
        } else if (gender != null) {
            log.info("fetching data from database for gender: {}",gender);
            fetchedCustomers = customerRepository.findByGender(gender);
        }

        return fetchedCustomers.isEmpty() ? null : fetchedCustomers;
    }

    @Override
    public List<Customer> getCustomersNew(Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        String cacheKey = getCacheKey(age, minAge, maxAge, email, gender);

        Cache cache = cacheManager.getCache("Customer");

        // Check if the cache contains the data for the specified criteria
        if (cache != null) {
            List<Customer> cachedCustomers = cache.get(cacheKey, List.class);

            if (cachedCustomers == null) {
                // If the initial search in cache returns null, try with adjusted age criteria
                List<Customer> filteredCustomers = searchWithAdjustedAge(cache, age, minAge, maxAge, email, gender);

                if (!filteredCustomers.isEmpty()) {
                    cache.put(cacheKey, filteredCustomers);
                    log.info("Fetching data from the cache");
                    return filteredCustomers;
                }
            } else {
                log.info("Fetching data from the cache");
                return cachedCustomers;
            }
        }

        // If data is not found in the cache, fetch from the database
        log.info("Fetching data from the database");
        List<Customer> fetchedCustomers = fetchDataFromDatabase(age, minAge, maxAge, email, gender);

        // Cache the fetched data
        if (!fetchedCustomers.isEmpty()) {
            cache.put(cacheKey, fetchedCustomers);
        }

        return fetchedCustomers;
    }

    // Helper method to construct a cache key based on criteria
    private String getCacheKey(Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        return "age=" + age + ",minAge=" + minAge + ",maxAge=" + maxAge + ",email=" + email + ",gender=" + gender;
    }

    // Helper method to perform iterative search with adjusted age criteria
    private List<Customer> searchWithAdjustedAge(Cache cache, Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        if(minAge != null && maxAge==null){
            for (int i = 0; i <= minAge; i++) {
                // Adjust minAge criteria
                Integer adjustedMinAge =Math.max(minAge - i, 0);
                String adjustedCacheKey = getCacheKey(age, adjustedMinAge, maxAge, email, gender);
                List<Customer> adjustedCustomers = cache.get(adjustedCacheKey, List.class);

                if (adjustedCustomers != null) {
                    // Filter the cached data based on additional criteria
                    List<Customer> filteredCustomers = filterCustomers(adjustedCustomers, age, minAge, maxAge, email, gender);

                    if (!filteredCustomers.isEmpty()) {
                        return filteredCustomers;
                    }
                }
            }
        } else if (minAge == null && maxAge != null) {
            for (int i = 0; i <= 100-maxAge; i++) {
                // Adjust maxAge criteria
                Integer adjustedMaxAge =Math.min(maxAge + i, 100);
                String adjustedCacheKey = getCacheKey(age, minAge, adjustedMaxAge, email, gender);
                List<Customer> adjustedCustomers = cache.get(adjustedCacheKey, List.class);

                if (adjustedCustomers != null) {
                    // Filter the cached data based on additional criteria
                    List<Customer> filteredCustomers = filterCustomers(adjustedCustomers, age, minAge, maxAge, email, gender);

                    if (!filteredCustomers.isEmpty()) {
                        return filteredCustomers;
                    }
                }
            }
        } else if (minAge !=null && maxAge!=null) {
            for (int minDiff = 0; minDiff <= minAge; minDiff++) {
                for (int maxDiff = 0; maxDiff <= 100-maxAge; maxDiff++) {
                    // Adjust minAge and maxAge criteria
                    Integer adjustedMinAge =Math.max(minAge - minDiff, 0);
                    Integer adjustedMaxAge =Math.min(maxAge + maxDiff, 100);

                    String adjustedCacheKey = getCacheKey(age, adjustedMinAge, adjustedMaxAge, email, gender);
                    List<Customer> adjustedCustomers = cache.get(adjustedCacheKey, List.class);

                    if (adjustedCustomers != null) {
                        // Filter the cached data based on additional criteria
                        List<Customer> filteredCustomers = filterCustomers(adjustedCustomers, age, minAge, maxAge, email, gender);

                        if (!filteredCustomers.isEmpty()) {
                            return filteredCustomers;
                        }
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    // Helper method to filter customers based on additional criteria
    private List<Customer> filterCustomers(List<Customer> customers, Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        List<Customer> filteredCustomers = new ArrayList<>();
        for (Customer customer : customers) {
            boolean ageFilter = (age == null) || (age <= customer.getAge());
            boolean minAgeFilter = (minAge == null) || (minAge <= customer.getAge());
            boolean maxAgeFilter = (maxAge == null) || (maxAge >= customer.getAge());
            boolean emailFilter = (email == null) || email.equals(customer.getEmailId());
            boolean genderFilter = (gender == null) || gender.equals(customer.getGender());

            if (ageFilter && minAgeFilter && maxAgeFilter && emailFilter && genderFilter) {
                filteredCustomers.add(customer);
            }
        }
        return filteredCustomers;
    }
    // Helper method to fetch data from the database based on criteria
    private List<Customer> fetchDataFromDatabase(Integer age, Integer minAge, Integer maxAge, String email, String gender) {
        if (age != null) {
            log.info("fetching data from database for age: {} " , age);
            return customerRepository.findByAge(age);
        } else if (minAge != null && maxAge != null) {
            log.info("fetching data from database for age between a range of: {} and  {}", minAge, maxAge);
            return customerRepository.findByAgeBetween(minAge, maxAge);
        } else if (email != null) {
            log.info("fetching data from database for email: {}", email);
            return customerRepository.findByEmailId(email);
        } else if (minAge != null) {
            log.info("fetching data from database for age greater than: {}" , minAge);
            return customerRepository.findByAgeGreaterThan(minAge);
        } else if (maxAge != null) {
            log.info("fetching data from database for age less than: {}", maxAge);
            return customerRepository.findByAgeLessThan(maxAge);
        } else if (gender != null) {
            log.info("fetching data from database for gender: {}", gender);
            return customerRepository.findByGender(gender);
        } else {
            // If no criteria are specified, return an empty list or handle as needed
            return new ArrayList<>();
        }
    }



    @Override
    @CachePut(key = "#id")
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
    @CacheEvict(key = "#id")
    public void deleteCustomerById(String id) {
        Customer fetchedCustomer = customerRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Resource not found"));
        customerRepository.delete(fetchedCustomer);
    }

}
