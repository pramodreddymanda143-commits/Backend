package com.smartcampus.placement.entity;
import jakarta.persistence.*;
@Entity @Table(name="applications", uniqueConstraints=@UniqueConstraint(columnNames={"student_id","job_id"}))
public class Application {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private Student student; @ManyToOne(optional=false) private Job job;
 private String status;
 public Application() {} public Application(Student s,Job j,String status){student=s;job=j;this.status=status;}
 public Long getId(){return id;} public Student getStudent(){return student;} public Job getJob(){return job;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}