package com.ipap;

import com.ipap.consumer.KafkaMessageConsumer;
import com.ipap.dto.EmployeeDto;
import com.ipap.entity.Employee;
import com.ipap.producer.KafkaMessageProducer;
import com.ipap.repository.EmployeeRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EmbeddedKafka(partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    @LocalServerPort
    private int port;
    private String baseUrl = "http://localhost";
    private static RestTemplate restTemplate;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Value("${test.topic}")
    private String topic;
    @Autowired
    public KafkaTemplate<String, Employee> template;
    @Autowired
    KafkaMessageProducer kafkaMessageProducer;
    @Autowired
    KafkaMessageConsumer kafkaMessageConsumer;

    @BeforeAll
    public static void init() {
        restTemplate = new RestTemplate();
    }

    @BeforeEach
    public void setUp() {
        baseUrl = baseUrl.concat(":").concat(String.valueOf(port)).concat("/employee");
        kafkaMessageConsumer.resetLatch();
    }

    @Test
    @Order(1)
    public void testRestTemplateRequest() {
        EmployeeDto employeeDto = new EmployeeDto("John", "Rambo", "john.rambo@mail.com");
        EmployeeDto response = restTemplate.postForObject(baseUrl.concat("/save"), employeeDto, EmployeeDto.class);

        assert response != null;
        assertEquals("John", response.firstname());
        assertEquals("Rambo", response.lastname());
        assertEquals("john.rambo@mail.com", response.email());
    }

    @Test
    @Order(2)
    public void testEmbeddedMongoDB() {
        Employee employee = Employee.builder()
                .id(UUID.randomUUID().toString())
                .firstname("Rocky")
                .lastname("Balboa")
                .email("rocky.balboa@mail.com")
                .createdAt(new Date())
                .build();

        employeeRepository.save(employee);

        Employee emp = employeeRepository.findAll().stream().findFirst().orElseThrow();

        assertEquals(emp.getId(), employee.getId());
        assertThat(employeeRepository.findAll()).extracting(Employee::getId).containsOnly(employee.getId());
    }

    @Test
    @Order(3)
    public void testEmbeddedKafkaBroker() throws Exception {
        Employee employee = Employee.builder()
                .id(UUID.randomUUID().toString())
                .firstname("Maria")
                .lastname("Spendings")
                .email("maria.spendings@mail.com")
                .createdAt(new Date())
                .build();

        template.send(topic, employee);

        boolean messageConsumed = kafkaMessageConsumer.getLatch()
                .await(10, TimeUnit.SECONDS);
        assertTrue(messageConsumed);
        assertThat(kafkaMessageConsumer.getPayload(), containsString(employee.toString()));
    }

    @Test
    @Order(4)
    public void testEmbeddedKafkaBroker_embeddedMongoDB() throws InterruptedException {
        Employee employee = Employee.builder()
                .id(UUID.randomUUID().toString())
                .firstname("Maria")
                .lastname("Spendings")
                .email("maria.spendings@mail.com")
                .createdAt(new Date())
                .build();

        kafkaMessageProducer.sendMessageToTopic(topic, employee);

        boolean messageConsumed = kafkaMessageConsumer.getLatch().await(10, TimeUnit.SECONDS);
        assertTrue(messageConsumed);

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Employee> optionalEmployee = employeeRepository.findAll().stream().findFirst();
            assertThat(optionalEmployee.isPresent()).isTrue();
            assertThat(optionalEmployee.get().getId()).isEqualTo(employee.getId());
            assertThat(employeeRepository.findAll()).extracting(Employee::getId).containsOnly(employee.getId());
        });
    }

    @Test
    @Order(5)
    public void EndToEndIntegrationTest() {
        EmployeeDto employeeDto = new EmployeeDto("John", "Rambo", "john.rambo@mail.com");
        EmployeeDto response = restTemplate.postForObject(baseUrl.concat("/save"), employeeDto, EmployeeDto.class);

        assertNotNull(response);

        await().pollInterval(Duration.ofSeconds(3)).atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<Employee> optionalEmployee = employeeRepository.findAll().stream().findFirst();
            assertThat(optionalEmployee.isPresent()).isTrue();
            assertThat(optionalEmployee.get().getEmail()).isEqualTo(employeeDto.email());
            assertThat(employeeRepository.findAll()).extracting(Employee::getEmail).containsOnly(employeeDto.email());
            assertThat(employeeRepository.findAll())
                    .extracting(Employee::getId).containsOnly(optionalEmployee.get().getId());
        });
    }


}
