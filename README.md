# SOEN345 Ticket Reservation Application  

A cloud-based ticket booking application for events including movies, concerts, travel, and sports.  

## Project Information  

- **Course:** SOEN 345 - Software Testing, Verification and Quality Assurance.  
- **Semester:** Winter 2026.  
- **Team Members:**
  - Yasmeen Adi - 40223656.  
  - Rim ChCharafeddine - 40282994.  
  - Khujista faqiri - 40249541.  
  - Nayla Nocera - 40283927.
  - Nicole Antoun -  40284018
    
## Project Objectives  
Develop a ticket booking system that allows  
- Users to browse and reserve tickets for various events  
- Event organizers to manage events  
- Digital confirmations via email/SMS  
- Cloud-based deployment with high availability
  
## Technology Stack  

- **Language:** Java  
- **IDE:** Android Studio  
- **Database:** MySQL Supabase
- **Version Control:** GitHub  
- **CI/CD:** GitHub Actions  
- **Testing:** JUnit 5

  ## Getting Started  

### Prerequisites  

- Java JDK 11 or higher  
- Android Studio (latest version)  
- Git  
- MySQL (local) or Supabase account

### Installation

1. Clone the repository:
```bash
   git clone https://github.com/khujista-01/SOEN345.git
   cd SOEN345-TicketReservation  
```
3. Configure your database connection in `app/src/main/resources/application.properties`  

4. Run the application 


##  Development Workflow

We follow a feature-branch workflow:

1. Create a new branch from `develop` for each feature/sprint
2. Name branches descriptively: `feature/user-authentication`, `sprint/sprint-1`
3. Make your changes and commit regularly
4. Create a Pull Request when ready for review
5. After approval, merge to `develop`
6. `main` branch is for production-ready releases only  

## Testing

Run tests using:
```bash
./gradlew test
```

## Sprint Schedule

- **Sprint 1:** Setup & Basic Features (Current)
- **Sprint 2:** Core Functionality
- **Sprint 3:** Integration & Testing
- **Sprint 4:** Polish & Deployment

## License

This project is for educational purposes as part of SOEN345 at Concordia University.
