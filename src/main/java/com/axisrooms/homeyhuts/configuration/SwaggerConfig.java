package com.axisrooms.homeyhuts.configuration;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    //Swagger UI is available at http://localhost:61151/swagger-ui.html
    @Bean
    public Docket swaggerSettings() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(Predicates.not(PathSelectors.regex("/error.*")))
                .build()
                .apiInfo(apiInfo())
                .pathMapping("/");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("StaymakerMicroservice")
                .version("0.1.0")
                .description("StaymakerMicroservice is an application that communicates with staymaker. It is a layer between the CM and the OTA.")
                .build();
    }

}
