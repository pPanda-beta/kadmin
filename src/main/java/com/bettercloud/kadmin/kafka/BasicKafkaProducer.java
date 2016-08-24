package com.bettercloud.kadmin.kafka;

import com.bettercloud.kadmin.api.kafka.KadminProducer;
import com.bettercloud.kadmin.api.kafka.KadminProducerConfig;
import lombok.Getter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by davidesposito on 7/20/16.
 */
public class BasicKafkaProducer<ValueT> implements KadminProducer<String, ValueT> {

    @Getter private final KadminProducerConfig config;
    @Getter private final String id;
    @Getter private long lastUsedTime = -1;
    private KafkaProducer<String, Object> producer;
    private final AtomicLong sentCount;
    private final AtomicLong errorCount;

    public BasicKafkaProducer(KadminProducerConfig config) {
        this.config = config;
        this.id = UUID.randomUUID().toString();
        this.sentCount = new AtomicLong(0);
        this.errorCount = new AtomicLong(0);

        init();
    }

    public long getSentCount() {
        return sentCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    /**
     * Gets called before init(). Set any default configs here
     * @param config
     */
    protected void initConfig(KadminProducerConfig config) { }

    protected void init() {
        initConfig(config);

        final Properties properties = new Properties();
        properties.put("acks", "all");
        properties.put("bootstrap.servers", config.getKafkaHost());
        properties.put("schema.registry.url", config.getSchemaRegistryUrl());
        properties.put("key.serializer", config.getKeySerializer());
        properties.put("value.serializer", config.getValueSerializer().getClassName());

        this.producer = new KafkaProducer<>(properties);
    }

    @Override
    public void send(String key, ValueT val) {
        if (this.producer != null) {
            ProducerRecord<String, Object> pr = new ProducerRecord<>(config.getTopic(), key, val);
            try {
                lastUsedTime = System.currentTimeMillis();
                producer.send(pr);
                sentCount.getAndIncrement();
            } catch (Exception e) {
                errorCount.getAndIncrement();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void shutdown() {
        if (producer != null) {
            producer.close();
            producer = null;
        }
    }
}
