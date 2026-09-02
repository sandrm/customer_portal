package com.example.customerportal.auth.service;

import com.example.customerportal.auth.dto.RegisteredUserResponse;
import com.example.customerportal.auth.dto.RegistrationRequest;
import com.example.customerportal.auth.entity.User;
import com.example.customerportal.auth.exception.DuplicateEmailException;
import com.example.customerportal.auth.exception.ValidationException;
import com.example.customerportal.auth.repository.UserRepository;
import com.example.customerportal.common.exception.InvalidParameter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,128}$"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisteredUserResponse register(RegistrationRequest request) {
        List<InvalidParameter> errors = new ArrayList<>();
        String rawEmail = request.email() == null ? "" : request.email().strip().toLowerCase();
        String rawPassword = request.password() == null ? "" : request.password();

        if (rawEmail.isEmpty()) {
            errors.add(new InvalidParameter("email", "must not be blank"));
        } else if (!EMAIL_PATTERN.matcher(rawEmail).matches()) {
            errors.add(new InvalidParameter("email", "must be a valid email address"));
        }

        if (rawPassword.isEmpty()) {
            errors.add(new InvalidParameter("password", "must not be blank"));
        } else if (rawPassword.length() < 8) {
            errors.add(new InvalidParameter("password", "must be at least 8 characters long"));
        } else if (rawPassword.length() > 128) {
            errors.add(new InvalidParameter("password", "must be at most 128 characters long"));
        } else if (!PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            errors.add(new InvalidParameter("password", "must contain at least one uppercase, one lowercase, one digit, and one special character"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }

        if (userRepository.existsByEmail(rawEmail)) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setEmail(rawEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        try {
            User saved = userRepository.save(user);
            return new RegisteredUserResponse(saved.getId(), saved.getEmail(), saved.getCreatedAt());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException();
        }
    }
}
