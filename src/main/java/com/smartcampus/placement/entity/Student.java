package com.smartcampus.placement.entity;
import jakarta.persistence.*;
@Entity @Table(name="students")
public class Student {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 private String name, email, branch; private Double cgpa; private String skills; private String resumeFile;
 public Student() {}
 public Student(String name,String email,String branch,Double cgpa,String skills){this.name=name;this.email=email;this.branch=branch;this.cgpa=cgpa;this.skills=skills;}
 public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;}
 public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getBranch(){return branch;} public void setBranch(String v){branch=v;}
 public Double getCgpa(){return cgpa;} public void setCgpa(Double v){cgpa=v;} public String getSkills(){return skills;} public void setSkills(String v){skills=v;}
 public String getResumeFile(){return resumeFile;} public void setResumeFile(String v){resumeFile=v;}
}