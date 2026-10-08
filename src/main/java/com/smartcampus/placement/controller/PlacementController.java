package com.smartcampus.placement.controller;
import com.smartcampus.placement.entity.*;
import com.smartcampus.placement.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.nio.file.*; import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api")
@CrossOrigin(origins={"http://localhost:5173","http://localhost:5174"})
public class PlacementController {
 private final StudentRepository students; private final CompanyRepository companies; private final JobRepository jobs; private final ApplicationRepository applications;
 public PlacementController(StudentRepository s,CompanyRepository c,JobRepository j,ApplicationRepository a){students=s;companies=c;jobs=j;applications=a;}

 @GetMapping("/students") public List<Student> students(){return students.findAll();}
 @PostMapping("/students") public Student addStudent(@RequestBody Student s){return students.save(s);}
 @PutMapping("/students/{id}") public ResponseEntity<Student> updateStudent(@PathVariable Long id,@RequestBody Student s){return students.findById(id).map(x->{x.setName(s.getName());x.setEmail(s.getEmail());x.setBranch(s.getBranch());x.setCgpa(s.getCgpa());x.setSkills(s.getSkills());return ResponseEntity.ok(students.save(x));}).orElse(ResponseEntity.notFound().build());}
 @PostMapping("/students/{id}/resume") public ResponseEntity<?> resume(@PathVariable Long id,@RequestParam("file") MultipartFile file) throws IOException {
   return students.findById(id).map(s->{try{Path dir=Paths.get("uploads");Files.createDirectories(dir);String name=UUID.randomUUID()+"_"+file.getOriginalFilename();Files.copy(file.getInputStream(),dir.resolve(name),StandardCopyOption.REPLACE_EXISTING);s.setResumeFile(name);students.save(s);return ResponseEntity.ok(mapOf("message","Resume uploaded","file",name));}catch(IOException e){return ResponseEntity.internalServerError().body(mapOf("error","Upload failed"));}}).orElse(ResponseEntity.notFound().build());
 }
 @GetMapping("/companies") public List<Company> companies(){return companies.findAll();}
 @PostMapping("/companies") public Company addCompany(@RequestBody Company c){return companies.save(c);}
 @GetMapping("/jobs") public List<Map<String,Object>> jobList(){
   return jobs.findAll().stream().map(j->mapOf("id",j.getId(),"title",j.getTitle(),"branch",j.getBranch(),"requiredSkills",j.getRequiredSkills(),"minCgpa",j.getMinCgpa(),"openings",j.getOpenings(),"company",j.getCompany().getName(),"companyId",j.getCompany().getId())).collect(Collectors.toList());
 }
 @PostMapping("/jobs") public ResponseEntity<?> addJob(@RequestBody Map<String,Object> p){
   Company c=companies.findById(Long.valueOf(p.get("companyId").toString())).orElse(null); if(c==null)return ResponseEntity.badRequest().body(mapOf("error","Company not found"));
   Job j=new Job(p.get("title").toString(),p.get("branch").toString(),p.get("requiredSkills").toString(),Double.valueOf(p.get("minCgpa").toString()),Integer.valueOf(p.get("openings").toString()),c); return ResponseEntity.ok(jobs.save(j));
 }
 @GetMapping("/jobs/eligible/{studentId}") public ResponseEntity<?> eligible(@PathVariable Long studentId, Authentication authentication){
   if(isStudent(authentication)&&!isOwnStudent(studentId,authentication))return ResponseEntity.status(403).body(mapOf("error","You can only view your own eligibility."));
   Student s=students.findById(studentId).orElse(null); if(s==null)return ResponseEntity.notFound().build();
   return ResponseEntity.ok(jobs.findAll().stream().filter(j->eligibleFor(s,j)).map(j->mapOf("id",j.getId(),"title",j.getTitle(),"company",j.getCompany().getName(),"branch",j.getBranch(),"requiredSkills",j.getRequiredSkills(),"minCgpa",j.getMinCgpa(),"openings",j.getOpenings())).toList());
 }
 private boolean eligibleFor(Student s,Job j){boolean branch=s.getBranch()!=null&&(j.getBranch().equalsIgnoreCase("Any")||j.getBranch().equalsIgnoreCase(s.getBranch())); boolean cgpa=s.getCgpa()!=null&&s.getCgpa()>=j.getMinCgpa(); String ss=(s.getSkills()==null?"":s.getSkills()).toLowerCase(); boolean skill=Arrays.stream(j.getRequiredSkills().split(",")).map(String::trim).filter(x->!x.isBlank()).allMatch(ss::contains); return branch&&cgpa&&skill;}
 private static Map<String,Object> mapOf(Object... entries){Map<String,Object> m=new LinkedHashMap<>(); for(int i=0;i<entries.length;i+=2){m.put(String.valueOf(entries[i]),entries[i+1]);} return m;}
 @PostMapping("/applications") public ResponseEntity<?> apply(@RequestBody Map<String,Object> p, Authentication authentication){
   Long requestedStudentId=Long.valueOf(p.get("studentId").toString());
   if(isStudent(authentication)&&!isOwnStudent(requestedStudentId,authentication))return ResponseEntity.status(403).body(mapOf("error","You can only apply for yourself."));
   Long sid=requestedStudentId, jid=Long.valueOf(p.get("jobId").toString()); if(applications.existsByStudentIdAndJobId(sid,jid))return ResponseEntity.badRequest().body(mapOf("error","Already applied"));
   Student s=students.findById(sid).orElse(null); Job j=jobs.findById(jid).orElse(null); if(s==null||j==null||!eligibleFor(s,j))return ResponseEntity.badRequest().body(mapOf("error","Student is not eligible"));
   return ResponseEntity.ok(applications.save(new Application(s,j,"Applied")));
 }
 @GetMapping("/applications") public List<Application> allApplications(){return applications.findAll();}
 @GetMapping("/applications/student/{id}") public ResponseEntity<?> studentApplications(@PathVariable Long id, Authentication authentication){
   if(isStudent(authentication)&&!isOwnStudent(id,authentication))return ResponseEntity.status(403).body(mapOf("error","You can only view your own applications."));
   return ResponseEntity.ok(applications.findByStudentId(id));
 }
 @PatchMapping("/applications/{id}") public ResponseEntity<Application> status(@PathVariable Long id,@RequestParam String value){return applications.findById(id).map(a->{a.setStatus(value);return ResponseEntity.ok(applications.save(a));}).orElse(ResponseEntity.notFound().build());}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){return mapOf("students",students.count(),"companies",companies.count(),"jobs",jobs.count(),"applications",applications.count());}
 private boolean isStudent(Authentication authentication){return authentication.getAuthorities().stream().anyMatch(authority->authority.getAuthority().equals("ROLE_STUDENT"));}
 private boolean isOwnStudent(Long studentId,Authentication authentication){return students.findByEmailIgnoreCase(authentication.getName()).map(student->student.getId().equals(studentId)).orElse(false);}
}
