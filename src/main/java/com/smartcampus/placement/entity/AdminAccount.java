package com.smartcampus.placement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_accounts")
public class AdminAccount {
  @Id
  private Long id;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  protected AdminAccount() {}

  public AdminAccount(String email, String passwordHash) {
    this.id = 1L;
    this.email = email;
    this.passwordHash = passwordHash;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }
}
