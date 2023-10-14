package com.ipap.consumer;

import com.ipap.entity.Employee;
import com.ipap.service.EmployeeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Data
@Component
public class KafkaMessageConsumer {

    private final EmployeeService employeeService;

    private CountDownLatch latch = new CountDownLatch(1);
    private String payload;

    public KafkaMessageConsumer(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @KafkaListener(topics = "employee-topic", groupId = "employee-group")
    public void onReceiveKafkaMessage(Employee employee) {
        log.info("Received message from kafka topic: {}", employee);
        employeeService.saveEmployee(employee);
        payload = employee.toString();
        latch.countDown();
    }

    public void resetLatch() {
        latch = new CountDownLatch(1);
    }
}
