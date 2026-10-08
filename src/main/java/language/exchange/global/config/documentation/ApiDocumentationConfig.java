package language.exchange.global.config.documentation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class ApiDocumentationConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("언어교환 API")
                        .description("한국어/일본어 언어교환 주제·질문 관리")
                        .version("v1.0.0"));
    }
}