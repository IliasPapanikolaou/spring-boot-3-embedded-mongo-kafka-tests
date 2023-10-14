package com.ipap.producer;

import com.ipap.entity.Employee;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMessageProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessageToTopic(String topicName, Employee employee) {
        try {
            kafkaTemplate.send(topicName, employee)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            RecordMetadata recordMetadata = result.getRecordMetadata();
                            log.info("Sent message to Kafka with correlationId: {} to topic: {} with offset: {}",
                                    employee.getId(), recordMetadata.topic(), recordMetadata.offset());
                        } else {
                            log.error("Unable to send message to Kafka with correlationId: {} due to: {}",
                                    employee.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.error("Exception occurred during process: {}", ex.getMessage());
        }
    }
}
