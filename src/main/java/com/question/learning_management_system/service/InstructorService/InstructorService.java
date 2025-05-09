package com.question.learning_management_system.service.InstructorService;

import com.question.learning_management_system.dto.InstructorDto;
import com.question.learning_management_system.entity.Course;
import com.question.learning_management_system.entity.Instructor;
import com.question.learning_management_system.enums.Role;
import com.question.learning_management_system.exception.AlreadyExistsException;
import com.question.learning_management_system.exception.InstructorNotFoundException;
import com.question.learning_management_system.repository.CourseRepository;
import com.question.learning_management_system.repository.InstructorRepository;
import com.question.learning_management_system.request.InstructorRequest.CreateInstructorRequest;
import com.question.learning_management_system.request.InstructorRequest.UpdateInstructorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class InstructorService implements IInstructorService {

    private final InstructorRepository instructorRepository;
    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public InstructorDto findById(Long id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new InstructorNotFoundException("Instructor with id: " + id + " not found!"));
        return convertToDto(instructor);
    }

    @Override
    public List<InstructorDto> getAllInstructors() {
        return instructorRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public InstructorDto addInstructor(CreateInstructorRequest request) {
        validateInstructorUniqueness(request.getEmail(), request.getPhoneNumber());

        Instructor newInstructor = buildInstructorFromRequest(request);
        newInstructor = instructorRepository.save(newInstructor);

        log.info("Instructor with ID {} created successfully", newInstructor.getId());
        return convertToDto(newInstructor);
    }

    @Override
    public InstructorDto updateInstructor(UpdateInstructorRequest request, Long id) {
        Instructor existingInstructor = instructorRepository.findById(id)
                .orElseThrow(() -> new InstructorNotFoundException("Instructor with id: " + id + " not found!"));

        if (!existingInstructor.getEmail().equalsIgnoreCase(request.getEmail()) && existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Email already exists for another instructor.");
        }

        updateInstructorInfo(existingInstructor, request);
        instructorRepository.save(existingInstructor);

        log.info("Instructor with ID {} updated successfully", existingInstructor.getId());
        return convertToDto(existingInstructor);
    }

    @Override
    public InstructorDto getInstructorByEmail(String email) {
        Instructor instructor = instructorRepository.findByEmail(email);
        if (instructor == null) {
            throw new InstructorNotFoundException("Instructor with email: " + email + " not found!");
        }
        return convertToDto(instructor);
    }

    @Override
    public void deleteInstructor(Long id) {
        Instructor instructor = instructorRepository.findById(id)
                .orElseThrow(() -> new InstructorNotFoundException("Instructor with id: " + id + " not found!"));
        instructorRepository.delete(instructor);

        log.info("Instructor with ID {} deleted successfully", id);
    }

    @Override
    public void deleteInstructorByEmail(String email) {
        Instructor instructor = instructorRepository.findByEmail(email);
        if (instructor == null) {
            throw new InstructorNotFoundException("Instructor with email: " + email + " not found!");
        }
        instructorRepository.delete(instructor);

        log.info("Instructor with email {} deleted successfully", email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return instructorRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phoneNumber) {
        return instructorRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsById(Long id) {
        return instructorRepository.existsById(id);
    }

    @Override
    public InstructorDto calculateInstructorRating(Long instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
                .orElseThrow(() -> new InstructorNotFoundException("Instructor with id: " + instructorId + " not found!"));

        List<Course> courses = courseRepository.findByInstructorId(instructorId);

        if (courses.isEmpty()) {
            instructor.setRating(0.0);
        } else {
            double averageRating = courses.stream()
                    .filter(course -> course.getAverageRate() != null)
                    .mapToDouble(Course::getAverageRate)
                    .average()
                    .orElse(0.0);

            instructor.setRating(averageRating);
        }

        instructorRepository.save(instructor);

        log.info("Instructor with ID {} rating calculated and updated successfully.", instructorId);
        return convertToDto(instructor);
    }

    // -------------------- Private Helper Methods --------------------

    private void validateInstructorUniqueness(String email, String phone) {
        if (existsByEmail(email)) {
            throw new AlreadyExistsException("Instructor with email: " + email + " already exists!");
        }
        if (existsByPhone(phone)) {
            throw new AlreadyExistsException("Instructor with phone number: " + phone + " already exists!");
        }
    }

    private Instructor buildInstructorFromRequest(CreateInstructorRequest request) {
        return Instructor.builder()
                .name(request.getName())
                .email(request.getEmail())
                .bio(request.getBio())
                .phoneNumber(request.getPhoneNumber())
                .rating(0.0)
                .age(request.getAge())
                .gender(request.getGender())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .build();
    }

    private void updateInstructorInfo(Instructor instructor, UpdateInstructorRequest request) {
        instructor.setName(request.getName());
        instructor.setEmail(request.getEmail());
        instructor.setBio(request.getBio());
        instructor.setPhoneNumber(request.getPhoneNumber());
        instructor.setAge(request.getAge());
        instructor.setPassword(passwordEncoder.encode(request.getPassword()));

    }

    private InstructorDto convertToDto(Instructor instructor) {
        return modelMapper.map(instructor, InstructorDto.class);
    }
}
