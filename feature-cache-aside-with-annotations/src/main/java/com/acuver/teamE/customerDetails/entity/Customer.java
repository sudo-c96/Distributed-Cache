package com.acuver.teamE.customerDetails.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "CustomerDetails")
public class Customer {
    @Id
    private String id;

    private String firstName;

    private String lastName;

    private String gender;

    private Integer age;

    private String contactNo;

    private String emailId;

}
