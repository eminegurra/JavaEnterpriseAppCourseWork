package com.eazybytes.eazyschool.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
public class Courses extends BaseEntity{

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO,generator="native")
    @GenericGenerator(name = "native",strategy = "native")
    private int courseId;

    private String name;

    private String fees;

    @Column(length = 600)
    private String description;

    private String imageUrl;
    
    @ManyToMany(mappedBy = "courses", fetch = FetchType.EAGER,cascade = CascadeType.PERSIST)
    private Set<Person> persons = new HashSet<>();

    @OneToMany(mappedBy = "courses", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CourseDocuments> documents;
    
    public void setDocuments(Set<CourseDocuments> documents) {
        this.documents = documents;
    }
}
