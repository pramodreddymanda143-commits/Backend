package com.smartcampus.placement.entity;
import jakarta.persistence.*;
@Entity @Table(name="companies")
public class Company {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String name,email,industry;
 public Company() {} public Company(String name,String email,String industry){this.name=name;this.email=email;this.industry=industry;}
 public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getIndustry(){return industry;} public void setIndustry(String v){industry=v;}
}