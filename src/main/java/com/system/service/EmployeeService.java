package com.system.service;

import com.system.entity.Employee;
import com.system.entity.Role;
import com.system.entity.User;
import com.system.repository.EmployeeRepository;
import com.system.repository.RoleRepository;
import com.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<Employee> getAllEmployees(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return employeeRepository.searchEmployees(search, pageable);
        }
        return employeeRepository.findAll(pageable);
    }

    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> getEmployeeByWhatsapp(String whatsappNumber) {
        return employeeRepository.findByWhatsappNumber(whatsappNumber);
    }

    @Transactional
    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmployeeCode(employee.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee code already exists.");
        }
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (employeeRepository.existsByWhatsappNumber(employee.getWhatsappNumber())) {
            throw new IllegalArgumentException("WhatsApp number already registered.");
        }

        // Auto-create User credentials for the Employee
        Role empRole = roleRepository.findByName("ROLE_EMPLOYEE")
                .orElseThrow(() -> new IllegalStateException("Default ROLE_EMPLOYEE role not configured."));

        User linkedUser = User.builder()
                .username(employee.getEmployeeCode())
                .email(employee.getEmail())
                .passwordHash(passwordEncoder.encode(employee.getEmployeeCode() + "123")) // Default: code123
                .roles(new HashSet<>(Collections.singletonList(empRole)))
                .build();

        linkedUser = userRepository.save(linkedUser);
        employee.setUser(linkedUser);

        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existing = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));

        if (!existing.getEmployeeCode().equals(updatedEmployee.getEmployeeCode()) &&
                employeeRepository.existsByEmployeeCode(updatedEmployee.getEmployeeCode())) {
            throw new IllegalArgumentException("Employee code already exists.");
        }
        if (!existing.getEmail().equals(updatedEmployee.getEmail()) &&
                employeeRepository.existsByEmail(updatedEmployee.getEmail())) {
            throw new IllegalArgumentException("Email already exists.");
        }
        if (!existing.getWhatsappNumber().equals(updatedEmployee.getWhatsappNumber()) &&
                employeeRepository.existsByWhatsappNumber(updatedEmployee.getWhatsappNumber())) {
            throw new IllegalArgumentException("WhatsApp number already registered.");
        }

        existing.setFirstName(updatedEmployee.getFirstName());
        existing.setLastName(updatedEmployee.getLastName());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setMobileNumber(updatedEmployee.getMobileNumber());
        existing.setWhatsappNumber(updatedEmployee.getWhatsappNumber());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setDesignation(updatedEmployee.getDesignation());
        existing.setMonthlySalary(updatedEmployee.getMonthlySalary());
        existing.setOfficeLatitude(updatedEmployee.getOfficeLatitude());
        existing.setOfficeLongitude(updatedEmployee.getOfficeLongitude());
        existing.setAllowedRadiusMeters(updatedEmployee.getAllowedRadiusMeters());
        existing.setStatus(updatedEmployee.getStatus());

        // Update corresponding User email if changed
        if (existing.getUser() != null) {
            User user = existing.getUser();
            user.setEmail(updatedEmployee.getEmail());
            user.setActive(updatedEmployee.getStatus().equalsIgnoreCase("ACTIVE"));
            userRepository.save(user);
        }

        return employeeRepository.save(existing);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with id: " + id));
        
        // Soft delete: set status to INACTIVE and disable user login
        employee.setStatus("INACTIVE");
        if (employee.getUser() != null) {
            User user = employee.getUser();
            user.setActive(false);
            userRepository.save(user);
        }
        employeeRepository.save(employee);
    }
}
