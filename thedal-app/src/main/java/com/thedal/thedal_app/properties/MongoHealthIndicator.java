// package com.thedal.thedal_app.properties;

// import org.springframework.boot.actuate.health.Health;
// import org.springframework.boot.actuate.health.HealthIndicator;
// import org.springframework.data.mongodb.core.MongoTemplate;
// import org.springframework.stereotype.Component;


// @Component
// public class MongoHealthIndicator implements HealthIndicator {
//     private final MongoTemplate mongoTemplate;

//     public MongoHealthIndicator(MongoTemplate mongoTemplate) {
//         this.mongoTemplate = mongoTemplate;
//     }

//     @Override
//     public Health health() {
//         try {
//             mongoTemplate.getDb().runCommand("{ ping: 1 }");
//             return Health.up().build();
//         } catch (Exception e) {
//             return Health.down()
//                 .withException(e)
//                 .build();
//         }
//     }

// }
