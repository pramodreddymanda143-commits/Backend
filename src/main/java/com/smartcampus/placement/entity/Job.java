package com.smartcampus.placement.entity;
import jakarta.persistence.*;
@Entity @Table(name="jobs")
public class Job {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String title,branch,requiredSkills; private Double minCgpa; private Integer openings;
 @ManyToOne(optional=false) private Company company;
 public Job() {}
 public Job(String title,String branch,String requiredSkills,Double minCgpa,Integer openings,Company company){this.title=title;this.branch=branch;this.requiredSkills=requiredSkills;this.minCgpa=minCgpa;this.openings=openings;this.company=company;}
 public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getBranch(){return branch;} public void setBranch(String v){branch=v;} public String getRequiredSkills(){return requiredSkills;} public void setRequiredSkills(String v){requiredSkills=v;}
 public Double getMinCgpa(){return minCgpa;} public void setMinCgpa(Double v){minCgpa=v;} public Integer getOpenings(){return openings;} public void setOpenings(Integer v){openings=v;}
 public Company getCompany(){return company;} public void setCompany(Company v){company=v;}
}