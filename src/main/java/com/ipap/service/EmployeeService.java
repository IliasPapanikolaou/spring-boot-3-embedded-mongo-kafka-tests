package com.ipap.service;

import com.ipap.dto.EmployeeDto;
import com.ipap.entity.Employee;
import com.ipap.producer.KafkaMessageProducer;
import com.ipap.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class EmployeeService {

    private final KafkaMessageProducer kafkaMessageProducer;
    private final EmployeeRepository employeeRepository;

    public EmployeeService(KafkaMessageProducer kafkaMessageProducer, EmployeeRepository employeeRepository) {
        this.kafkaMessageProducer = kafkaMessageProducer;
        this.employeeRepository = employeeRepository;
    }

    public EmployeeDto sendMessageToKafka(EmployeeDto employeeDto) {
        kafkaMessageProducer.sendMessageToTopic("employee-topic", toEntity(employeeDto));
        return employeeDto;
    }

    public void saveEmployee(Employee employee) {
        employeeRepository.save(employee);
    }

    private static Employee toEntity(EmployeeDto employeeDto) {
        return Employee.builder()
                .id(UUID.randomUUID().toString())
                .email(employeeDto.email())
                .firstname(employeeDto.firstname())
                .lastname(employeeDto.lastname())
                .createdAt(new Date())
                .build();
    }
}
