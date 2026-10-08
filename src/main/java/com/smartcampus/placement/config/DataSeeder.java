package com.smartcampus.placement.config;
import com.smartcampus.placement.entity.*;
import com.smartcampus.placement.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {
  @Bean
  CommandLineRunner seed(
      StudentRepository sr,
      CompanyRepository cr,
      JobRepository jr,
      ApplicationRepository ar) {
    return args -> {
      if (sr.count() == 0) {
        sr.save(new Student("Ananya Sharma", "ananya@example.com", "CSE", 8.7, "Java, React, SQL, Spring Boot"));
        sr.save(new Student("Rahul Kumar", "rahul@example.com", "ECE", 7.9, "Python, SQL, Linux, Networking"));
        sr.save(new Student("Priya Nair", "priya@example.com", "IT", 8.9, "Java, Spring Boot, React, AWS"));
        sr.save(new Student("Karthik Iyer", "karthik@example.com", "CSE", 8.2, "C++, Java, DSA, SQL"));
        sr.save(new Student("Meera Singh", "meera@example.com", "EEE", 7.4, "Python, Embedded, MATLAB, PLC"));
      }

      if (cr.count() == 0) {
        Company google = cr.save(new Company("Google", "careers@google.example", "Technology"));
        Company microsoft = cr.save(new Company("Microsoft", "campus@microsoftexample.com", "Technology"));
        Company amazon = cr.save(new Company("Amazon", "recruiting@amazon.example", "E-commerce & Cloud"));
        Company infosys = cr.save(new Company("Infosys", "campus@infosys.example", "IT Services"));
        Company tcs = cr.save(new Company("TCS", "careers@tcs.example", "Consulting & IT"));
        Company accenture = cr.save(new Company("Accenture", "placement@accenture.example", "Consulting"));
        Company capgemini = cr.save(new Company("Capgemini", "campus@capgemini.example", "Technology Services"));
        Company oracle = cr.save(new Company("Oracle", "jobs@oracle.example", "Database & Cloud"));

        jr.save(new Job("Software Engineer", "CSE", "Java, Spring Boot, DSA", 8.5, 6, google));
        jr.save(new Job("Data Center Operations Engineer", "ECE", "Linux, Networking, Python", 7.6, 4, google));
        jr.save(new Job("Frontend Engineer", "CSE", "React, JavaScript, HTML, CSS", 8.0, 5, microsoft));
        jr.save(new Job("Cloud Engineer", "IT", "AWS, Docker, Linux, Kubernetes", 7.8, 5, microsoft));
        jr.save(new Job("SDE Intern", "CSE", "Java, Python, DSA", 8.2, 7, amazon));
        jr.save(new Job("Business Analyst", "Any", "SQL, Excel, Power BI", 7.0, 3, amazon));
        jr.save(new Job("Full Stack Developer", "IT", "Java, React, SQL", 7.5, 6, infosys));
        jr.save(new Job("System Engineer", "Any", "Java, SQL, Communication", 7.0, 8, infosys));
        jr.save(new Job("Java Developer", "CSE", "Java, Spring Boot, SQL", 7.3, 4, tcs));
        jr.save(new Job("Data Analyst", "Any", "SQL, Python, Tableau", 7.1, 5, accenture));
        jr.save(new Job("Cloud Engineer", "IT", "AWS, Linux, Docker, CI/CD", 7.8, 4, capgemini));
        jr.save(new Job("Database Developer", "CSE", "SQL, PL/SQL, Database Design", 7.7, 3, oracle));
      }

      if (ar.count() == 0) {
        Student ananya = sr.findByEmail("ananya@example.com").orElse(null);
        Student priy = sr.findByEmail("priya@example.com").orElse(null);
        Student rahul = sr.findByEmail("rahul@example.com").orElse(null);
        Student karthik = sr.findByEmail("karthik@example.com").orElse(null);

        if (ananya != null && priy != null) {
          Job googleSde = jr.findAll().stream().filter(job -> job.getTitle().equals("Software Engineer") && job.getCompany().getName().equals("Google")).findFirst().orElse(null);
          Job infosysFsd = jr.findAll().stream().filter(job -> job.getTitle().equals("Full Stack Developer") && job.getCompany().getName().equals("Infosys")).findFirst().orElse(null);
          if (googleSde != null) {
            ar.save(new Application(ananya, googleSde, "Shortlisted"));
          }
          if (infosysFsd != null) {
            ar.save(new Application(priy, infosysFsd, "Applied"));
          }
        }

        if (rahul != null && karthik != null) {
          Job googleOps = jr.findAll().stream().filter(job -> job.getTitle().equals("Data Center Operations Engineer") && job.getCompany().getName().equals("Google")).findFirst().orElse(null);
          Job amazonSde = jr.findAll().stream().filter(job -> job.getTitle().equals("SDE Intern") && job.getCompany().getName().equals("Amazon")).findFirst().orElse(null);
          if (googleOps != null) {
            ar.save(new Application(rahul, googleOps, "Applied"));
          }
          if (amazonSde != null) {
            ar.save(new Application(karthik, amazonSde, "Selected"));
          }
        }
      }
    };
  }
}

