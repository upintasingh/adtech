package com.adtech.insight.repository;

import com.adtech.insight.dto.MetricType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class DynamoMetricsRepository {
    @Value("${dynamodb.table.daily}")
    private String dynamoTableDaily;

    private final DynamoDbClient dynamoDb;


    public long query(
            String tenant, String campaignId,
            MetricType type, Instant from, Instant to) {
        String pk = "TENANT#" + tenant + "#CAMPAIGN#" + campaignId;

        QueryRequest request = QueryRequest.builder()
                .tableName(dynamoTableDaily)
                .keyConditionExpression(
                        "pk = :pk AND sk BETWEEN :from AND :to")
                .expressionAttributeValues(Map.of(
                        ":pk", AttributeValue.fromS(pk),
                        ":from", AttributeValue.fromS(
                                "DATE#" + from.toString()),
                        ":to", AttributeValue.fromS(
                                "DATE#" + to.toString())
                ))
                .build();


        return dynamoDb.query(request)
                .items()
                .stream()
                .mapToLong(item ->
                        Long.parseLong(
                                item.get(type.attr()).n()))
                .sum();
    }

}
