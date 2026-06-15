package com.acuver.teamE.customerDetails.config;

import com.acuver.teamE.customerDetails.entity.Customer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import com.acuver.teamE.customerDetails.repository.CustomerRepository;
import org.redisson.api.MapOptions;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.MapLoader;
import org.redisson.api.map.MapWriter;
import org.redisson.config.Config;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Data
@Getter
@Setter
@Configuration
@Slf4j
public class RedisConfig implements InitializingBean {

    private final String CACHE_NAME="Customer";

    private RedissonClient redissonClient;

    @Autowired
    private CustomerRepository customerRepository;

    @Bean
    public RMapCache<String, Customer> customerRMapCache() {
        final RMapCache<String, Customer> customerRMapCache = this.redissonClient.getMapCache(CACHE_NAME, MapOptions.<String, Customer>defaults()
                .loader(mapLoader)
                .writer(getMapWriter())
                .writeMode(MapOptions.WriteMode.WRITE_THROUGH));
        return customerRMapCache;
    }
    @Bean
    public RedissonClient redissonClient() {
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }

    private MapWriter<String, Customer> getMapWriter() {
        return new MapWriter<String, Customer>() {

            @Override
            public void write(final Map<String, Customer> map) {
                map.forEach( (k, v) -> {
                    customerRepository.save(v);
                    log.info("Customer saved to the Database through the Cache Client: "+v.getId());
                });
            }

            @Override
            public void delete(Collection<String> keys) {
                keys.stream().forEach(e -> {
                    customerRepository.delete(customerRepository.findById(e).orElseThrow(() -> new NoSuchElementException("Resource Not Found")));
                    log.info("Customer deleted from Database through the Cache Client: "+e);
                });
            }
        };
    }

    MapLoader<String, Customer> mapLoader = new MapLoader<String, Customer>() {
        @Override
        public Iterable<String> loadAllKeys() {
            List<String> result = customerRepository.findAll().stream().map(customer -> customer.getId()).collect(Collectors.toList());
            log.info("List of Customer IDs fetched from Database by the Cache Client: "+result);
            return result;
        }

        @Override
        public Customer load(String key) {
            Customer foundCustomer = customerRepository.findById(key).orElseThrow(() -> new NoSuchElementException("Resource not found"));
            log.info("Customer ID found from Database by the Cache Client: "+foundCustomer.getId());
            return foundCustomer;
        }
    };


    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(300))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .withCacheConfiguration("Customer",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(300)));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6379");
        this.redissonClient = Redisson.create(config);
    }

    /**
     * Handling multiple cache - customerCacheByName
     *
     *
    @Bean
    public RMapCache<String, Customer> customerNameRMapCache() {
        final RMapCache<String, Customer> customerNameRMapCache = this.redissonClient.getMapCache("CustomerNameCache", MapOptions.<String, Customer>defaults()
                .writer(getCustomerNameMapWriter())
                .writeMode(MapOptions.WriteMode.WRITE_THROUGH));
        return customerNameRMapCache;
    }

    private MapWriter<String, Customer> getCustomerNameMapWriter() {
        return new MapWriter<String, Customer>() {
            @Override
            public void write(Map<String, Customer> map) {
                map.forEach( (k, v) -> {
                    customerRepository.save(v);
                    log.info("Customer saved in the Database from the Cache: "+v.getFirstName());
                });
            }

            @Override
            public void delete(Collection<String> collection) {
                collection.stream().forEach(e -> {
                    customerRepository.delete(customerRepository.findByFirstName(e).orElseThrow(() -> new NoSuchElementException("Resource Not Found")));
                    log.info("Customer deleted from Database: "+e);
                });
            }
        };
    }
     **/
    
}
