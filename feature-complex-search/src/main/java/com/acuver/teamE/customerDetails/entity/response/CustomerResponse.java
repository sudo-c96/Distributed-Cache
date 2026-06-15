package com.acuver.teamE.customerDetails.entity.response;

import com.acuver.teamE.customerDetails.entity.Customer;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CustomerResponse {

    private List<Customer> content;

    private Integer pageNo;

    private Integer pageSize;

    private Long count;

    private Integer totalPages;

    private Boolean last;

}
