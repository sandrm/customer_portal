# customer_portal
Code for training "Backend Development with Agentic Engineering"

# Run application using local profile (for H2 database): 
mvn clean spring-boot:run "-Dspring-profiles.active=local"

# Check application running and check database H2 structure 
http://localhost:8080/h2-console/

# Call endpoint to registration new user:
POST
http://localhost:8080/api/v1/auth/register

{
    "email": "new.user@example.com",
    "password": "StrongP@ssw0rd"
}
