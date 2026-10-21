package com.cafe.management.service.impl;

import com.cafe.management.dto.CreateUserRequest;
import com.cafe.management.dto.UserDto;
import com.cafe.management.entity.User;
import com.cafe.management.entity.UserStatus;
import com.cafe.management.exception.BadRequestException;
import com.cafe.management.exception.DuplicateResourceException;
import com.cafe.management.exception.ResourceNotFoundException;
import com.cafe.management.repository.UserRepository;
import com.cafe.management.service.UserService;
import com.cafe.management.util.DtoMapper;
import com.cafe.management.util.StaffCodes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(String search) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return DtoMapper.toUserDtos(userRepository.search(normalized));
    }

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        // The badge number can only be derived once the row id exists, so it is
        // stamped straight after the insert and flushed with the same transaction.
        User saved = userRepository.save(user);
        saved.setStaffCode(StaffCodes.forUserId(saved.getId()));

        return DtoMapper.toUserDto(saved);
    }

    @Override
    @Transactional
    public UserDto updateStatus(Long id, UserStatus status) {
        User user = findUser(id);

        if (user.getRole() == com.cafe.management.entity.Role.ADMIN && status == UserStatus.INACTIVE) {
            long activeAdmins = userRepository.findByRoleAndStatus(com.cafe.management.entity.Role.ADMIN, UserStatus.ACTIVE)
                    .stream()
                    .filter(admin -> !admin.getId().equals(id))
                    .count();
            if (activeAdmins == 0) {
                throw new BadRequestException("At least one active ADMIN account must remain.");
            }
        }

        user.setStatus(status);
        return DtoMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return DtoMapper.toUserDto(findUser(id));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
