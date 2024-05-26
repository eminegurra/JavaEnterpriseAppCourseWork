package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.*;
import com.eazybytes.eazyschool.repository.CoursesRepository;
import com.eazybytes.eazyschool.repository.EazyClassRepository;
import com.eazybytes.eazyschool.repository.PersonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import com.eazybytes.eazyschool.service.PersonService;
import com.eazybytes.eazyschool.repository.RolesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("admin")
public class AdminController {

    @Autowired
    EazyClassRepository eazyClassRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    CoursesRepository coursesRepository;
    
    @Autowired
    PersonService personService;

    @Autowired
    private RolesRepository rolesRepository;

    private static final String UPLOAD_DIR = "C:\\Users\\User\\Desktop\\JavaEnterpriseAppCourseWork\\src\\main\\resources\\static\\assets\\images\\";
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);


    @RequestMapping("/displayClasses")
    public ModelAndView displayClasses(Model model) {
        List<EazyClass> eazyClasses = eazyClassRepository.findAll();
        ModelAndView modelAndView = new ModelAndView("classes.html");
        modelAndView.addObject("eazyClasses",eazyClasses);
        modelAndView.addObject("eazyClass", new EazyClass());
        return modelAndView;
    }

    @PostMapping("/addNewClass")
    public ModelAndView addNewClass(Model model, @ModelAttribute("eazyClass") EazyClass eazyClass) {
        eazyClassRepository.save(eazyClass);
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/displayClasses");
        return modelAndView;
    }

    @RequestMapping("/deleteClass")
    public ModelAndView deleteClass(Model model, @RequestParam int id) {
        Optional<EazyClass> eazyClass = eazyClassRepository.findById(id);
        for(Person person : eazyClass.get().getPersons()){
            person.setEazyClass(null);
            personRepository.save(person);
        }
        eazyClassRepository.deleteById(id);
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/displayClasses");
        return modelAndView;
    }

    @GetMapping("/displayStudents")
    public ModelAndView displayStudents(Model model, @RequestParam int classId, HttpSession session,
                                        @RequestParam(value = "error", required = false) String error) {
        String errorMessage = null;
        ModelAndView modelAndView = new ModelAndView("students.html");
        Optional<EazyClass> eazyClass = eazyClassRepository.findById(classId);
        modelAndView.addObject("eazyClass",eazyClass.get());
        modelAndView.addObject("person",new Person());
        session.setAttribute("eazyClass",eazyClass.get());
        if(error != null) {
            errorMessage = "Invalid Email entered!!";
            modelAndView.addObject("errorMessage", errorMessage);
        }
        return modelAndView;
    }

    @PostMapping("/addStudent")
    public ModelAndView addStudent(Model model, @ModelAttribute("person") Person person, HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        EazyClass eazyClass = (EazyClass) session.getAttribute("eazyClass");
        Person personEntity = personRepository.readByEmail(person.getEmail());
        if(personEntity==null || !(personEntity.getPersonId()>0)){
            modelAndView.setViewName("redirect:/admin/displayStudents?classId="+eazyClass.getClassId()
                    +"&error=true");
            return modelAndView;
        }
        personEntity.setEazyClass(eazyClass);
        personRepository.save(personEntity);
        eazyClass.getPersons().add(personEntity);
        eazyClassRepository.save(eazyClass);
        modelAndView.setViewName("redirect:/admin/displayStudents?classId="+eazyClass.getClassId());
        return modelAndView;
    }

    @GetMapping("/deleteStudent")
    public ModelAndView deleteStudent(Model model, @RequestParam int personId, HttpSession session) {
        EazyClass eazyClass = (EazyClass) session.getAttribute("eazyClass");
        Optional<Person> person = personRepository.findById(personId);
        person.get().setEazyClass(null);
        eazyClass.getPersons().remove(person.get());
        EazyClass eazyClassSaved = eazyClassRepository.save(eazyClass);
        session.setAttribute("eazyClass",eazyClassSaved);
        ModelAndView modelAndView = new ModelAndView("redirect:/admin/displayStudents?classId="+eazyClass.getClassId());
        return modelAndView;
    }

 @GetMapping("/displayCourses")
    public ModelAndView displayCourses(Model model) {
        //List<Courses> courses = coursesRepository.findByOrderByNameDesc();
        List<Courses> courses = coursesRepository.findAll(Sort.by("name").descending());
        ModelAndView modelAndView = new ModelAndView("courses_secure.html");
        modelAndView.addObject("courses",courses);
        modelAndView.addObject("course", new Courses());
        return modelAndView;
    }

    @PostMapping("/addNewCourse")
    public ModelAndView addNewCourse(Model model, @ModelAttribute("course") Courses course,
                                     @RequestParam("image") MultipartFile file, HttpSession session) {
        Person person = (Person) session.getAttribute("loggedInPerson");

        ModelAndView modelAndView = new ModelAndView();
        if (!file.isEmpty()) {
            try {
                // Log file details
                System.out.println("File name: " + file.getOriginalFilename());
                System.out.println("File size: " + file.getSize());

                // Get the filename
                String filename = StringUtils.cleanPath(file.getOriginalFilename());

                // Copy file to the target location
                Path path = Paths.get(UPLOAD_DIR + filename);
                Files.copy(file.getInputStream(), path);

                // Set the profile picture URL in the Courses object
                course.setImageUrl("/assets/images/" + filename); // Relative URL

                // Log imageUrl value
                System.out.println("Image URL: " + course.getImageUrl());
            } catch (IOException e) {
                e.printStackTrace(); // Handle error
            }
        }
        Courses  savedCourse = coursesRepository.save(course);
        session.setAttribute("loggedInPerson", savedCourse);

        modelAndView.setViewName("redirect:/admin/displayCourses");
        return modelAndView;
    }

    @GetMapping("/viewStudents")
    public ModelAndView viewStudents(Model model, @RequestParam int id
                 ,HttpSession session,@RequestParam(required = false) String error) {
        String errorMessage = null;
        ModelAndView modelAndView = new ModelAndView("course_students.html");
        Optional<Courses> courses = coursesRepository.findById(id);
        modelAndView.addObject("courses",courses.get());
        modelAndView.addObject("person",new Person());
        session.setAttribute("courses",courses.get());
        if(error != null) {
            errorMessage = "Invalid Email entered!!";
            modelAndView.addObject("errorMessage", errorMessage);
        }
        return modelAndView;
    }

    @PostMapping("/addStudentToCourse")
    public ModelAndView addStudentToCourse(Model model, @ModelAttribute("person") Person person,
                                           HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        Courses courses = (Courses) session.getAttribute("courses");
        Person personEntity = personRepository.readByEmail(person.getEmail());
        if(personEntity==null || !(personEntity.getPersonId()>0)){
            modelAndView.setViewName("redirect:/admin/viewStudents?id="+courses.getCourseId()
                    +"&error=true");
            return modelAndView;
        }
        personEntity.getCourses().add(courses);
        courses.getPersons().add(personEntity);
        personRepository.save(personEntity);
        session.setAttribute("courses",courses);
        modelAndView.setViewName("redirect:/admin/viewStudents?id="+courses.getCourseId());
        return modelAndView;
    }

    @GetMapping("/deleteStudentFromCourse")
    public ModelAndView deleteStudentFromCourse(Model model, @RequestParam int personId,
                                                HttpSession session) {
        Courses courses = (Courses) session.getAttribute("courses");
        Optional<Person> person = personRepository.findById(personId);
        person.get().getCourses().remove(courses);
        courses.getPersons().remove(person);
        personRepository.save(person.get());
        session.setAttribute("courses",courses);
        ModelAndView modelAndView = new
                ModelAndView("redirect:/admin/viewStudents?id="+courses.getCourseId());
        return modelAndView;
    }

    @RequestMapping(value = "/createLecturer", method = RequestMethod.POST)
    public String createLecturer(@Valid @ModelAttribute("person") Person person, BindingResult result) {
    logger.info("Entered createLecturer method");

    if (result.hasErrors()) {
        logger.error("Form validation errors: {}", result.getAllErrors());
        return "lecturers"; // Return to the same page to display validation errors
    }

    logger.info("Attempting to create a new lecturer: {}", person);

    boolean isSaved = personService.createNewLecturer(person);

    if (isSaved) {
        logger.info("Lecturer created successfully");
        return "redirect:/admin/displayLecturers"; // Redirect to display the updated list of lecturers
    } else {
        logger.error("Failed to create lecturer");
        return "lecturers"; // Return to the same page with an error message
    }
}
  
    @GetMapping("/displayLecturers")
    public ModelAndView displayLecturers() {
        ModelAndView modelAndView = new ModelAndView("lecturers");
        Roles lecturerRole = rolesRepository.getByRoleName("LECTURER");

        // Fetch the list of persons with the role of "LECTURER"
        // List<Person> lecturers = personRepository.findByRoles(lecturerRole);
        List<Person> lecturers = personRepository.findByRolesRoleName("LECTURER");

        modelAndView.addObject("lecturers", lecturers);
        modelAndView.addObject("person", new Person()); // Add this line
        return modelAndView;
    }
    @GetMapping("/assignCourse")
    public String showAssignCourseForm(Model model) {
        List<Person> lecturers = personRepository.findByRolesRoleName("LECTURER");
        List<Courses> courses = coursesRepository.findAll();
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("courses", courses);
        return "assign_course.html";
    }

    @PostMapping("/assignCourse")
    public String assignCourseToLecturer(@RequestParam("lecturerId") int lecturerId, @RequestParam("courseId") int courseId) {
        Optional<Person> lecturerOpt = personRepository.findById(lecturerId);
        Optional<Courses> courseOpt = coursesRepository.findById(courseId);

        if (lecturerOpt.isPresent() && courseOpt.isPresent()) {
            Person lecturer = lecturerOpt.get();
            Courses course = courseOpt.get();
            lecturer.getCourses().add(course);
            course.getPersons().add(lecturer);
            personRepository.save(lecturer);
            coursesRepository.save(course);
        }
        return "redirect:/admin/displayLecturers";
    }
}
