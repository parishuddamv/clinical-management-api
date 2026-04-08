# Contributing to ClinicOS

Thank you for your interest in contributing to ClinicOS! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Report issues responsibly
- Help others succeed

## Getting Started

### 1. Fork the Repository

```bash
# Fork on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/clinical-management-system.git
cd clinical-management-system
```

### 2. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
# or for bug fixes
git checkout -b fix/bug-name
```

### 3. Set Up Development Environment

```bash
# Follow SETUP.md for complete setup
bash quickstart.sh  # macOS/Linux
quickstart.bat      # Windows
```

## Development Workflow

### Writing Code

1. **Follow Code Style**
   - Use Google Java Style Guide
   - Format code: `mvn fmt:format`
   - Use meaningful variable names
   - Add Javadoc for public methods

2. **Structure**
   ```
   module/
   ├── src/main/java/com/clinicos/[module]/
   │   ├── entity/        # JPA entities
   │   ├── dto/          # Data transfer objects
   │   ├── repository/   # Spring Data repositories
   │   ├── service/      # Business logic
   │   ├── controller/   # REST endpoints
   │   ├── exception/    # Custom exceptions
   │   └── config/       # Configuration classes
   ├── src/test/java/    # Unit & integration tests
   └── pom.xml           # Module dependencies
   ```

3. **Commit Messages**
   ```
   feat: add patient search functionality
   fix: resolve database connection timeout
   docs: update API documentation
   test: add unit tests for PatientService
   refactor: simplify patient validation logic
   ```

### Testing

```bash
# Run all tests
mvn test

# Run specific module tests
cd clinic-patient
mvn test

# Run specific test class
mvn test -Dtest=PatientServiceTest

# Run specific test method
mvn test -Dtest=PatientServiceTest#testRegisterPatientSuccess

# Check coverage
mvn jacoco:report
```

### Code Quality

```bash
# Format code
mvn fmt:format

# Check formatting
mvn fmt:check

# Static analysis (if configured)
mvn spotbugs:check
```

## Submitting Changes

### 1. Commit Your Changes

```bash
git add .
git commit -m "feat: add patient search by tags"
```

### 2. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 3. Create Pull Request

- Go to GitHub and create a Pull Request
- Use descriptive title and description
- Link related issues: "Fixes #123"
- Ensure CI/CD pipeline passes

## Pull Request Guidelines

### Title Format
```
[MODULE] Type: Description

Examples:
[clinic-patient] feat: add phone number validation
[clinic-common] fix: resolve JWT token expiration issue
```

### Description Template
```markdown
## Description
Brief description of changes

## Related Issues
Fixes #123

## Changes Made
- Change 1
- Change 2
- Change 3

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Breaking Changes
None / List any breaking changes

## Screenshots/Videos (if applicable)
Add images for UI changes
```

### Review Process

1. At least 2 approvals required
2. All CI/CD checks must pass
3. No conflicts with main branch
4. Code coverage maintained/improved

## Reporting Issues

### Bug Reports

```markdown
**Description:**
Clear description of the bug

**Steps to Reproduce:**
1. Step 1
2. Step 2
3. Step 3

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Environment:**
- OS: Windows/macOS/Linux
- Java Version: 21.x
- Maven Version: 3.9.x
```

### Feature Requests

```markdown
**Title:** Clear title of feature

**Description:**
Why this feature is needed and what problem it solves

**Suggested Implementation:**
How you think it should work

**Example Usage:**
Show example of how it would be used
```

## Module-Specific Guidelines

### clinic-common
- Add reusable components only
- Update common security configs
- Add shared DTOs and entities
- Update base exception hierarchy

### clinic-patient
- Patient entity modifications
- Search/filter improvements
- Validation enhancements
- Performance optimizations

### clinic-appointment
- Appointment scheduling logic
- Conflict detection
- Reminder generation
- Calendar integrations

### clinic-followup
- Follow-up task management
- Clinical notes handling
- Status tracking
- Notification triggers

### clinic-notification
- New notification channels (Twilio, AWS SES)
- Template management
- Delivery tracking
- Retry logic

### clinic-billing
- Invoice generation
- Payment processing
- Tax calculation
- Report generation

### clinic-gateway
- Route configurations
- Filter enhancements
- Rate limiting
- Authentication flows

## Documentation

### Code Documentation
```java
/**
 * Register a new patient for a clinic
 *
 * @param clinicId the clinic identifier
 * @param request the registration request with patient details
 * @return the registered patient response
 * @throws DuplicatePatientException if patient with same phone exists
 */
public PatientResponse registerPatient(String clinicId, RegisterPatientRequest request) {
    // implementation
}
```

### Updating README

If adding new features, update:
1. Feature list
2. API endpoints table
3. Architecture diagram
4. Configuration options
5. Examples

## Performance Considerations

- Use pagination for large datasets
- Add appropriate database indexes
- Implement caching where needed
- Monitor query performance
- Use connection pooling

## Security Checklist

- [ ] No hardcoded secrets
- [ ] Validate all inputs
- [ ] Sanitize SQL queries (use parameterized queries)
- [ ] Check authorization on protected endpoints
- [ ] Use HTTPS in production
- [ ] Update dependencies regularly

## Release Process

1. **Version Bumping**
   ```bash
   mvn versions:set -DnewVersion=1.1.0
   ```

2. **Release Notes**
   - Summarize changes
   - List breaking changes
   - Note migration steps

3. **Tag Release**
   ```bash
   git tag -a v1.1.0 -m "Release version 1.1.0"
   git push origin v1.1.0
   ```

## Communication

- **Discussions**: GitHub Discussions
- **Issues**: GitHub Issues
- **Email**: support@clinicos.com
- **Slack**: Available for active contributors

## License

By contributing, you agree that your contributions will be licensed under the project's license.

---

Thank you for contributing to ClinicOS! 🎉

