# Job Portal Application

## Overview

A full-stack Job Portal web application built using Java and Spring Boot that connects job seekers and recruiters on a single platform. The application allows recruiters to post jobs and manage hiring activities, while job seekers can search, save, and apply for jobs.

This project demonstrates backend engineering concepts such as authentication, role-based authorization, database management, MVC architecture, and service-oriented design using Spring Boot.

---

# Features

## Authentication & Authorization

* Secure login and registration system
* Role-based access for:

  * Job Seekers
  * Recruiters
* Spring Security integration

## Job Seeker Features

* Search jobs using global search
* View job details
* Save jobs for later
* Apply to jobs
* Manage profile information
* Track job activity

## Recruiter Features

* Post new jobs
* Manage posted jobs
* View applicants
* Update recruiter profile

## Search & Filtering

* Global search functionality
* Search jobs by:

  * Keywords
  * Title
  * Skills
  * Location

## Backend Functionality

* RESTful architecture principles
* Layered architecture:

  * Controller Layer
  * Service Layer
  * Repository Layer
* Database integration with JPA/Hibernate
* Entity relationships and persistence handling

---

# Tech Stack

## Backend

* Java
* Spring Boot
* Spring MVC
* Spring Security
* Spring Data JPA
* Hibernate

## Frontend

* HTML
* CSS
* Thymeleaf
* Bootstrap

## Database

* MySQL

## Tools & Technologies

* Maven
* Git
* GitHub
* IntelliJ IDEA / VS Code

---

# Project Architecture

```text
Client/UI
   ↓
Controller Layer
   ↓
Service Layer
   ↓
Repository Layer
   ↓
MySQL Database
```

---

# Folder Structure

```text
src
 ┣ main
 ┃ ┣ java
 ┃ ┃ ┗ com.luv2code.jobportal
 ┃ ┃    ┣ controller
 ┃ ┃    ┣ services
 ┃ ┃    ┣ repository
 ┃ ┃    ┣ entity
 ┃ ┃    ┣ config
 ┃ ┃    ┗ util
 ┃ ┗ resources
 ┃    ┣ templates
 ┃    ┣ static
 ┃    ┗ application.properties
```

---

# Key Learning Outcomes

This project helped in gaining hands-on experience with:

* Spring Boot application development
* Authentication and authorization
* Database design and ORM
* MVC architecture
* CRUD operations
* Repository and service patterns
* Git and GitHub workflow
* Backend project structuring
* Real-world job portal workflow implementation

---

# Installation & Setup

## Prerequisites

* Java 17+
* Maven
* MySQL
* Git

## Clone Repository

```bash
git clone https://github.com/dexesh/JobPortal.git
```

## Navigate to Project

```bash
cd JobPortal
```

## Configure Database

Update `application.properties`:

```properties
spring.datasource.url=YOUR_DATABASE_URL
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

## Run Application

```bash
mvn spring-boot:run
```

---

# Job Recommendation System

One of the key features of this project is an AI-powered Job Recommendation System designed to provide personalized and relevant job recommendations to users.

## How The Recommendation System Works

### Step 1: User Profile & Resume Processing

When a job seeker creates and saves their profile:

* The user's resume is parsed and processed.
* Unnecessary information such as:

  * Education details
  * Hobbies
  * Irrelevant noise
    is filtered out.
* A clean professional summary is generated from the resume.

This processed summary helps improve recommendation quality by focusing only on important technical and professional skills.

---

### Step 2: Embedding Generation

After generating the cleaned summary:

* Text embeddings are created using Ollama embedding models.
* These embeddings represent the semantic meaning of the candidate profile.
* The embeddings are then stored inside Pinecone Vector Database.

---

### Step 3: Recruiter Job Posting Processing

When recruiters post new jobs:

* The job description is processed.
* Embeddings are generated for the job description using Ollama.
* The generated job embeddings are stored in Pinecone.

---

### Step 4: Semantic Matching & Recommendation

The system performs semantic similarity matching between:

* Candidate profile embeddings
* Job description embeddings

Using vector similarity search in Pinecone, the system retrieves the most relevant job opportunities for the user.

This enables intelligent recommendations based on meaning and skills rather than simple keyword matching.

---

## AI/ML Workflow

```text
Resume Upload
      ↓
Resume Parsing
      ↓
Noise Removal & Summary Generation
      ↓
Embedding Generation using Ollama
      ↓
Store Embeddings in Pinecone
      ↓
Recruiter Posts Job
      ↓
Generate Job Description Embeddings
      ↓
Store Job Embeddings in Pinecone
      ↓
Semantic Similarity Search
      ↓
Recommended Jobs Returned To User
```

---

## Technologies Used In Recommendation System

### AI & Embeddings

* Ollama
* Embedding Models
* LLM-based text processing

### Vector Database

* Pinecone Vector Database

### Backend

* Java
* Spring Boot
* Spring Data JPA

---

## Concepts Demonstrated

This feature demonstrates understanding of:

* AI-powered recommendation systems
* Retrieval-based architectures
* Vector embeddings
* Semantic search
* Resume parsing
* Vector databases
* LLM integration
* Backend + AI integration
* Real-world intelligent system design

---

# Future Improvements

* Resume upload feature
* Email notifications
* JWT authentication
* Docker support
* CI/CD pipeline integration
* AI-based job recommendation system
* Real-time chat between recruiter and candidate
* Cloud deployment

---

# Screenshots

Add application screenshots here:

* Login Page
* Dashboard
* Job Listings
* Recruiter Panel
* Search Functionality

---

# GitHub Repository

Repository Link:

[https://github.com/dexesh/JobPortal](https://github.com/dexesh/JobPortal)

---

# About Me

I am a backend-focused software developer passionate about Java, Spring Boot, backend systems, and scalable application development.

This project was built to strengthen my understanding of enterprise backend development and real-world application architecture.

---

# Contact

GitHub: [https://github.com/dexesh](https://github.com/dexesh)

LinkedIn: Add your LinkedIn profile here

---

# License

This project is for educational and learning purposes.
