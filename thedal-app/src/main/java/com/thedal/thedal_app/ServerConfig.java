package com.thedal.thedal_app;

// @Configuration
// public class ServerConfig {

//     @Bean
//     public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
//         return factory -> {
//             try {
//                 factory.setAddress(InetAddress.getByName("0.0.0.0")); // Bind to all interfaces
//             } catch (UnknownHostException e) {
//                 throw new RuntimeException("Failed to set address", e);
//             }
//             //factory.setPort(8080); // Set your desired port
//         };
//  